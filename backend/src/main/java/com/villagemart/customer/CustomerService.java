package com.villagemart.customer;

import com.villagemart.common.ApiException;
import com.villagemart.common.IdempotencyService;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.customer.CustomerDtos.CollectionResultDto;
import com.villagemart.customer.CustomerDtos.CustomerDetailDto;
import com.villagemart.customer.CustomerDtos.CustomerDto;
import com.villagemart.customer.CustomerDtos.CustomerRequest;
import com.villagemart.payment.Payment;
import com.villagemart.payment.PaymentDirection;
import com.villagemart.payment.PaymentDtos.CreditCollectionRequest;
import com.villagemart.payment.PaymentService;
import com.villagemart.payment.PaymentType;
import com.villagemart.sales.Sale;
import com.villagemart.sales.SaleRepository;
import com.villagemart.village.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final Set<String> SORTABLE = Set.of("name", "createdAt");
    private static final String COLLECTION_OPERATION = "CREDIT_COLLECTION";

    private final CustomerRepository repository;
    private final VillageService villageService;
    private final SaleRepository saleRepository;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> search(String q, Long villageId, Boolean active, int page, int size, String sort) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        Page<Customer> result = repository.search(like, villageId, active, Pages.of(page, size, sort, SORTABLE, Sort.by("name")));
        Map<Long, BigDecimal> outstanding = new HashMap<>();
        if (!result.isEmpty()) {
            for (Object[] row : saleRepository.outstandingForCustomers(result.getContent().stream().map(Customer::getId).toList())) {
                outstanding.put((Long) row[0], (BigDecimal) row[1]);
            }
        }
        return PageResponse.of(result, c -> toDto(c, outstanding.getOrDefault(c.getId(), BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public CustomerDetailDto detail(Long id) {
        Customer c = get(id);
        Object[] t = saleRepository.customerTotals(id).get(0);
        BigDecimal outstanding = Money.money(saleRepository.outstandingForCustomer(id));
        return new CustomerDetailDto(toDto(c, outstanding), ((Number) t[0]).longValue(), Money.money((BigDecimal) t[1]),
                outstanding, (LocalDate) t[4]);
    }

    @Transactional
    public CustomerDto create(CustomerRequest r) {
        Customer c = new Customer();
        apply(c, r);
        return toDto(repository.save(c), BigDecimal.ZERO);
    }

    @Transactional
    public Customer createEntity(CustomerRequest r) {
        Customer c = new Customer();
        apply(c, r);
        return repository.save(c);
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest r) {
        Customer c = get(id);
        apply(c, r);
        return toDto(c, Money.money(saleRepository.outstandingForCustomer(id)));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repository.findDetailed(id).orElseThrow(() -> ApiException.notFound("Customer", id));
    }

    /** Collects money against a customer's credit balance, allocating to the oldest outstanding invoices first. */
    @Transactional
    public CollectionResultDto collect(Long customerId, CreditCollectionRequest r, String idempotencyKey) {
        idempotencyService.validateKey(idempotencyKey);
        String hash = idempotencyService.hash(new Object[]{customerId, r});
        Customer customer = get(customerId);
        if (idempotencyService.findExisting(COLLECTION_OPERATION, idempotencyKey, hash).isPresent()) {
            return new CollectionResultDto(customerId, Money.money(r.amount()),
                    Money.money(saleRepository.outstandingForCustomer(customerId)), true, List.of());
        }
        List<Sale> open = saleRepository.lockOutstandingForCustomer(customerId);
        BigDecimal outstanding = open.stream().map(Sale::getOutstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = Money.money(r.amount());
        if (outstanding.signum() == 0) {
            throw ApiException.business("NO_OUTSTANDING", customer.getName() + " has no outstanding balance");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw ApiException.business("AMOUNT_EXCEEDS_OUTSTANDING",
                    "Amount ₹%s exceeds outstanding balance ₹%s".formatted(amount.toPlainString(), Money.money(outstanding).toPlainString()));
        }
        BigDecimal remaining = amount;
        List<Payment> payments = new ArrayList<>();
        for (Sale sale : open) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal alloc = remaining.min(sale.getOutstandingAmount());
            sale.setOutstandingAmount(Money.money(sale.getOutstandingAmount().subtract(alloc)));
            sale.setPaidAmount(Money.money(sale.getPaidAmount().add(alloc)));
            payments.add(paymentService.record(sale, customer, null, PaymentType.CREDIT_COLLECTION, r.method(),
                    PaymentDirection.IN, alloc, r.paymentDate(), r.reference(), r.notes()));
            remaining = remaining.subtract(alloc);
        }
        idempotencyService.record(COLLECTION_OPERATION, idempotencyKey, hash, payments.get(0).getId());
        return new CollectionResultDto(customerId, amount, Money.money(outstanding.subtract(amount)), false,
                payments.stream().map(PaymentService::toDto).toList());
    }

    private void apply(Customer c, CustomerRequest r) {
        String mobile = r.mobile() == null || r.mobile().isBlank() ? null : r.mobile().trim();
        if (mobile != null) {
            repository.findByMobile(mobile).ifPresent(existing -> {
                if (!existing.getId().equals(c.getId())) {
                    throw ApiException.conflict("DUPLICATE_MOBILE", "Mobile " + mobile + " already belongs to " + existing.getName());
                }
            });
        }
        c.setName(r.name().trim());
        c.setMobile(mobile);
        c.setVillage(r.villageId() == null ? null : villageService.get(r.villageId()));
        c.setAddress(r.address() == null || r.address().isBlank() ? null : r.address().trim());
        c.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        if (r.active() != null) {
            c.setActive(r.active());
        }
    }

    public static CustomerDto toDto(Customer c, BigDecimal outstanding) {
        return new CustomerDto(c.getId(), c.getName(), c.getMobile(),
                c.getVillage() == null ? null : c.getVillage().getId(),
                c.getVillage() == null ? null : c.getVillage().getName(),
                c.getAddress(), c.getNotes(), c.isActive(), Money.money(outstanding), c.getCreatedAt(), c.getUpdatedAt());
    }
}
