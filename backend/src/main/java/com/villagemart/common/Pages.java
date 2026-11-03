package com.villagemart.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class Pages {

    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    /**
     * Builds a bounded pageable. {@code sort} has the form {@code field,dir}; only whitelisted fields are accepted.
     */
    public static Pageable of(int page, int size, String sort, Set<String> allowedFields, Sort defaultSort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Sort resolved = defaultSort;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (!allowedFields.contains(field)) {
                throw ApiException.badRequest("INVALID_SORT", "Sorting by '" + field + "' is not supported");
            }
            Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            resolved = Sort.by(dir, field);
        }
        return PageRequest.of(safePage, safeSize, resolved);
    }
}
