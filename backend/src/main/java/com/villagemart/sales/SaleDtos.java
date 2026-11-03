package com.villagemart.sales;

import com.villagemart.customer.CustomerDtos.CustomerRequest;
import com.villagemart.payment.PaymentDtos.PaymentDto;
import com.villagemart.payment.PaymentDtos.PaymentLine;
import com.villagemart.payment.PaymentMethod;
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

public final class SaleDtos {

    private SaleDtos() {
    }

    public record SaleItemRequest(
            @NotNull(message = "Product is required") Long productId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
            @Digits(integer = 11, fraction = 3) BigDecimal quantity,
            @DecimalMin(value = "0.01", message = "Price must be greater than zero") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
            @DecimalMin(value = "0.00", message = "Discount cannot be negative") @Digits(integer = 12, fraction = 2) BigDecimal discountAmount) {
    }

    public record SaleRequest(
            Long customerId,
            @Valid CustomerRequest newCustomer,
            Long villageId,
            LocalDate saleDate,
            @NotEmpty(message = "Add at least one item") @Size(max = 200) List<@Valid @NotNull SaleItemRequest> items,
            @DecimalMin(value = "0.00", message = "Bill discount cannot be negative") @Digits(integer = 12, fraction = 2) BigDecimal billDiscount,
            @NotNull(message = "Payment mode is required") PaymentMode paymentMode,
            @Size(max = 10) List<@Valid @NotNull PaymentLine> payments,
            boolean priceOverrideConfirmed,
            boolean roundOff,
            @Size(max = 500) String notes) {
    }

    public record SaleItemDto(Long id, Long productId, String productName, String sku, String categoryName, String unitCode,
                              BigDecimal quantity, BigDecimal listPrice, BigDecimal unitPrice, boolean priceOverridden,
                              BigDecimal discountAmount, BigDecimal billDiscountShare, BigDecimal taxableAmount,
                              BigDecimal taxRate, BigDecimal taxAmount, BigDecimal lineTotal, BigDecimal unitCost,
                              BigDecimal costTotal, BigDecimal returnedQuantity, BigDecimal returnableQuantity) {
    }

    public record SaleSummaryDto(Long id, String invoiceNumber, LocalDate saleDate, SaleType saleType, Long customerId,
                                 String customerName, Long villageId, String villageName, SaleStatus status,
                                 PaymentMode paymentMode, BigDecimal totalAmount, BigDecimal paidAmount,
                                 BigDecimal outstandingAmount, BigDecimal returnedAmount, Instant createdAt) {
    }

    public record SaleDto(Long id, String invoiceNumber, LocalDate saleDate, SaleType saleType, Long customerId,
                          String customerName, String customerMobile, Long villageId, String villageName,
                          SaleStatus status, PaymentMode paymentMode, BigDecimal subtotal, BigDecimal itemDiscountTotal,
                          BigDecimal billDiscount, BigDecimal taxableAmount, BigDecimal taxAmount, BigDecimal roundOff,
                          BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal outstandingAmount,
                          BigDecimal returnedAmount, BigDecimal costTotal, BigDecimal grossProfit, String notes,
                          Instant cancelledAt, String cancelReason, Instant createdAt, List<SaleItemDto> items,
                          List<PaymentDto> payments, List<SalesReturnDto> returns, boolean replayed) {
    }

    public record CancelSaleRequest(@NotBlank(message = "Cancellation reason is required") @Size(max = 300) String reason) {
    }

    public record SalesReturnItemRequest(
            @NotNull(message = "Sale item is required") Long saleItemId,
            @NotNull @DecimalMin(value = "0.001", message = "Return quantity must be greater than zero")
            @Digits(integer = 11, fraction = 3) BigDecimal quantity,
            boolean saleable) {
    }

    public record SalesReturnRequest(
            @NotEmpty(message = "Select at least one item to return") List<@Valid @NotNull SalesReturnItemRequest> items,
            @NotBlank(message = "Return reason is required") @Size(max = 300) String reason,
            LocalDate returnDate,
            PaymentMethod refundMethod) {
    }

    public record SalesReturnItemDto(Long id, Long saleItemId, Long productId, String productName, String unitCode,
                                     BigDecimal quantity, boolean saleable, BigDecimal refundAmount,
                                     BigDecimal revenueReversed, BigDecimal unitCost, BigDecimal costReversed) {
    }

    public record SalesReturnDto(Long id, String returnNumber, Long saleId, String invoiceNumber, LocalDate returnDate,
                                 String reason, BigDecimal refundAmount, BigDecimal revenueReversed,
                                 BigDecimal costReversed, BigDecimal creditAdjusted, BigDecimal cashRefunded,
                                 PaymentMethod refundMethod, Instant createdAt, List<SalesReturnItemDto> items) {
    }
}
