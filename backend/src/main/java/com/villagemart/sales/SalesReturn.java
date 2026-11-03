package com.villagemart.sales;

import com.villagemart.common.BaseEntity;
import com.villagemart.payment.PaymentMethod;
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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "sales_returns")
public class SalesReturn extends BaseEntity {

    @Column(name = "return_number", nullable = false, unique = true, updatable = false)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", updatable = false)
    private Sale sale;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false)
    private String reason;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "revenue_reversed", nullable = false)
    private BigDecimal revenueReversed;

    @Column(name = "cost_reversed", nullable = false)
    private BigDecimal costReversed;

    @Column(name = "credit_adjusted", nullable = false)
    private BigDecimal creditAdjusted;

    @Column(name = "cash_refunded", nullable = false)
    private BigDecimal cashRefunded;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method")
    private PaymentMethod refundMethod;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<SalesReturnItem> items = new ArrayList<>();
}
