package com.villagemart.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOperationAndIdemKey(String operation, String idemKey);
}
