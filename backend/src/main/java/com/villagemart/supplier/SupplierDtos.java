package com.villagemart.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierDto(Long id, String name, String mobile, String address, String gstNumber, String notes,
                              boolean active, Instant createdAt, Instant updatedAt) {
    }

    public record SupplierDetailDto(SupplierDto supplier, long completedPurchases, BigDecimal totalPurchaseAmount,
                                    BigDecimal totalReturnedAmount) {
    }

    public record SupplierRequest(
            @NotBlank(message = "Supplier name is required") @Size(max = 150) String name,
            @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Mobile must be a valid 10-digit Indian mobile number") String mobile,
            @Size(max = 300) String address,
            @Pattern(regexp = "^$|^\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Invalid GST number") String gstNumber,
            @Size(max = 500) String notes,
            Boolean active) {
    }
}
