package com.villagemart.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_sequences")
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_name", nullable = false)
    private String sequenceName;

    @Column(nullable = false)
    private String prefix;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
