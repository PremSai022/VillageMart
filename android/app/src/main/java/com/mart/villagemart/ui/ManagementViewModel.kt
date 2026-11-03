package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.CategoryRequest
import com.mart.villagemart.data.CreditCollectionRequest
import com.mart.villagemart.data.CustomerRequest
import com.mart.villagemart.data.DecimalValue
import com.mart.villagemart.data.ExpenseRequest
import com.mart.villagemart.data.InventorySummaryDto
import com.mart.villagemart.data.PurchaseItemRequest
import com.mart.villagemart.data.PurchaseRequest
import com.mart.villagemart.data.SaleDto
import com.mart.villagemart.data.SalesReturnRequest
import com.mart.villagemart.data.StockAdjustmentRequest
import com.mart.villagemart.data.VillageMartApi
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.VillageRequest
import com.mart.villagemart.data.SupplierRequest
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class FeatureRecord(
    val id: Long,
    val title: String,
    val subtitle: String = "",
    val trailing: String = "",
    val status: String = ""
)

data class ManagementState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val records: List<FeatureRecord> = emptyList(),
    val options: List<Pair<Long, String>> = emptyList(),
    val secondaryOptions: List<Pair<Long, String>> = emptyList(),
    val unreadCount: Long = 0,
    val error: String? = null,
    val message: String? = null,
    val loaded: Boolean = false,
    val savedCount: Int = 0,
    val inventorySummary: InventorySummaryDto? = null,
    val saleDetail: SaleDto? = null,
    val saleDetailLoading: Boolean = false
)

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val repository: VillageMartRepository,
    private val api: VillageMartApi
) : ViewModel() {
    private val mutableState = MutableStateFlow(ManagementState())
    val state: StateFlow<ManagementState> = mutableState.asStateFlow()

    fun load(feature: String, query: String? = null) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val records = when (feature) {
                    "suppliers", "supplier-details" -> repository.suppliers(query).map {
                        FeatureRecord(it.id, it.name, it.mobile.orEmpty(), it.gstNumber.orEmpty(), if (it.active) "ACTIVE" else "INACTIVE")
                    }
                    "villages", "village-details" -> repository.villages(query).map {
                        FeatureRecord(it.id, it.name, listOfNotNull(it.district, it.state).joinToString(", "), status = if (it.active) "ACTIVE" else "INACTIVE")
                    }
                    "customers", "customer-details" -> repository.customers(query).map {
                        FeatureRecord(it.id, it.name, it.mobile.orEmpty(), it.outstandingBalance.toString(), if (it.active) "ACTIVE" else "INACTIVE")
                    }
                    "expenses" -> repository.expenses().map {
                        FeatureRecord(it.id, it.categoryName, it.description.orEmpty(), "₹${it.amount}", it.expenseDate)
                    }
                    "purchases", "purchase-details" -> repository.purchases().map {
                        FeatureRecord(it.id, it.purchaseNumber, it.supplierName, "₹${it.totalAmount}", it.status)
                    }
                    "sales", "invoice-details" -> repository.sales().map {
                        FeatureRecord(it.id, it.invoiceNumber, it.customerName ?: "Direct sale", "₹${it.totalAmount}", it.status)
                    }
                    "notifications" -> repository.notifications().map {
                        FeatureRecord(it.id, it.title, it.message, status = if (it.read) "READ" else "UNREAD")
                    }
                    "categories" -> api.categories(activeOnly = false).map { FeatureRecord(it.id, it.name, it.description.orEmpty()) }
                    "stock-movements" -> repository.stockMovements().map {
                        FeatureRecord(
                            it.id,
                            it.productName,
                            listOfNotNull(
                                it.movementType.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase) +
                                    " · ${it.previousQuantity} → ${it.newQuantity}",
                                it.reason?.takeIf(String::isNotBlank) ?: it.referenceNumber
                            ).joinToString("\n"),
                            (if (it.quantityChange.value.signum() > 0) "+" else "") + it.quantityChange.toString(),
                            it.createdAt.take(10)
                        )
                    }
                    "inventory", "low-stock", "out-of-stock", "stock-adjustment" ->
                        repository.inventory(
                            status = when (feature) {
                                "low-stock" -> "LOW_STOCK"
                                "out-of-stock" -> "OUT_OF_STOCK"
                                else -> null
                            },
                            query = query
                        ).map {
                            FeatureRecord(
                                it.productId,
                                it.productName,
                                "${it.availableQuantity} ${it.unitCode} · minimum ${it.minStockQuantity}",
                                "₹${it.stockValue}",
                                it.stockStatus
                            )
                        }
                    else -> emptyList()
                }
                val options = when (feature) {
                    "expenses" -> repository.expenseCategories().map { it.id to it.name }
                    "purchases" -> repository.suppliers().map { it.id to it.name }
                    "stock-adjustment" -> api.products(active = true, size = 100).content.map { it.id to it.name }
                    else -> emptyList()
                }
                val secondaryOptions = if (feature == "purchases") {
                    api.products(active = true, size = 100).content.map { it.id to it.name }
                } else {
                    emptyList()
                }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    records = records,
                    options = options,
                    secondaryOptions = secondaryOptions,
                    unreadCount = if (feature == "notifications") repository.unreadCount() else mutableState.value.unreadCount,
                    inventorySummary = if (feature == "inventory") repository.inventorySummary() else mutableState.value.inventorySummary,
                    loaded = true
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, loaded = true, error = error.userMessage(JSON))
            }
        }
    }

    fun create(feature: String, values: Map<String, String>) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, message = null)
            try {
                when (feature) {
                    "suppliers" -> repository.createSupplier(
                        SupplierRequest(
                            name = values.required("name"),
                            mobile = values["mobile"].clean(),
                            address = values["address"].clean(),
                            gstNumber = values["gstNumber"].clean(),
                            notes = values["notes"].clean()
                        )
                    )
                    "villages" -> repository.createVillage(
                        VillageRequest(
                            name = values.required("name"),
                            district = values["district"].clean(),
                            state = values.required("state"),
                            notes = values["notes"].clean()
                        )
                    )
                    "customers" -> repository.createCustomer(
                        CustomerRequest(
                            name = values.required("name"),
                            mobile = values["mobile"].clean(),
                            address = values["address"].clean(),
                            notes = values["notes"].clean()
                        )
                    )
                    "categories" -> api.createCategory(
                        CategoryRequest(values.required("name"), values["description"].clean())
                    )
                    "expenses" -> repository.createExpense(
                        ExpenseRequest(
                            categoryId = values.required("categoryId").toLong(),
                            amount = DecimalValue(values.required("amount")),
                            expenseDate = values["date"]?.takeIf(String::isNotBlank) ?: java.time.LocalDate.now().toString(),
                            description = values["description"].clean()
                        )
                    )
                    "purchases" -> repository.createPurchase(
                        PurchaseRequest(
                            supplierId = values.required("supplierId").toLong(),
                            items = listOf(
                                PurchaseItemRequest(
                                    productId = values.required("productId").toLong(),
                                    quantity = DecimalValue(values.required("quantity")),
                                    unitPrice = DecimalValue(values.required("unitPrice")),
                                    discountAmount = values["discount"].clean()?.let(::DecimalValue)
                                )
                            ),
                            transportCharges = values["transport"].clean()?.let(::DecimalValue),
                            notes = values["notes"].clean(),
                            complete = true
                        )
                    )
                    "stock-adjustment" -> api.adjustStock(
                        StockAdjustmentRequest(
                            productId = values.required("productId").toLong(),
                            direction = values.required("direction"),
                            quantity = DecimalValue(values.required("quantity")),
                            unitCost = values["unitCost"].clean()?.let(::DecimalValue),
                            reason = values.required("reason")
                        )
                    )
                }
                mutableState.value = mutableState.value.copy(
                    saving = false,
                    message = "Saved",
                    savedCount = mutableState.value.savedCount + 1
                )
                load(feature)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    fun clearFeedback() {
        mutableState.value = mutableState.value.copy(error = null, message = null)
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            try {
                repository.markNotificationRead(id)
                load("notifications")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.userMessage(JSON))
            }
        }
    }

    fun cancelSale(id: Long) {
        viewModelScope.launch {
            try {
                api.cancelSale(id, mapOf("reason" to "Cancelled by owner from Android app"))
                load("sales")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.userMessage(JSON))
            }
        }
    }

    fun loadSaleDetails(id: Long) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saleDetailLoading = true, error = null)
            try {
                mutableState.value = mutableState.value.copy(
                    saleDetail = repository.sale(id),
                    saleDetailLoading = false
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    saleDetailLoading = false,
                    error = error.userMessage(JSON)
                )
            }
        }
    }

    fun createSalesReturn(id: Long, idempotencyKey: String, request: SalesReturnRequest) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, message = null)
            try {
                repository.createSalesReturn(id, idempotencyKey, request)
                mutableState.value = mutableState.value.copy(saving = false, message = "Return recorded.")
                load("sales")
                loadSaleDetails(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    fun collectCredit(customerId: Long, amount: String, method: String, idempotencyKey: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, message = null)
            try {
                val result = repository.collectCredit(
                    customerId,
                    idempotencyKey,
                    CreditCollectionRequest(method = method, amount = DecimalValue(amount))
                )
                mutableState.value = mutableState.value.copy(
                    saving = false,
                    message = "Payment recorded. Remaining balance: ₹${result.outstandingAfter}"
                )
                load("customers")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    private fun Map<String, String>.required(key: String): String =
        get(key)?.trim()?.takeIf(String::isNotEmpty) ?: error("${key.replaceFirstChar(Char::uppercase)} is required")

    private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
