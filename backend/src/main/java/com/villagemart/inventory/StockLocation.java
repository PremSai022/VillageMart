package com.villagemart.inventory;

import com.villagemart.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A physical stock location. MVP uses a single default "TROLLEY" location; designed for future vehicles/warehouses. */
@Getter
@Setter
@Entity
@Table(name = "stock_locations")
public class StockLocation extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "location_type", nullable = false)
    private String locationType;

    @Column(name = "is_default", nullable = false)
    private boolean defaultLocation;
}
