package com.villagemart.expense;

import com.villagemart.common.BaseEntity;
import com.villagemart.village.Village;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    private String description;

    @Column(name = "receipt_image_url")
    private String receiptImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private Village village;

    @Column(nullable = false)
    private boolean deleted;
}
