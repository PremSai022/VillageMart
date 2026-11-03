package com.villagemart.common;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DocumentSequence s where s.sequenceName = :name")
    Optional<DocumentSequence> lockByName(String name);
}
