package com.villagemart.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class BusinessSettingsDtos {

    private BusinessSettingsDtos() {
    }

    public record BusinessSettingsDto(Long id, String businessName, String address, String mobile, String email,
                                      boolean gstEnabled, String gstin, String stateName, String currencyCode,
                                      String timezone, String invoiceFooter, boolean lowStockPushEnabled) {
    }

    public record UpdateBusinessSettingsRequest(
            @NotBlank @Size(max = 150) String businessName,
            @Size(max = 300) String address,
            @Pattern(regexp = "^$|^[0-9+]{10,15}$", message = "Mobile must be 10-15 digits") String mobile,
            @Email @Size(max = 120) String email,
            boolean gstEnabled,
            @Pattern(regexp = "^$|^[0-9A-Z]{15}$", message = "GSTIN must be 15 uppercase alphanumeric characters") String gstin,
            @Size(max = 60) String stateName,
            @Size(max = 300) String invoiceFooter,
            boolean lowStockPushEnabled) {
    }
}
