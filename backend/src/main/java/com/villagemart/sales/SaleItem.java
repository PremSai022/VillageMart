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

/** Line item with an immutable snapshot of product name, SKU, unit, price and cost at sale time. */
@Getter
@Setter
@Entity
@Table(name = "sale_items")
public class SaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", updatable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", updatable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(name = "category_name", updatable = false)
    private String categoryName;

    @Column(name = "unit_code", nullable = false, updatable = false)
    private String unitCode;

    @Column(nullable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "list_price", nullable = false, updatable = false)
    private BigDecimal listPrice;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "price_overridden", nullable = false, updatable = false)
    private boolean priceOverridden;

    @Column(name = "discount_amount", nullable = false, updatable = false)
    private BigDecimal discountAmount;

    @Column(name = "bill_discount_share", nullable = false, updatable = false)
    private BigDecimal billDiscountShare;

    @Column(name = "taxable_amount", nullable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "tax_rate", nullable = false, updatable = false)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, updatable = false)
    private BigDecimal taxAmount;

    @Column(name = "line_total", nullable = false, updatable = false)
    private BigDecimal lineTotal;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "cost_total", nullable = false)
    private BigDecimal costTotal;

    @Column(name = "returned_quantity", nullable = false)
    private BigDecimal returnedQuantity;

    public BigDecimal returnableQuantity() {
        return quantity.subtract(returnedQuantity);
    }
}
