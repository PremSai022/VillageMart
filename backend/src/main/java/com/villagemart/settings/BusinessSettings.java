package com.villagemart.settings;

import com.villagemart.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "business_settings")
public class BusinessSettings extends BaseEntity {

    @Column(name = "business_name", nullable = false)
    private String businessName;

    private String address;
    private String mobile;
    private String email;

    @Column(name = "gst_enabled", nullable = false)
    private boolean gstEnabled;

    private String gstin;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "INR";

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "invoice_footer")
    private String invoiceFooter;

    @Column(name = "low_stock_push_enabled", nullable = false)
    private boolean lowStockPushEnabled = true;
}
