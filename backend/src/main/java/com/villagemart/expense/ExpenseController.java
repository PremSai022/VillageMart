package com.villagemart.expense;

import com.villagemart.common.PageResponse;
import com.villagemart.expense.ExpenseDtos.ExpenseCategoryDto;
import com.villagemart.expense.ExpenseDtos.ExpenseCategoryRequest;
import com.villagemart.expense.ExpenseDtos.ExpenseDto;
import com.villagemart.expense.ExpenseDtos.ExpenseRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Expenses")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService service;

    @GetMapping
    public PageResponse<ExpenseDto> list(@RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Long villageId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String sort) {
        return service.search(categoryId, villageId, from, to, page, size, sort);
    }

    @GetMapping("/{id}")
    public ExpenseDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDto create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ExpenseDto update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/categories")
    public List<ExpenseCategoryDto> categories() {
        return service.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseCategoryDto createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        return service.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public ExpenseCategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody ExpenseCategoryRequest request) {
        return service.updateCategory(id, request);
    }
}
