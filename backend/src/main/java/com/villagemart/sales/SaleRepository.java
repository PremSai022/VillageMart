package com.villagemart.sales;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query(value = """
            select s from Sale s left join fetch s.customer c left join fetch s.village v
            where (:customerId is null or c.id = :customerId)
              and (:villageId is null or v.id = :villageId)
              and (:status is null or s.status = :status)
              and (:paymentMode is null or s.paymentMode = :paymentMode)
              and (:from is null or s.saleDate >= :from)
              and (:to is null or s.saleDate <= :to)
              and (:q is null or lower(s.invoiceNumber) like :q or lower(c.name) like :q or c.mobile like :q)
            """,
            countQuery = """
            select count(s) from Sale s left join s.customer c left join s.village v
            where (:customerId is null or c.id = :customerId)
              and (:villageId is null or v.id = :villageId)
              and (:status is null or s.status = :status)
              and (:paymentMode is null or s.paymentMode = :paymentMode)
              and (:from is null or s.saleDate >= :from)
              and (:to is null or s.saleDate <= :to)
              and (:q is null or lower(s.invoiceNumber) like :q or lower(c.name) like :q or c.mobile like :q)
            """)
    Page<Sale> search(Long customerId, Long villageId, SaleStatus status, PaymentMode paymentMode, LocalDate from,
                      LocalDate to, String q, Pageable pageable);

    @Query("select s from Sale s left join fetch s.customer left join fetch s.village where s.id = :id")
    Optional<Sale> findDetailed(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> lockById(Long id);

    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Sale s
            where s.customer.id = :customerId and s.outstandingAmount > 0
              and s.status <> com.villagemart.sales.SaleStatus.CANCELLED
            order by s.saleDate, s.id
            """)
    List<Sale> lockOutstandingForCustomer(Long customerId);

    @Query("""
            select coalesce(sum(s.outstandingAmount), 0) from Sale s
            where s.customer.id = :customerId and s.status <> com.villagemart.sales.SaleStatus.CANCELLED
            """)
    java.math.BigDecimal outstandingForCustomer(Long customerId);

    /** Rows: customerId, outstanding. */
    @Query("""
            select s.customer.id, coalesce(sum(s.outstandingAmount), 0) from Sale s
            where s.customer.id in :customerIds and s.status <> com.villagemart.sales.SaleStatus.CANCELLED
            group by s.customer.id
            """)
    List<Object[]> outstandingForCustomers(Collection<Long> customerIds);

    /** Rows: count, total, net revenue(taxable - returned revenue), net cost. */
    @Query("""
            select count(s), coalesce(sum(s.totalAmount - s.returnedAmount), 0),
                   coalesce(sum(s.taxableAmount - s.returnedRevenue), 0), coalesce(sum(s.costTotal - s.returnedCost), 0),
                   max(s.saleDate)
            from Sale s where s.customer.id = :customerId and s.status <> com.villagemart.sales.SaleStatus.CANCELLED
            """)
    List<Object[]> customerTotals(Long customerId);
}
