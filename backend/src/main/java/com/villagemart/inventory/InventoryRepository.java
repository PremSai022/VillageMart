package com.villagemart.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id in :productIds and i.location.id = :locationId order by i.product.id")
    List<Inventory> lockForUpdate(Collection<Long> productIds, Long locationId);

    Optional<Inventory> findByProductIdAndLocationId(Long productId, Long locationId);

    List<Inventory> findByProductIdInAndLocationId(Collection<Long> productIds, Long locationId);

    @Query(value = """
            select i from Inventory i join fetch i.product p join fetch p.category c join fetch p.unit u
            where i.location.id = :locationId
              and (:status is null or i.stockStatus = :status)
              and (:q is null or lower(p.name) like :q or lower(p.sku) like :q)
              and (:categoryId is null or c.id = :categoryId)
              and (:includeInactive = true or p.active = true)
            """,
            countQuery = """
            select count(i) from Inventory i join i.product p
            where i.location.id = :locationId
              and (:status is null or i.stockStatus = :status)
              and (:q is null or lower(p.name) like :q or lower(p.sku) like :q)
              and (:categoryId is null or p.category.id = :categoryId)
              and (:includeInactive = true or p.active = true)
            """)
    Page<Inventory> search(Long locationId, StockStatus status, String q, Long categoryId, boolean includeInactive,
                           Pageable pageable);

    @Query("""
            select i from Inventory i join fetch i.product p join fetch p.category join fetch p.unit
            where i.location.id = :locationId and i.stockStatus = :status and p.active = true
            order by p.name
            """)
    List<Inventory> findActiveByStatus(Long locationId, StockStatus status);

    @Query("""
            select count(i), coalesce(sum(i.quantity), 0), coalesce(sum(i.quantity * i.averageCost), 0)
            from Inventory i where i.location.id = :locationId and i.product.active = true
            """)
    List<Object[]> totals(Long locationId);

    @Query("""
            select count(i) from Inventory i
            where i.location.id = :locationId and i.stockStatus = :status and i.product.active = true
            """)
    long countActiveByStatus(Long locationId, StockStatus status);
}
