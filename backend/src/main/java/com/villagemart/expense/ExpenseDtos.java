package com.villagemart.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseCategoryDto(Long id, String name, boolean active) {
    }

    public record ExpenseCategoryRequest(@NotBlank(message = "Category name is required") @Size(max = 60) String name,
                                         Boolean active) {
    }

    public record ExpenseDto(Long id, Long categoryId, String categoryName, BigDecimal amount, LocalDate expenseDate,
                             String description, String receiptImageUrl, Long villageId, String villageName,
                             Instant createdAt, Instant updatedAt) {
    }

    public record ExpenseRequest(
            @NotNull(message = "Expense category is required") Long categoryId,
            @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull(message = "Expense date is required") LocalDate expenseDate,
            @Size(max = 300) String description,
            @Size(max = 500) String receiptImageUrl,
            Long villageId) {
    }
}
