package com.villagemart.sales;

import com.villagemart.common.BaseEntity;
import com.villagemart.customer.Customer;
import com.villagemart.village.Village;
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
@Table(name = "sales")
public class Sale extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, updatable = false)
    private String invoiceNumber;

    @Column(name = "sale_date", nullable = false, updatable = false)
    private LocalDate saleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, updatable = false)
    private SaleType saleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id", updatable = false)
    private Village village;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, updatable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false, updatable = false)
    private BigDecimal subtotal;

    @Column(name = "item_discount_total", nullable = false, updatable = false)
    private BigDecimal itemDiscountTotal;

    @Column(name = "bill_discount", nullable = false, updatable = false)
    private BigDecimal billDiscount;

    @Column(name = "taxable_amount", nullable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", nullable = false, updatable = false)
    private BigDecimal taxAmount;

    @Column(name = "round_off", nullable = false, updatable = false)
    private BigDecimal roundOff;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private BigDecimal totalAmount;

    @Column(name = "cost_total", nullable = false)
    private BigDecimal costTotal;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "outstanding_amount", nullable = false)
    private BigDecimal outstandingAmount;

    @Column(name = "returned_amount", nullable = false)
    private BigDecimal returnedAmount;

    @Column(name = "returned_revenue", nullable = false)
    private BigDecimal returnedRevenue;

    @Column(name = "returned_cost", nullable = false)
    private BigDecimal returnedCost;

    private String notes;

    @Column(name = "idempotency_key", unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Version
    private Long version;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<SaleItem> items = new ArrayList<>();
}
