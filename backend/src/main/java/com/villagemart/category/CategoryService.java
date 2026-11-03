package com.villagemart.category;

import com.villagemart.category.CategoryDtos.CategoryDto;
import com.villagemart.category.CategoryDtos.CategoryRequest;
import com.villagemart.common.ApiException;
import com.villagemart.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> list(boolean activeOnly) {
        Map<Long, Long> counts = productRepository.countByCategory();
        return repository.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(c -> !activeOnly || c.isActive())
                .map(c -> toDto(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest r) {
        ensureUnique(r.name(), null);
        Category c = new Category();
        apply(c, r);
        repository.save(c);
        return toDto(c, 0);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest r) {
        Category c = get(id);
        ensureUnique(r.name(), id);
        apply(c, r);
        return toDto(c, productRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category c = get(id);
        if (productRepository.countByCategoryId(id) > 0) {
            throw ApiException.conflict("CATEGORY_IN_USE",
                    "Category '" + c.getName() + "' has products. Move or deactivate them first, or deactivate the category.");
        }
        repository.delete(c);
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Category", id));
    }

    private void apply(Category c, CategoryRequest r) {
        c.setName(r.name().trim());
        c.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        if (r.sortOrder() != null) {
            c.setSortOrder(r.sortOrder());
        }
        if (r.active() != null) {
            c.setActive(r.active());
        }
    }

    private void ensureUnique(String name, Long currentId) {
        repository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw ApiException.conflict("DUPLICATE_CATEGORY", "Category '" + name.trim() + "' already exists");
            }
        });
    }

    private static CategoryDto toDto(Category c, long count) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getSortOrder(), c.isActive(), count);
    }
}
