package com.villagemart.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(value = """
            select m from StockMovement m join fetch m.product p
            where (:productId is null or p.id = :productId)
              and (:type is null or m.movementType = :type)
              and (:from is null or m.createdAt >= :from)
              and (:to is null or m.createdAt < :to)
            """,
            countQuery = """
            select count(m) from StockMovement m
            where (:productId is null or m.product.id = :productId)
              and (:type is null or m.movementType = :type)
              and (:from is null or m.createdAt >= :from)
              and (:to is null or m.createdAt < :to)
            """)
    Page<StockMovement> search(Long productId, MovementType type, Instant from, Instant to, Pageable pageable);

    boolean existsByProductId(Long productId);

    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByIdAsc(String referenceType, Long referenceId);
}
