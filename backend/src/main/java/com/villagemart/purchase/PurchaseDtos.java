package com.villagemart.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDtos {

    private PurchaseDtos() {
    }

    public record PurchaseItemRequest(
            @NotNull(message = "Product is required") Long productId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
            @Digits(integer = 11, fraction = 3) BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
            @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
            @DecimalMin(value = "0.00", message = "Discount cannot be negative")
            @Digits(integer = 12, fraction = 2) BigDecimal discountAmount) {
    }

    public record PurchaseRequest(
            @NotNull(message = "Supplier is required") Long supplierId,
            @Size(max = 50) String supplierInvoiceNumber,
            LocalDate purchaseDate,
            @NotEmpty(message = "At least one item is required") @Size(max = 200) List<@Valid PurchaseItemRequest> items,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal transportCharges,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal otherCharges,
            @Size(max = 500) String notes,
            boolean complete) {
    }

    public record PurchaseItemDto(Long id, Long productId, String productName, String unitCode, BigDecimal quantity,
                                  BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal lineTotal,
                                  BigDecimal allocatedCharges, BigDecimal landedUnitCost, BigDecimal returnedQuantity,
                                  BigDecimal returnableQuantity) {
    }

    public record PurchaseDto(Long id, String purchaseNumber, Long supplierId, String supplierName,
                              String supplierInvoiceNumber, LocalDate purchaseDate, PurchaseStatus status,
                              BigDecimal subtotal, BigDecimal discountTotal, BigDecimal transportCharges,
                              BigDecimal otherCharges, BigDecimal totalAmount, BigDecimal returnedAmount, String notes,
                              Instant completedAt, Instant cancelledAt, String cancelReason, Instant createdAt,
                              List<PurchaseItemDto> items, List<PurchaseReturnDto> returns) {
    }

    public record PurchaseSummaryDto(Long id, String purchaseNumber, Long supplierId, String supplierName,
                                     String supplierInvoiceNumber, LocalDate purchaseDate, PurchaseStatus status,
                                     BigDecimal totalAmount, BigDecimal returnedAmount, Instant createdAt) {
    }

    public record CancelRequest(@NotBlank(message = "Cancellation reason is required") @Size(max = 300) String reason) {
    }

    public record PurchaseReturnItemRequest(
            @NotNull Long purchaseItemId,
            @NotNull @DecimalMin(value = "0.001", message = "Return quantity must be greater than zero")
            @Digits(integer = 11, fraction = 3) BigDecimal quantity) {
    }

    public record PurchaseReturnRequest(
            @NotEmpty(message = "Select at least one item to return") List<@Valid PurchaseReturnItemRequest> items,
            @NotBlank(message = "Return reason is required") @Size(max = 300) String reason,
            @Size(max = 60) String supplierReference,
            LocalDate returnDate) {
    }

    public record PurchaseReturnItemDto(Long id, Long purchaseItemId, Long productId, String productName,
                                        BigDecimal quantity, BigDecimal unitCost, BigDecimal amount) {
    }

    public record PurchaseReturnDto(Long id, String returnNumber, Long purchaseId, LocalDate returnDate, String reason,
                                    BigDecimal totalAmount, String supplierReference, Instant createdAt,
                                    List<PurchaseReturnItemDto> items) {
    }
}
