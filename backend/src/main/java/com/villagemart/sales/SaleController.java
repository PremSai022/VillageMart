package com.villagemart.sales;

import com.villagemart.common.PageResponse;
import com.villagemart.sales.SaleDtos.CancelSaleRequest;
import com.villagemart.sales.SaleDtos.SaleDto;
import com.villagemart.sales.SaleDtos.SaleRequest;
import com.villagemart.sales.SaleDtos.SaleSummaryDto;
import com.villagemart.sales.SaleDtos.SalesReturnDto;
import com.villagemart.sales.SaleDtos.SalesReturnRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Sales")
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService service;
    private final SalesReturnService returnService;

    @Operation(summary = "Create a sale. Requires an Idempotency-Key header; retries with the same key return the original invoice.")
    @PostMapping
    public ResponseEntity<SaleDto> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                          @Valid @RequestBody SaleRequest request) {
        SaleDto sale = service.create(request, idempotencyKey);
        return ResponseEntity.status(sale.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(sale);
    }

    @GetMapping
    public PageResponse<SaleSummaryDto> list(@RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) Long villageId,
                                             @RequestParam(required = false) SaleStatus status,
                                             @RequestParam(required = false) PaymentMode paymentMode,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String sort) {
        return service.search(customerId, villageId, status, paymentMode, from, to, q, page, size, sort);
    }

    @GetMapping("/{id}")
    public SaleDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public SaleDto cancel(@PathVariable Long id, @Valid @RequestBody CancelSaleRequest request) {
        return service.cancel(id, request);
    }

    @PostMapping("/{id}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public SalesReturnDto createReturn(@PathVariable Long id,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                       @Valid @RequestBody SalesReturnRequest request) {
        return returnService.create(id, request, idempotencyKey);
    }

    @GetMapping("/{id}/returns")
    public List<SalesReturnDto> returns(@PathVariable Long id) {
        return returnService.forSale(id);
    }
}
