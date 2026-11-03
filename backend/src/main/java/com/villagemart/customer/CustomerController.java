package com.villagemart.customer;

import com.villagemart.common.PageResponse;
import com.villagemart.customer.CustomerDtos.CollectionResultDto;
import com.villagemart.customer.CustomerDtos.CustomerDetailDto;
import com.villagemart.customer.CustomerDtos.CustomerDto;
import com.villagemart.customer.CustomerDtos.CustomerRequest;
import com.villagemart.payment.PaymentDtos.CreditCollectionRequest;
import com.villagemart.payment.PaymentDtos.PaymentDto;
import com.villagemart.payment.PaymentService;
import com.villagemart.sales.SaleDtos.SaleSummaryDto;
import com.villagemart.sales.SaleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;
    private final SaleService saleService;
    private final PaymentService paymentService;

    @GetMapping
    public PageResponse<CustomerDto> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Long villageId,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String sort) {
        return service.search(q, villageId, active, page, size, sort);
    }

    @GetMapping("/{id}")
    public CustomerDetailDto get(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@Valid @RequestBody CustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/history")
    public PageResponse<SaleSummaryDto> history(@PathVariable Long id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        service.get(id);
        return saleService.search(id, null, null, null, null, null, null, page, size);
    }

    @GetMapping("/{id}/payments")
    public PageResponse<PaymentDto> payments(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        service.get(id);
        return paymentService.search(id, null, null, null, null, page, size);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<CollectionResultDto> collect(@PathVariable Long id,
                                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                       @Valid @RequestBody CreditCollectionRequest request) {
        CollectionResultDto result = service.collect(id, request, idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
}
