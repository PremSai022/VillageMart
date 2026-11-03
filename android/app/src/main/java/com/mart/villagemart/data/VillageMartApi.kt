package com.mart.villagemart.data

import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface VillageMartApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @GET("auth/me")
    suspend fun me(): UserDto

    @GET("dashboard/summary")
    suspend fun dashboard(
        @Query("period") period: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): DashboardSummaryDto

    @GET("categories")
    suspend fun categories(@Query("activeOnly") activeOnly: Boolean = true): List<CategoryDto>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryRequest): CategoryDto

    @GET("units")
    suspend fun units(): List<UnitDto>

    @GET("products")
    suspend fun products(
        @Query("q") query: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("active") active: Boolean? = true,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<ProductDto>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Long): ProductDto

    @POST("products")
    suspend fun createProduct(@Body request: ProductRequest): ProductDto

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body request: ProductRequest): ProductDto

    @PATCH("products/{id}/status")
    suspend fun setProductStatus(@Path("id") id: Long, @Body request: ProductStatusRequest): ProductDto

    @GET("inventory")
    suspend fun inventory(
        @Query("q") query: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<InventoryItemDto>

    @GET("inventory/summary")
    suspend fun inventorySummary(): InventorySummaryDto

    @GET("inventory/low-stock")
    suspend fun lowStock(): List<InventoryItemDto>

    @GET("inventory/out-of-stock")
    suspend fun outOfStock(): List<InventoryItemDto>

    @GET("inventory/movements")
    suspend fun stockMovements(
        @Query("productId") productId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<StockMovementDto>

    @POST("inventory/adjustments")
    suspend fun adjustStock(@Body request: StockAdjustmentRequest): InventoryItemDto

    @GET("suppliers")
    suspend fun suppliers(@Query("q") query: String? = null, @Query("page") page: Int = 0): PageResponse<SupplierDto>

    @POST("suppliers")
    suspend fun createSupplier(@Body request: SupplierRequest): SupplierDto

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: Long, @Body request: SupplierRequest): SupplierDto

    @GET("villages")
    suspend fun villages(@Query("q") query: String? = null, @Query("page") page: Int = 0): PageResponse<VillageDto>

    @POST("villages")
    suspend fun createVillage(@Body request: VillageRequest): VillageDto

    @PUT("villages/{id}")
    suspend fun updateVillage(@Path("id") id: Long, @Body request: VillageRequest): VillageDto

    @GET("customers")
    suspend fun customers(@Query("q") query: String? = null, @Query("page") page: Int = 0): PageResponse<CustomerDto>

    @POST("customers")
    suspend fun createCustomer(@Body request: CustomerRequest): CustomerDto

    @PUT("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body request: CustomerRequest): CustomerDto

    @GET("customers/{id}")
    suspend fun customer(@Path("id") id: Long): CustomerDetailDto

    @POST("customers/{id}/payments")
    suspend fun collectCredit(
        @Path("id") id: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreditCollectionRequest
    ): CreditCollectionResultDto

    @GET("expenses")
    suspend fun expenses(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0
    ): PageResponse<ExpenseDto>

    @GET("expenses/categories")
    suspend fun expenseCategories(): List<ExpenseCategoryDto>

    @POST("expenses")
    suspend fun createExpense(@Body request: ExpenseRequest): ExpenseDto

    @GET("sales")
    suspend fun sales(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0
    ): PageResponse<SaleSummaryDto>

    @GET("sales/{id}")
    suspend fun sale(@Path("id") id: Long): SaleDto

    @POST("sales")
    suspend fun createSale(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SaleRequest
    ): SaleDto

    @POST("sales/{id}/cancel")
    suspend fun cancelSale(@Path("id") id: Long, @Body request: Map<String, String>): SaleDto

    @POST("sales/{id}/returns")
    suspend fun createSalesReturn(
        @Path("id") id: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SalesReturnRequest
    ): SalesReturnDto

    @GET("purchases")
    suspend fun purchases(@Query("page") page: Int = 0): PageResponse<PurchaseSummaryDto>

    @POST("purchases")
    suspend fun createPurchase(
        @Header("Idempotency-Key") idempotencyKey: String?,
        @Body request: PurchaseRequest
    ): JsonObject

    @POST("purchases/{id}/complete")
    suspend fun completePurchase(@Path("id") id: Long): JsonObject

    @GET("notifications")
    suspend fun notifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("page") page: Int = 0
    ): PageResponse<NotificationDto>

    @GET("notifications/active-alerts")
    suspend fun activeAlerts(): List<NotificationDto>

    @GET("notifications/unread-count")
    suspend fun unreadCount(): UnreadCountDto

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): NotificationDto

    @POST("notifications/devices")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<Unit>

    @GET("reports/daily-sales")
    suspend fun dailySales(@Query("period") period: String = "TODAY"): JsonObject

    @GET("reports/product-sales")
    suspend fun productSales(@Query("period") period: String = "THIS_MONTH"): JsonObject

    @GET("reports/village-sales")
    suspend fun villageSales(@Query("period") period: String = "THIS_MONTH"): JsonObject

    @GET("reports/profit")
    suspend fun profit(@Query("period") period: String = "THIS_MONTH"): JsonObject

    @GET("reports/stock")
    suspend fun stockReport(): JsonObject

    @GET("reports/{name}")
    suspend fun report(@Path("name") name: String, @Query("period") period: String? = null): JsonObject

    @Streaming
    @GET("sales/{id}/invoice")
    suspend fun invoice(@Path("id") id: Long): ResponseBody

    @GET("settings/business")
    suspend fun businessSettings(): BusinessSettingsDto

    @PUT("settings/business")
    suspend fun updateBusinessSettings(@Body request: BusinessSettingsRequest): BusinessSettingsDto

    @Streaming
    @GET
    suspend fun download(@Url url: String): ResponseBody
}
