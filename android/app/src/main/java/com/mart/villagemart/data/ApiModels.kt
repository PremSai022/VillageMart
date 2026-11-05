package com.mart.villagemart.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val mobileNumber: String,
    val email: String? = null,
    val password: String,
    val businessName: String,
    val businessAddress: String? = null,
    val acceptTerms: Boolean
)

@Serializable
data class RegistrationStatusDto(val registrationOpen: Boolean)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val mobile: String? = null,
    val email: String? = null,
    val fullName: String,
    val role: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserDto
)

@Serializable
data class ApiError(
    val status: Int = 0,
    val code: String = "NETWORK_ERROR",
    val message: String = "The request could not be completed",
    val fieldErrors: List<FieldViolation> = emptyList()
)

@Serializable
data class FieldViolation(val field: String, val message: String)

@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val last: Boolean = true
)

@Serializable
data class CategoryDto(val id: Long, val name: String, val description: String? = null, val active: Boolean = true)

@Serializable
data class UnitDto(val id: Long, val code: String, val name: String, val allowsDecimal: Boolean)

@Serializable
data class ProductDto(
    val id: Long,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    val categoryId: Long,
    val categoryName: String,
    val unitId: Long,
    val unitCode: String,
    val unitName: String,
    val unitAllowsDecimal: Boolean = true,
    val description: String? = null,
    val imageUrl: String? = null,
    val purchasePrice: DecimalValue,
    val sellingPrice: DecimalValue,
    val minSellingPrice: DecimalValue? = null,
    val minStockQuantity: DecimalValue,
    val taxRate: DecimalValue = DecimalValue.ZERO,
    val hsnCode: String? = null,
    val active: Boolean,
    val availableQuantity: DecimalValue = DecimalValue.ZERO,
    val averageCost: DecimalValue = DecimalValue.ZERO,
    val stockStatus: String = "NORMAL"
)

@Serializable
data class BusinessSettingsDto(
    val businessName: String = "",
    val address: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val gstEnabled: Boolean = false,
    val gstin: String? = null,
    val stateName: String? = null,
    val currencyCode: String = "INR",
    val timezone: String = "Asia/Kolkata",
    val invoiceFooter: String? = null,
    val lowStockPushEnabled: Boolean = false
)

@Serializable
data class BusinessSettingsRequest(
    val businessName: String,
    val address: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val gstEnabled: Boolean = false,
    val gstin: String? = null,
    val stateName: String? = null,
    val invoiceFooter: String? = null,
    val lowStockPushEnabled: Boolean = false
)

@Serializable
data class StockMovementDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val movementType: String,
    val quantityChange: DecimalValue,
    val previousQuantity: DecimalValue,
    val newQuantity: DecimalValue,
    val unitCost: DecimalValue? = null,
    val averageCostAfter: DecimalValue? = null,
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val referenceNumber: String? = null,
    val reason: String? = null,
    val createdBy: Long? = null,
    val createdAt: String
)

@Serializable
data class ProductRequest(
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    val categoryId: Long,
    val unitId: Long,
    val description: String? = null,
    val imageUrl: String? = null,
    val purchasePrice: DecimalValue,
    val sellingPrice: DecimalValue,
    val minSellingPrice: DecimalValue? = null,
    val minStockQuantity: DecimalValue,
    val taxRate: DecimalValue? = null,
    val hsnCode: String? = null,
    val active: Boolean = true
)

@Serializable
data class ProductStatusRequest(val active: Boolean)

@Serializable
data class InventoryItemDto(
    val productId: Long,
    val productName: String,
    val sku: String? = null,
    val categoryName: String,
    val unitCode: String,
    val availableQuantity: DecimalValue,
    val minStockQuantity: DecimalValue,
    val averageCost: DecimalValue,
    val stockValue: DecimalValue,
    val sellingPrice: DecimalValue,
    val stockStatus: String,
    val suggestedReorderQuantity: DecimalValue
)

@Serializable
data class InventorySummaryDto(
    val totalProducts: Long,
    val totalQuantity: DecimalValue,
    val totalValue: DecimalValue,
    val lowStockCount: Long,
    val outOfStockCount: Long
)

@Serializable
data class DashboardSummaryDto(
    val from: String,
    val to: String,
    val salesAmount: DecimalValue,
    val grossProfit: DecimalValue,
    val expenses: DecimalValue,
    val netProfit: DecimalValue,
    val netCollected: DecimalValue,
    val creditGiven: DecimalValue,
    val totalOutstanding: DecimalValue,
    val totalProducts: Long,
    val totalInventoryQuantity: DecimalValue,
    val totalInventoryValue: DecimalValue,
    val lowStockCount: Long,
    val outOfStockCount: Long,
    val lowStockItems: List<InventoryItemDto> = emptyList(),
    val outOfStockItems: List<InventoryItemDto> = emptyList(),
    val recentSales: List<SaleSummaryDto> = emptyList(),
    val recentPurchases: List<PurchaseSummaryDto> = emptyList(),
    val bestSellers: List<ProductSalesRow> = emptyList()
)

@Serializable
data class SupplierDto(
    val id: Long,
    val name: String,
    val mobile: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class SupplierRequest(
    val name: String,
    val mobile: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class VillageDto(
    val id: Long,
    val name: String,
    val district: String? = null,
    val state: String,
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class VillageRequest(
    val name: String,
    val district: String? = null,
    val state: String,
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class CustomerDto(
    val id: Long,
    val name: String,
    val mobile: String? = null,
    val villageId: Long? = null,
    val villageName: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val outstandingBalance: DecimalValue = DecimalValue.ZERO
)

@Serializable
data class CustomerDetailDto(
    val customer: CustomerDto,
    val totalInvoices: Long,
    val totalPurchases: DecimalValue,
    val outstandingBalance: DecimalValue,
    val lastPurchaseDate: String? = null
)

@Serializable
data class CustomerRequest(
    val name: String,
    val mobile: String? = null,
    val villageId: Long? = null,
    val address: String? = null,
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class CreditCollectionRequest(
    val method: String,
    val amount: DecimalValue,
    val paymentDate: String? = null,
    val reference: String? = null,
    val notes: String? = null
)

@Serializable
data class CreditCollectionResultDto(
    val customerId: Long,
    val amountCollected: DecimalValue,
    val outstandingAfter: DecimalValue,
    val replayed: Boolean = false
)

@Serializable
data class ExpenseCategoryDto(val id: Long, val name: String, val active: Boolean = true)

@Serializable
data class ExpenseDto(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val amount: DecimalValue,
    val expenseDate: String,
    val description: String? = null,
    val villageId: Long? = null,
    val villageName: String? = null
)

@Serializable
data class ExpenseRequest(
    val categoryId: Long,
    val amount: DecimalValue,
    val expenseDate: String,
    val description: String? = null,
    val villageId: Long? = null
)

@Serializable
data class SaleItemRequest(
    val productId: Long,
    val quantity: DecimalValue,
    val unitPrice: DecimalValue? = null,
    val discountAmount: DecimalValue? = null
)

@Serializable
data class PaymentLine(val method: String, val amount: DecimalValue, val reference: String? = null)

@Serializable
data class SaleRequest(
    val customerId: Long? = null,
    val villageId: Long? = null,
    val saleDate: String? = null,
    val items: List<SaleItemRequest>,
    val billDiscount: DecimalValue? = null,
    val paymentMode: String,
    val payments: List<PaymentLine> = emptyList(),
    val priceOverrideConfirmed: Boolean = false,
    val roundOff: Boolean = false,
    val notes: String? = null
)

@Serializable
data class SaleSummaryDto(
    val id: Long,
    val invoiceNumber: String,
    val saleDate: String,
    val customerName: String? = null,
    val villageName: String? = null,
    val status: String,
    val paymentMode: String,
    val totalAmount: DecimalValue,
    val paidAmount: DecimalValue,
    val outstandingAmount: DecimalValue
)

@Serializable
data class SaleItemDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val unitCode: String,
    val quantity: DecimalValue,
    val unitPrice: DecimalValue,
    val lineTotal: DecimalValue,
    val returnableQuantity: DecimalValue
)

@Serializable
data class SaleDto(
    val id: Long,
    val invoiceNumber: String,
    val saleDate: String,
    val saleType: String,
    val customerName: String? = null,
    val villageName: String? = null,
    val status: String,
    val paymentMode: String,
    val subtotal: DecimalValue,
    val taxableAmount: DecimalValue,
    val totalAmount: DecimalValue,
    val paidAmount: DecimalValue,
    val outstandingAmount: DecimalValue,
    val items: List<SaleItemDto> = emptyList(),
    val returns: List<SalesReturnDto> = emptyList(),
    val replayed: Boolean = false
)

@Serializable
data class SalesReturnItemRequest(
    val saleItemId: Long,
    val quantity: DecimalValue,
    val saleable: Boolean
)

@Serializable
data class SalesReturnRequest(
    val items: List<SalesReturnItemRequest>,
    val reason: String,
    val returnDate: String? = null,
    val refundMethod: String? = null
)

@Serializable
data class SalesReturnDto(
    val id: Long,
    val returnNumber: String,
    val saleId: Long,
    val invoiceNumber: String,
    val returnDate: String,
    val reason: String,
    val refundAmount: DecimalValue,
    val revenueReversed: DecimalValue,
    val costReversed: DecimalValue,
    val creditAdjusted: DecimalValue,
    val cashRefunded: DecimalValue,
    val refundMethod: String? = null,
    val items: List<SalesReturnItemDto> = emptyList()
)

@Serializable
data class SalesReturnItemDto(
    val id: Long,
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val unitCode: String,
    val quantity: DecimalValue,
    val saleable: Boolean,
    val refundAmount: DecimalValue
)

@Serializable
data class PurchaseItemRequest(
    val productId: Long,
    val quantity: DecimalValue,
    val unitPrice: DecimalValue,
    val discountAmount: DecimalValue? = null
)

@Serializable
data class PurchaseRequest(
    val supplierId: Long,
    val supplierInvoiceNumber: String? = null,
    val purchaseDate: String? = null,
    val items: List<PurchaseItemRequest>,
    val transportCharges: DecimalValue? = null,
    val otherCharges: DecimalValue? = null,
    val notes: String? = null,
    val complete: Boolean = true
)

@Serializable
data class PurchaseSummaryDto(
    val id: Long,
    val purchaseNumber: String,
    val supplierName: String,
    val purchaseDate: String,
    val status: String,
    val totalAmount: DecimalValue,
    val returnedAmount: DecimalValue
)

@Serializable
data class NotificationDto(
    val id: Long,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val productId: Long? = null,
    val productName: String? = null,
    val read: Boolean,
    val alertActive: Boolean = true
)

@Serializable
data class UnreadCountDto(val count: Long)

@Serializable
data class ProductSalesRow(
    val productId: Long,
    val productName: String,
    val quantitySold: DecimalValue,
    val netRevenue: DecimalValue,
    val grossProfit: DecimalValue
)

@Serializable
data class StockAdjustmentRequest(
    val productId: Long,
    val type: String = "MANUAL_ADJUSTMENT",
    val direction: String,
    val quantity: DecimalValue,
    val unitCost: DecimalValue? = null,
    val reason: String
)

@Serializable
data class CategoryRequest(val name: String, val description: String? = null, val sortOrder: Int = 0, val active: Boolean = true)

@Serializable
data class DeviceRegistrationRequest(val token: String, val platform: String = "ANDROID")

@Serializable
data class CollectionResultDto(val amountCollected: DecimalValue, val outstandingAfter: DecimalValue)

@Serializable
data class ReportQuery(
    val period: String? = null,
    val from: String? = null,
    val to: String? = null
)
