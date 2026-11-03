package com.villagemart.inventory;

import com.villagemart.common.BaseEntity;
import com.villagemart.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private StockLocation location;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false)
    private BigDecimal averageCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false)
    private StockStatus stockStatus;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    @Version
    private Long version;
}
