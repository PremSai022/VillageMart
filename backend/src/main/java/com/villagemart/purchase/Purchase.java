package com.villagemart.purchase;

import com.villagemart.common.BaseEntity;
import com.villagemart.supplier.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "purchases")
public class Purchase extends BaseEntity {

    @Column(name = "purchase_number", nullable = false, unique = true, updatable = false)
    private String purchaseNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "supplier_invoice_number")
    private String supplierInvoiceNumber;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "transport_charges", nullable = false)
    private BigDecimal transportCharges;

    @Column(name = "other_charges", nullable = false)
    private BigDecimal otherCharges;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "returned_amount", nullable = false)
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    private String notes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Version
    private Long version;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PurchaseItem> items = new ArrayList<>();
}
