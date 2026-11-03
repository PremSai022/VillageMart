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
@Table(name = "purchase_return_items")
public class PurchaseReturnItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_return_id")
    private PurchaseReturn purchaseReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_item_id")
    private PurchaseItem purchaseItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private BigDecimal amount;
}
