package com.villagemart.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryDto(Long id, String name, String description, int sortOrder, boolean active, long productCount) {
    }

    public record CategoryRequest(
            @NotBlank(message = "Category name is required") @Size(max = 80) String name,
            @Size(max = 255) String description,
            @PositiveOrZero Integer sortOrder,
            Boolean active) {
    }
}
