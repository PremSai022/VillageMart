package com.villagemart.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleIdOrderByIdAsc(Long saleId);

    @Query(value = """
            select p from Payment p left join fetch p.sale s left join fetch p.customer c
            where (:customerId is null or c.id = :customerId)
              and (:method is null or p.method = :method)
              and (:type is null or p.paymentType = :type)
              and (:from is null or p.paymentDate >= :from)
              and (:to is null or p.paymentDate <= :to)
            """,
            countQuery = """
            select count(p) from Payment p
            where (:customerId is null or p.customer.id = :customerId)
              and (:method is null or p.method = :method)
              and (:type is null or p.paymentType = :type)
              and (:from is null or p.paymentDate >= :from)
              and (:to is null or p.paymentDate <= :to)
            """)
    Page<Payment> search(Long customerId, PaymentMethod method, PaymentType type, LocalDate from, LocalDate to,
                         Pageable pageable);

    /** Rows: method, direction, paymentType, sum(amount), count. */
    @Query("""
            select p.method, p.direction, p.paymentType, coalesce(sum(p.amount), 0), count(p) from Payment p
            where p.paymentDate between :from and :to
            group by p.method, p.direction, p.paymentType
            """)
    List<Object[]> totalsBetween(LocalDate from, LocalDate to);
}
