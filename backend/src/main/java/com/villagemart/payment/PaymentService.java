package com.villagemart.payment;

import com.villagemart.common.BusinessClock;
import com.villagemart.common.DocumentNumberService;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.customer.Customer;
import com.villagemart.payment.PaymentDtos.PaymentDto;
import com.villagemart.sales.Sale;
import com.villagemart.sales.SalesReturn;
import com.villagemart.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final DocumentNumberService documentNumberService;
    private final BusinessClock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public Payment record(Sale sale, Customer customer, SalesReturn salesReturn, PaymentType type, PaymentMethod method,
                          PaymentDirection direction, BigDecimal amount, LocalDate date, String reference, String notes) {
        Payment p = new Payment();
        p.setPaymentNumber(documentNumberService.next(DocumentNumberService.PAYMENT));
        p.setSale(sale);
        p.setCustomer(customer);
        p.setSalesReturn(salesReturn);
        p.setPaymentType(type);
        p.setMethod(method);
        p.setDirection(direction);
        p.setAmount(Money.money(amount));
        p.setPaymentDate(date == null ? clock.today() : date);
        p.setReference(reference == null || reference.isBlank() ? null : reference.trim());
        p.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        p.setCreatedBy(CurrentUser.idOrNull());
        return repository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Payment> forSale(Long saleId) {
        return repository.findBySaleIdOrderByIdAsc(saleId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentDto> search(Long customerId, PaymentMethod method, PaymentType type, LocalDate from,
                                           LocalDate to, int page, int size) {
        return PageResponse.of(repository.search(customerId, method, type, from, to,
                Pages.of(page, size, null, Set.of(), Sort.by(Sort.Direction.DESC, "paymentDate", "id"))), PaymentService::toDto);
    }

    public static PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getPaymentNumber(),
                p.getSale() == null ? null : p.getSale().getId(),
                p.getSale() == null ? null : p.getSale().getInvoiceNumber(),
                p.getCustomer() == null ? null : p.getCustomer().getId(),
                p.getCustomer() == null ? null : p.getCustomer().getName(),
                p.getSalesReturn() == null ? null : p.getSalesReturn().getId(),
                p.getPaymentType(), p.getMethod(), p.getDirection(), p.getAmount(), p.getPaymentDate(), p.getReference(),
                p.getNotes(), p.isReversed(), p.getCreatedAt());
    }
}
