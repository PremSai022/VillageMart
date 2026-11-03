package com.villagemart.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record PaymentDto(Long id, String paymentNumber, Long saleId, String invoiceNumber, Long customerId,
                             String customerName, Long salesReturnId, PaymentType paymentType, PaymentMethod method,
                             PaymentDirection direction, BigDecimal amount, LocalDate paymentDate, String reference,
                             String notes, boolean reversed, Instant createdAt) {
    }

    public record PaymentLine(
            @NotNull(message = "Payment method is required") PaymentMethod method,
            @NotNull @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
            @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 80) String reference) {
    }

    public record CreditCollectionRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 12, fraction = 2) BigDecimal amount,
            LocalDate paymentDate,
            @Size(max = 80) String reference,
            @Size(max = 300) String notes) {
    }
}
