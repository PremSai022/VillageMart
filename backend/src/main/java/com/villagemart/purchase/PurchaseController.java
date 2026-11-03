package com.villagemart.purchase;

import com.villagemart.common.PageResponse;
import com.villagemart.purchase.PurchaseDtos.CancelRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseSummaryDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Purchases")
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService service;

    @GetMapping
    public PageResponse<PurchaseSummaryDto> list(@RequestParam(required = false) Long supplierId,
                                                 @RequestParam(required = false) PurchaseStatus status,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(supplierId, status, from, to, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDto create(@Valid @RequestBody PurchaseRequest request,
                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.create(request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public PurchaseDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PurchaseDto update(@PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {
        return service.updateDraft(id, request);
    }

    @PostMapping("/{id}/complete")
    public PurchaseDto complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return service.cancel(id, request);
    }

    @PostMapping("/{id}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseReturnDto createReturn(@PathVariable Long id, @Valid @RequestBody PurchaseReturnRequest request) {
        return service.createReturn(id, request);
    }

    @GetMapping("/{id}/returns")
    public List<PurchaseReturnDto> returns(@PathVariable Long id) {
        return service.returns(id);
    }
}
