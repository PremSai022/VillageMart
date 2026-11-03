package com.villagemart.purchase;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query(value = """
            select p from Purchase p join fetch p.supplier s
            where (:supplierId is null or s.id = :supplierId)
              and (:status is null or p.status = :status)
              and (:from is null or p.purchaseDate >= :from)
              and (:to is null or p.purchaseDate <= :to)
              and (:q is null or lower(p.purchaseNumber) like :q or lower(p.supplierInvoiceNumber) like :q or lower(s.name) like :q)
            """,
            countQuery = """
            select count(p) from Purchase p join p.supplier s
            where (:supplierId is null or s.id = :supplierId)
              and (:status is null or p.status = :status)
              and (:from is null or p.purchaseDate >= :from)
              and (:to is null or p.purchaseDate <= :to)
              and (:q is null or lower(p.purchaseNumber) like :q or lower(p.supplierInvoiceNumber) like :q or lower(s.name) like :q)
            """)
    Page<Purchase> search(Long supplierId, PurchaseStatus status, LocalDate from, LocalDate to, String q, Pageable pageable);

    @Query("select p from Purchase p join fetch p.supplier where p.id = :id")
    Optional<Purchase> findDetailed(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> lockById(Long id);

    @Query("""
            select count(p) > 0 from Purchase p
            where p.supplier.id = :supplierId and lower(p.supplierInvoiceNumber) = lower(:invoice)
              and p.status <> com.villagemart.purchase.PurchaseStatus.CANCELLED
              and (:excludeId is null or p.id <> :excludeId)
            """)
    boolean existsActiveInvoice(Long supplierId, String invoice, Long excludeId);

    @Query("""
            select count(p), coalesce(sum(p.totalAmount), 0), coalesce(sum(p.returnedAmount), 0)
            from Purchase p where p.supplier.id = :supplierId and p.status = com.villagemart.purchase.PurchaseStatus.COMPLETED
            """)
    List<Object[]> supplierStats(Long supplierId);

    @Query("""
            select p from Purchase p join fetch p.supplier
            where p.status = com.villagemart.purchase.PurchaseStatus.COMPLETED
              and p.purchaseDate between :from and :to
            order by p.purchaseDate, p.id
            """)
    List<Purchase> findCompletedBetween(LocalDate from, LocalDate to);

    List<Purchase> findTop5ByStatusOrderByCompletedAtDesc(PurchaseStatus status);
}
