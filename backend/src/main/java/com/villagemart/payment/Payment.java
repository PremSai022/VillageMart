package com.villagemart.payment;

import com.villagemart.common.BaseEntity;
import com.villagemart.customer.Customer;
import com.villagemart.sales.Sale;
import com.villagemart.sales.SalesReturn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Immutable money ledger row. Reversals are recorded as new OUT rows; originals are only flagged. */
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "payment_number", nullable = false, unique = true, updatable = false)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_return_id")
    private SalesReturn salesReturn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentDirection direction;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    private String reference;

    private String notes;

    @Column(nullable = false)
    private boolean reversed;

    @Column(name = "created_by")
    private Long createdBy;
}
