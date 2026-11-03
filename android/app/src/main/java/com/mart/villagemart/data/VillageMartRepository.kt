package com.mart.villagemart.data

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mart.villagemart.worker.PendingSaleWorker
import com.mart.villagemart.notification.DeviceTokenStore
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SaleSubmission {
    data class Confirmed(val sale: SaleDto) : SaleSubmission
    data class Queued(val idempotencyKey: String) : SaleSubmission
}

@Singleton
class VillageMartRepository @Inject constructor(
    private val api: VillageMartApi,
    private val tokenStore: TokenStore,
    private val pendingSaleDao: PendingSaleDao,
    private val workManager: WorkManager,
    private val json: Json,
    private val deviceTokenStore: DeviceTokenStore
) {
    val pendingSales: Flow<List<PendingSale>> = pendingSaleDao.observeAll()

    suspend fun login(login: String, password: String): AuthResponse {
        val response = api.login(LoginRequest(login.trim(), password))
        tokenStore.save(response.accessToken, response.refreshToken)
        val pushToken = if (com.mart.villagemart.BuildConfig.FIREBASE_CONFIGURED) deviceTokenStore.get() else null
        if (pushToken != null) {
            try {
                api.registerDevice(DeviceRegistrationRequest(pushToken))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Device token registration failed; dashboard notifications remain available", error)
            }
        }
        return response
    }

    suspend fun logout() {
        val refresh = tokenStore.refreshToken()
        try {
            if (refresh != null) api.logout(LogoutRequest(refresh))
        } finally {
            tokenStore.clear()
        }
    }

    suspend fun dashboard(period: String? = null): DashboardSummaryDto = api.dashboard(period)

    suspend fun businessSettings(): BusinessSettingsDto = api.businessSettings()

    suspend fun updateBusinessSettings(request: BusinessSettingsRequest): BusinessSettingsDto =
        api.updateBusinessSettings(request)

    suspend fun report(name: String, period: String?): kotlinx.serialization.json.JsonObject =
        api.report(name, period)

    suspend fun inventorySummary(): InventorySummaryDto = api.inventorySummary()

    suspend fun stockMovements(): List<StockMovementDto> = api.stockMovements().content

    suspend fun product(id: Long): ProductDto = api.product(id)

    suspend fun products(query: String? = null, page: Int = 0): PageResponse<ProductDto> =
        api.products(query = query?.takeIf(String::isNotBlank), page = page)

    suspend fun productOptions(): Pair<List<CategoryDto>, List<UnitDto>> =
        api.categories() to api.units()

    suspend fun createProduct(request: ProductRequest): ProductDto = api.createProduct(request)

    suspend fun updateProduct(id: Long, request: ProductRequest): ProductDto = api.updateProduct(id, request)

    suspend fun setProductStatus(id: Long, active: Boolean): ProductDto =
        api.setProductStatus(id, ProductStatusRequest(active))

    suspend fun inventory(status: String? = null, query: String? = null): List<InventoryItemDto> =
        if (status == "LOW_STOCK") api.lowStock()
        else if (status == "OUT_OF_STOCK") api.outOfStock()
        else api.inventory(query = query?.takeIf(String::isNotBlank)).content

    suspend fun suppliers(query: String? = null): List<SupplierDto> = api.suppliers(query).content

    suspend fun createSupplier(request: SupplierRequest): SupplierDto = api.createSupplier(request)

    suspend fun villages(query: String? = null): List<VillageDto> = api.villages(query).content

    suspend fun createVillage(request: VillageRequest): VillageDto = api.createVillage(request)

    suspend fun customers(query: String? = null): List<CustomerDto> = api.customers(query).content

    suspend fun createCustomer(request: CustomerRequest): CustomerDto = api.createCustomer(request)

    suspend fun collectCredit(
        id: Long,
        idempotencyKey: String,
        request: CreditCollectionRequest
    ): CreditCollectionResultDto = api.collectCredit(id, idempotencyKey, request)

    suspend fun expenses(): List<ExpenseDto> = api.expenses().content

    suspend fun expenseCategories(): List<ExpenseCategoryDto> = api.expenseCategories()

    suspend fun createExpense(request: ExpenseRequest): ExpenseDto = api.createExpense(request)

    suspend fun createPurchase(request: PurchaseRequest): kotlinx.serialization.json.JsonObject =
        api.createPurchase(UUID.randomUUID().toString(), request)

    suspend fun sales(): List<SaleSummaryDto> = api.sales().content

    suspend fun sale(id: Long): SaleDto = api.sale(id)

    suspend fun createSalesReturn(
        id: Long,
        idempotencyKey: String,
        request: SalesReturnRequest
    ): SalesReturnDto = api.createSalesReturn(id, idempotencyKey, request)

    suspend fun purchases(): List<PurchaseSummaryDto> = api.purchases().content

    suspend fun notifications(): List<NotificationDto> = api.notifications().content

    suspend fun me(): UserDto = api.me()

    suspend fun markNotificationRead(id: Long): NotificationDto = api.markNotificationRead(id)

    suspend fun unreadCount(): Long = api.unreadCount().count

    suspend fun submitSale(request: SaleRequest): SaleSubmission {
        val key = UUID.randomUUID().toString()
        val draft = PendingSale(
            idempotencyKey = key,
            requestJson = json.encodeToString(SaleRequest.serializer(), request),
            createdAtEpochMillis = System.currentTimeMillis()
        )
        pendingSaleDao.put(draft)
        return try {
            val sale = api.createSale(key, request)
            pendingSaleDao.markConfirmed(key, sale.id, sale.invoiceNumber)
            SaleSubmission.Confirmed(sale)
        } catch (error: IOException) {
            pendingSaleDao.updateError(key, error.message ?: "Network unavailable")
            schedulePendingSales()
            SaleSubmission.Queued(key)
        } catch (error: HttpException) {
            if (error.code() >= 500) {
                pendingSaleDao.updateError(key, error.message())
                schedulePendingSales()
                SaleSubmission.Queued(key)
            } else {
                pendingSaleDao.markFailed(key, error.userMessage(json))
                throw error
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            pendingSaleDao.markFailed(key, error.userMessage(json))
            throw error
        }
    }

    suspend fun retryPendingSale(draft: PendingSale): SaleDto {
        val request = json.decodeFromString(SaleRequest.serializer(), draft.requestJson)
        val sale = api.createSale(draft.idempotencyKey, request)
        pendingSaleDao.markConfirmed(draft.idempotencyKey, sale.id, sale.invoiceNumber)
        return sale
    }

    private fun schedulePendingSales() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<PendingSaleWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(PENDING_SALE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    private companion object {
        const val PENDING_SALE_WORK = "pending-sales-sync"
        const val TAG = "VillageMartRepository"
    }
}
