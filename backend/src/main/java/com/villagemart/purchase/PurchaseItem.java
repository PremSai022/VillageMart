package com.villagemart.purchase;

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
@Table(name = "purchase_items")
public class PurchaseItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_code", nullable = false)
    private String unitCode;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "allocated_charges", nullable = false)
    private BigDecimal allocatedCharges;

    @Column(name = "landed_unit_cost", nullable = false)
    private BigDecimal landedUnitCost;

    @Column(name = "returned_quantity", nullable = false)
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    public BigDecimal returnableQuantity() {
        return quantity.subtract(returnedQuantity);
    }
}
