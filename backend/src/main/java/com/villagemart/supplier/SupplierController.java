package com.villagemart.supplier;

import com.villagemart.common.PageResponse;
import com.villagemart.purchase.PurchaseDtos.PurchaseSummaryDto;
import com.villagemart.purchase.PurchaseService;
import com.villagemart.supplier.SupplierDtos.SupplierDetailDto;
import com.villagemart.supplier.SupplierDtos.SupplierDto;
import com.villagemart.supplier.SupplierDtos.SupplierRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Suppliers")
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;
    private final PurchaseService purchaseService;

    @GetMapping
    public PageResponse<SupplierDto> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String sort) {
        return service.search(q, active, page, size, sort);
    }

    @GetMapping("/{id}")
    public SupplierDetailDto get(@PathVariable Long id) {
        return service.detail(id);
    }

    @GetMapping("/{id}/purchases")
    public PageResponse<PurchaseSummaryDto> purchases(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        service.get(id);
        return purchaseService.search(id, null, null, null, null, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto create(@Valid @RequestBody SupplierRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return service.update(id, request);
    }
}
