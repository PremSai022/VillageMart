package com.villagemart.payment;

import com.villagemart.common.PageResponse;
import com.villagemart.payment.PaymentDtos.PaymentDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping
    public PageResponse<PaymentDto> list(@RequestParam(required = false) Long customerId,
                                         @RequestParam(required = false) PaymentMethod method,
                                         @RequestParam(required = false) PaymentType type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "30") int size) {
        return service.search(customerId, method, type, from, to, page, size);
    }
}
