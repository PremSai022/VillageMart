package com.villagemart.customer;

import com.villagemart.common.BaseEntity;
import com.villagemart.village.Village;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String mobile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private Village village;

    private String address;

    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
