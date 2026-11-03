package com.villagemart.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false)
    private String idemKey;

    @Column(nullable = false)
    private String operation;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
