package com.villagemart.purchase;

import com.villagemart.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_returns")
public class PurchaseReturn extends BaseEntity {

    @Column(name = "return_number", nullable = false, unique = true, updatable = false)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false)
    private String reason;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "supplier_reference")
    private String supplierReference;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<PurchaseReturnItem> items = new ArrayList<>();
}
