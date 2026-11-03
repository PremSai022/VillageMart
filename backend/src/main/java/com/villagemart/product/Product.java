package com.villagemart.product;

import com.villagemart.category.Category;
import com.villagemart.common.BaseEntity;
import com.villagemart.unit.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "min_selling_price")
    private BigDecimal minSellingPrice;

    @Column(name = "min_stock_quantity", nullable = false)
    private BigDecimal minStockQuantity;

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;
}
