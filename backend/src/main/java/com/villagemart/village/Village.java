package com.villagemart.village;

import com.villagemart.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "villages")
public class Village extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String district;

    @Column(nullable = false)
    private String state;

    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
