package com.villagemart.supplier;

import com.villagemart.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String mobile;

    private String address;

    @Column(name = "gst_number")
    private String gstNumber;

    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
