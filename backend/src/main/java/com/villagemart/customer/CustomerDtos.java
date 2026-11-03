package com.villagemart.customer;

import com.villagemart.payment.PaymentDtos.PaymentDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerDto(Long id, String name, String mobile, Long villageId, String villageName, String address,
                              String notes, boolean active, BigDecimal outstandingBalance, Instant createdAt,
                              Instant updatedAt) {
    }

    public record CustomerDetailDto(CustomerDto customer, long totalInvoices, BigDecimal totalPurchases,
                                    BigDecimal outstandingBalance, LocalDate lastPurchaseDate) {
    }

    public record CustomerRequest(
            @NotBlank(message = "Customer name is required") @Size(max = 120) String name,
            @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Mobile must be a valid 10-digit Indian mobile number") String mobile,
            Long villageId,
            @Size(max = 300) String address,
            @Size(max = 500) String notes,
            Boolean active) {
    }

    public record CollectionResultDto(Long customerId, BigDecimal amountCollected, BigDecimal outstandingAfter,
                                      boolean replayed, List<PaymentDto> payments) {
    }
}
