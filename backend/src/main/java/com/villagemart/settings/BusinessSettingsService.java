package com.villagemart.settings;

import com.villagemart.common.ApiException;
import com.villagemart.config.AppProperties;
import com.villagemart.settings.BusinessSettingsDtos.BusinessSettingsDto;
import com.villagemart.settings.BusinessSettingsDtos.UpdateBusinessSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(2)
@RequiredArgsConstructor
public class BusinessSettingsService implements ApplicationRunner {

    private final BusinessSettingsRepository repository;
    private final AppProperties properties;

    /** Creates the settings row from environment defaults on first start-up. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            BusinessSettings s = new BusinessSettings();
            AppProperties.Business b = properties.business();
            s.setBusinessName(b != null && b.name() != null && !b.name().isBlank() ? b.name() : "VillageMart");
            s.setAddress(b == null ? null : blankToNull(b.address()));
            s.setMobile(b == null ? null : blankToNull(b.mobile()));
            s.setInvoiceFooter("Thank you for shopping with us!");
            repository.save(s);
        }
    }

    @Transactional(readOnly = true)
    public BusinessSettings getEntity() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> ApiException.notFound("Business settings", "default"));
    }

    @Transactional(readOnly = true)
    public BusinessSettingsDto get() {
        return toDto(getEntity());
    }

    @Transactional
    public BusinessSettingsDto update(UpdateBusinessSettingsRequest r) {
        if (r.gstEnabled() && (r.gstin() == null || r.gstin().isBlank())) {
            throw ApiException.business("GSTIN_REQUIRED", "GSTIN is required when GST is enabled");
        }
        BusinessSettings s = repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> ApiException.notFound("Business settings", "default"));
        s.setBusinessName(r.businessName().trim());
        s.setAddress(blankToNull(r.address()));
        s.setMobile(blankToNull(r.mobile()));
        s.setEmail(blankToNull(r.email()));
        s.setGstEnabled(r.gstEnabled());
        s.setGstin(blankToNull(r.gstin()));
        s.setStateName(blankToNull(r.stateName()));
        s.setInvoiceFooter(blankToNull(r.invoiceFooter()));
        s.setLowStockPushEnabled(r.lowStockPushEnabled());
        return toDto(s);
    }

    private static BusinessSettingsDto toDto(BusinessSettings s) {
        return new BusinessSettingsDto(s.getId(), s.getBusinessName(), s.getAddress(), s.getMobile(), s.getEmail(),
                s.isGstEnabled(), s.getGstin(), s.getStateName(), s.getCurrencyCode(), s.getTimezone(),
                s.getInvoiceFooter(), s.isLowStockPushEnabled());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
