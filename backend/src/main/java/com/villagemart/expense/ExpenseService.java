package com.villagemart.expense;

import com.villagemart.common.ApiException;
import com.villagemart.common.BusinessClock;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.expense.ExpenseDtos.ExpenseCategoryDto;
import com.villagemart.expense.ExpenseDtos.ExpenseCategoryRequest;
import com.villagemart.expense.ExpenseDtos.ExpenseDto;
import com.villagemart.expense.ExpenseDtos.ExpenseRequest;
import com.villagemart.village.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Set<String> SORTABLE = Set.of("expenseDate", "amount", "createdAt");

    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final VillageService villageService;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public PageResponse<ExpenseDto> search(Long categoryId, Long villageId, LocalDate from, LocalDate to, int page,
                                           int size, String sort) {
        return PageResponse.of(repository.search(categoryId, villageId, from, to,
                Pages.of(page, size, sort, SORTABLE, Sort.by(Sort.Direction.DESC, "expenseDate", "id"))), ExpenseService::toDto);
    }

    @Transactional(readOnly = true)
    public ExpenseDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public ExpenseDto create(ExpenseRequest r) {
        Expense e = new Expense();
        apply(e, r);
        return toDto(repository.save(e));
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseRequest r) {
        Expense e = find(id);
        apply(e, r);
        return toDto(e);
    }

    /** Soft delete: the record is retained for audit but excluded from all totals. */
    @Transactional
    public void delete(Long id) {
        find(id).setDeleted(true);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryDto> categories() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(ExpenseService::toDto).toList();
    }

    @Transactional
    public ExpenseCategoryDto createCategory(ExpenseCategoryRequest r) {
        ExpenseCategory c = new ExpenseCategory();
        applyCategory(c, r);
        return toDto(categoryRepository.save(c));
    }

    @Transactional
    public ExpenseCategoryDto updateCategory(Long id, ExpenseCategoryRequest r) {
        ExpenseCategory c = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Expense category", id));
        applyCategory(c, r);
        return toDto(c);
    }

    private void applyCategory(ExpenseCategory c, ExpenseCategoryRequest r) {
        String name = r.name().trim();
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(c.getId())) {
                throw ApiException.conflict("DUPLICATE_CATEGORY", "Expense category '" + name + "' already exists");
            }
        });
        c.setName(name);
        if (r.active() != null) {
            c.setActive(r.active());
        }
    }

    private Expense find(Long id) {
        return repository.findActive(id).orElseThrow(() -> ApiException.notFound("Expense", id));
    }

    private void apply(Expense e, ExpenseRequest r) {
        if (r.expenseDate().isAfter(clock.today())) {
            throw ApiException.business("FUTURE_DATE", "Expense date cannot be in the future");
        }
        ExpenseCategory category = categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> ApiException.notFound("Expense category", r.categoryId()));
        if (!category.isActive() && (e.getCategory() == null || !e.getCategory().getId().equals(category.getId()))) {
            throw ApiException.business("INACTIVE_CATEGORY", "Expense category '" + category.getName() + "' is inactive");
        }
        e.setCategory(category);
        e.setAmount(Money.money(r.amount()));
        e.setExpenseDate(r.expenseDate());
        e.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        e.setReceiptImageUrl(r.receiptImageUrl() == null || r.receiptImageUrl().isBlank() ? null : r.receiptImageUrl().trim());
        e.setVillage(r.villageId() == null ? null : villageService.get(r.villageId()));
    }

    static ExpenseCategoryDto toDto(ExpenseCategory c) {
        return new ExpenseCategoryDto(c.getId(), c.getName(), c.isActive());
    }

    static ExpenseDto toDto(Expense e) {
        return new ExpenseDto(e.getId(), e.getCategory().getId(), e.getCategory().getName(), e.getAmount(),
                e.getExpenseDate(), e.getDescription(), e.getReceiptImageUrl(),
                e.getVillage() == null ? null : e.getVillage().getId(),
                e.getVillage() == null ? null : e.getVillage().getName(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
