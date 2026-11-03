package com.villagemart.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query(value = """
            select e from Expense e join fetch e.category c left join fetch e.village v
            where e.deleted = false
              and (:categoryId is null or c.id = :categoryId)
              and (:villageId is null or v.id = :villageId)
              and (:from is null or e.expenseDate >= :from)
              and (:to is null or e.expenseDate <= :to)
            """,
            countQuery = """
            select count(e) from Expense e
            where e.deleted = false
              and (:categoryId is null or e.category.id = :categoryId)
              and (:villageId is null or e.village.id = :villageId)
              and (:from is null or e.expenseDate >= :from)
              and (:to is null or e.expenseDate <= :to)
            """)
    Page<Expense> search(Long categoryId, Long villageId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("select e from Expense e join fetch e.category left join fetch e.village where e.id = :id and e.deleted = false")
    Optional<Expense> findActive(Long id);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.deleted = false and e.expenseDate between :from and :to")
    BigDecimal totalBetween(LocalDate from, LocalDate to);

    /** Rows: categoryId, categoryName, total, count. */
    @Query("""
            select c.id, c.name, coalesce(sum(e.amount), 0), count(e) from Expense e join e.category c
            where e.deleted = false and e.expenseDate between :from and :to
            group by c.id, c.name order by sum(e.amount) desc
            """)
    List<Object[]> totalsByCategory(LocalDate from, LocalDate to);

    /** Rows: date, total. */
    @Query("""
            select e.expenseDate, coalesce(sum(e.amount), 0) from Expense e
            where e.deleted = false and e.expenseDate between :from and :to
            group by e.expenseDate order by e.expenseDate
            """)
    List<Object[]> totalsByDate(LocalDate from, LocalDate to);

    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);
}
