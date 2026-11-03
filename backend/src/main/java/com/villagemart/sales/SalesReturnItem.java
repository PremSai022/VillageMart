package com.villagemart.sales;

import com.villagemart.common.BaseEntity;
import com.villagemart.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "sales_return_items")
public class SalesReturnItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_return_id")
    private SalesReturn salesReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id")
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private boolean saleable;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "revenue_reversed", nullable = false)
    private BigDecimal revenueReversed;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "cost_reversed", nullable = false)
    private BigDecimal costReversed;
}
