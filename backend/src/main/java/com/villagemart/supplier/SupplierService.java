package com.villagemart.supplier;

import com.villagemart.common.ApiException;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.purchase.PurchaseRepository;
import com.villagemart.supplier.SupplierDtos.SupplierDetailDto;
import com.villagemart.supplier.SupplierDtos.SupplierDto;
import com.villagemart.supplier.SupplierDtos.SupplierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Set<String> SORTABLE = Set.of("name", "createdAt");

    private final SupplierRepository repository;
    private final PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public PageResponse<SupplierDto> search(String q, Boolean active, int page, int size, String sort) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(repository.search(like, active, Pages.of(page, size, sort, SORTABLE, Sort.by("name"))),
                SupplierService::toDto);
    }

    @Transactional(readOnly = true)
    public SupplierDetailDto detail(Long id) {
        Supplier s = get(id);
        Object[] stats = purchaseRepository.supplierStats(id).get(0);
        return new SupplierDetailDto(toDto(s), ((Number) stats[0]).longValue(),
                Money.money(toBig(stats[1])), Money.money(toBig(stats[2])));
    }

    @Transactional
    public SupplierDto create(SupplierRequest r) {
        Supplier s = new Supplier();
        apply(s, r);
        return toDto(repository.save(s));
    }

    @Transactional
    public SupplierDto update(Long id, SupplierRequest r) {
        Supplier s = get(id);
        apply(s, r);
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Supplier", id));
    }

    private void apply(Supplier s, SupplierRequest r) {
        s.setName(r.name().trim());
        s.setMobile(blankToNull(r.mobile()));
        s.setAddress(blankToNull(r.address()));
        s.setGstNumber(r.gstNumber() == null || r.gstNumber().isBlank() ? null : r.gstNumber().trim().toUpperCase());
        s.setNotes(blankToNull(r.notes()));
        if (r.active() != null) {
            s.setActive(r.active());
        }
    }

    public static SupplierDto toDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getMobile(), s.getAddress(), s.getGstNumber(), s.getNotes(),
                s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal toBig(Object o) {
        return o == null ? BigDecimal.ZERO : o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }
}
