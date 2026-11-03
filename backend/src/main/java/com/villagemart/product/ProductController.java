package com.villagemart.product;

import com.villagemart.common.PageResponse;
import com.villagemart.product.ProductDtos.ProductDto;
import com.villagemart.product.ProductDtos.ProductRequest;
import com.villagemart.product.ProductDtos.ProductStatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    public PageResponse<ProductDto> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Boolean active,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String sort) {
        return service.search(q, categoryId, active, page, size, sort);
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/barcode/{barcode}")
    public ProductDto byBarcode(@PathVariable String barcode) {
        return service.getByBarcode(barcode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductDto status(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return service.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
