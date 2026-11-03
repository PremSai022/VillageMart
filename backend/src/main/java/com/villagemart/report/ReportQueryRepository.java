package com.villagemart.report;

import com.villagemart.payment.PaymentDirection;
import com.villagemart.sales.SaleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Read-only aggregate queries used by reports and the dashboard. */
@Repository
@RequiredArgsConstructor
public class ReportQueryRepository {

    private final EntityManager em;

    /** date, invoices, taxable, tax, total, cost. */
    public List<Object[]> salesByDate(LocalDate from, LocalDate to, Long villageId) {
        return query("""
                select s.saleDate, count(s), coalesce(sum(s.taxableAmount), 0), coalesce(sum(s.taxAmount), 0),
                       coalesce(sum(s.totalAmount), 0), coalesce(sum(s.costTotal), 0)
                from Sale s
                where s.status <> :cancelled and s.saleDate between :from and :to
                  and (:villageId is null or s.village.id = :villageId)
                group by s.saleDate order by s.saleDate
                """, from, to).setParameter("villageId", villageId).getResultList();
    }

    /** date, count, revenueReversed, costReversed, refundAmount. */
    public List<Object[]> returnsByDate(LocalDate from, LocalDate to, Long villageId) {
        return query("""
                select r.returnDate, count(r), coalesce(sum(r.revenueReversed), 0), coalesce(sum(r.costReversed), 0),
                       coalesce(sum(r.refundAmount), 0)
                from SalesReturn r join r.sale s
                where s.status <> :cancelled and r.returnDate between :from and :to
                  and (:villageId is null or s.village.id = :villageId)
                group by r.returnDate order by r.returnDate
                """, from, to).setParameter("villageId", villageId).getResultList();
    }

    /** productId, name, sku, category, unit, qty, taxable, cost. */
    public List<Object[]> productSales(LocalDate from, LocalDate to) {
        return query("""
                select i.product.id, max(i.productName), max(i.sku), max(i.categoryName), max(i.unitCode),
                       coalesce(sum(i.quantity), 0), coalesce(sum(i.taxableAmount), 0), coalesce(sum(i.costTotal), 0)
                from SaleItem i join i.sale s
                where s.status <> :cancelled and s.saleDate between :from and :to
                group by i.product.id
                """, from, to).getResultList();
    }

    /** productId, name, sku, category, unit, qty returned, revenue reversed, cost reversed. */
    public List<Object[]> productReturns(LocalDate from, LocalDate to) {
        return query("""
                select ri.product.id, max(si.productName), max(si.sku), max(si.categoryName), max(si.unitCode),
                       coalesce(sum(ri.quantity), 0), coalesce(sum(ri.revenueReversed), 0), coalesce(sum(ri.costReversed), 0)
                from SalesReturnItem ri join ri.saleItem si join ri.salesReturn r join r.sale s
                where s.status <> :cancelled and r.returnDate between :from and :to
                group by ri.product.id
                """, from, to).getResultList();
    }

    /** villageId, villageName, invoices, distinct customers, total, taxable, cost, outstanding. */
    public List<Object[]> villageSales(LocalDate from, LocalDate to) {
        return query("""
                select v.id, v.name, count(s), count(distinct s.customer.id), coalesce(sum(s.totalAmount), 0),
                       coalesce(sum(s.taxableAmount), 0), coalesce(sum(s.costTotal), 0), coalesce(sum(s.outstandingAmount), 0)
                from Sale s left join s.village v
                where s.status <> :cancelled and s.saleDate between :from and :to
                group by v.id, v.name
                """, from, to).getResultList();
    }

    /** villageId, villageName, revenueReversed, costReversed. */
    public List<Object[]> villageReturns(LocalDate from, LocalDate to) {
        return query("""
                select v.id, v.name, coalesce(sum(r.revenueReversed), 0), coalesce(sum(r.costReversed), 0)
                from SalesReturn r join r.sale s left join s.village v
                where s.status <> :cancelled and r.returnDate between :from and :to
                group by v.id, v.name
                """, from, to).getResultList();
    }

    /** customerId, name, mobile, villageName, outstanding, open invoices, oldest invoice date. */
    public List<Object[]> customerOutstanding() {
        return em.createQuery("""
                select c.id, c.name, c.mobile, v.name, coalesce(sum(s.outstandingAmount), 0), count(s), min(s.saleDate)
                from Sale s join s.customer c left join c.village v
                where s.status <> :cancelled and s.outstandingAmount > 0
                group by c.id, c.name, c.mobile, v.name
                order by coalesce(sum(s.outstandingAmount), 0) desc
                """, Object[].class).setParameter("cancelled", SaleStatus.CANCELLED).getResultList();
    }

    /** Credit extended in the period: billed amount of sales in the period minus the payments taken at billing. */
    public java.math.BigDecimal creditGiven(LocalDate from, LocalDate to) {
        java.math.BigDecimal billed = em.createQuery("""
                select coalesce(sum(s.totalAmount), 0) from Sale s
                where s.status <> :cancelled and s.saleDate between :from and :to
                """, java.math.BigDecimal.class)
                .setParameter("cancelled", SaleStatus.CANCELLED).setParameter("from", from).setParameter("to", to)
                .getSingleResult();
        java.math.BigDecimal paidUpfront = em.createQuery("""
                select coalesce(sum(p.amount), 0) from Payment p join p.sale s
                where s.status <> :cancelled and s.saleDate between :from and :to
                  and p.paymentType = com.villagemart.payment.PaymentType.SALE_PAYMENT and p.direction = :in
                """, java.math.BigDecimal.class)
                .setParameter("cancelled", SaleStatus.CANCELLED).setParameter("from", from).setParameter("to", to)
                .setParameter("in", PaymentDirection.IN).getSingleResult();
        return billed.subtract(paidUpfront);
    }

    public java.math.BigDecimal totalOutstanding() {
        return em.createQuery("""
                select coalesce(sum(s.outstandingAmount), 0) from Sale s where s.status <> :cancelled
                """, java.math.BigDecimal.class).setParameter("cancelled", SaleStatus.CANCELLED).getSingleResult();
    }

    private TypedQuery<Object[]> query(String jpql, LocalDate from, LocalDate to) {
        return em.createQuery(jpql, Object[].class)
                .setParameter("cancelled", SaleStatus.CANCELLED)
                .setParameter("from", from)
                .setParameter("to", to);
    }
}
