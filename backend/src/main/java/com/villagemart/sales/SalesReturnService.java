package com.villagemart.sales;

import com.villagemart.common.ApiException;
import com.villagemart.common.BusinessClock;
import com.villagemart.common.DocumentNumberService;
import com.villagemart.common.IdempotencyService;
import com.villagemart.common.Money;
import com.villagemart.inventory.InventoryService;
import com.villagemart.inventory.MovementType;
import com.villagemart.inventory.StockChange;
import com.villagemart.payment.PaymentDirection;
import com.villagemart.payment.PaymentMethod;
import com.villagemart.payment.PaymentService;
import com.villagemart.payment.PaymentType;
import com.villagemart.sales.SaleDtos.SalesReturnDto;
import com.villagemart.sales.SaleDtos.SalesReturnItemDto;
import com.villagemart.sales.SaleDtos.SalesReturnItemRequest;
import com.villagemart.sales.SaleDtos.SalesReturnRequest;
import com.villagemart.security.CurrentUser;
import com.villagemart.unit.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sales returns against an original invoice. Saleable goods go back into stock at their original cost; damaged goods
 * are refunded but neither restocked nor removed from cost of goods sold. Refunds first settle outstanding credit.
 */
@Service
@RequiredArgsConstructor
public class SalesReturnService {

    public static final String REF_SALES_RETURN = "SALES_RETURN";
    private static final String IDEMPOTENCY_OPERATION = "SALES_RETURN_CREATE";

    private final SaleRepository saleRepository;
    private final SalesReturnRepository repository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final DocumentNumberService documentNumberService;
    private final IdempotencyService idempotencyService;
    private final BusinessClock clock;

    @Transactional
    public SalesReturnDto create(Long saleId, SalesReturnRequest r, String idempotencyKey) {
        String hash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.validateKey(idempotencyKey);
            hash = idempotencyService.hash(new Object[]{saleId, r});
            var existing = idempotencyService.findExisting(IDEMPOTENCY_OPERATION, idempotencyKey, hash);
            if (existing.isPresent()) {
                return toDto(repository.findById(existing.get()).orElseThrow());
            }
        }
        Sale sale = saleRepository.lockById(saleId).orElseThrow(() -> ApiException.notFound("Sale", saleId));
        if (sale.getStatus() == SaleStatus.CANCELLED || sale.getStatus() == SaleStatus.FULLY_RETURNED) {
            throw ApiException.business("NOT_RETURNABLE", "Sale " + sale.getInvoiceNumber() + " is " + sale.getStatus() + " and cannot be returned");
        }
        LocalDate date = r.returnDate() == null ? clock.today() : r.returnDate();
        if (date.isAfter(clock.today()) || date.isBefore(sale.getSaleDate())) {
            throw ApiException.business("INVALID_RETURN_DATE", "Return date must be between the sale date and today");
        }
        PaymentMethod refundMethod = r.refundMethod() == null ? PaymentMethod.CASH : r.refundMethod();

        Map<Long, SaleItem> saleItems = new HashMap<>();
        sale.getItems().forEach(i -> saleItems.put(i.getId(), i));
        Map<Long, BigDecimal[]> previous = previousTotals(saleId);
        Set<Long> seen = new HashSet<>();

        SalesReturn ret = new SalesReturn();
        ret.setReturnNumber(documentNumberService.next(DocumentNumberService.SALES_RETURN));
        ret.setSale(sale);
        ret.setReturnDate(date);
        ret.setReason(r.reason().trim());
        ret.setCreatedBy(CurrentUser.idOrNull());

        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (SalesReturnItemRequest req : r.items()) {
            if (!seen.add(req.saleItemId())) {
                throw ApiException.badRequest("DUPLICATE_ITEM", "Each invoice line can appear only once in a return");
            }
            SaleItem si = saleItems.get(req.saleItemId());
            if (si == null) {
                throw ApiException.badRequest("ITEM_NOT_IN_SALE", "Item " + req.saleItemId() + " does not belong to invoice " + sale.getInvoiceNumber());
            }
            BigDecimal qty = Money.qty(req.quantity());
            UnitService.validateQuantity(si.getProduct().getUnit(), qty, si.getProductName());
            BigDecimal returnable = si.returnableQuantity();
            if (qty.compareTo(returnable) > 0) {
                throw ApiException.business("RETURN_EXCEEDS_SOLD", "Only " + returnable.stripTrailingZeros().toPlainString()
                        + " " + si.getUnitCode() + " of " + si.getProductName() + " can be returned");
            }
            boolean lineComplete = qty.compareTo(returnable) == 0;
            BigDecimal[] prev = previous.getOrDefault(si.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal itemRefund = lineComplete ? si.getLineTotal().subtract(prev[0]) : prorate(si.getLineTotal(), qty, si.getQuantity());
            BigDecimal itemRevenue = lineComplete ? si.getTaxableAmount().subtract(prev[1]) : prorate(si.getTaxableAmount(), qty, si.getQuantity());
            BigDecimal itemCost = req.saleable() ? Money.money(si.getUnitCost().multiply(qty)) : Money.money(BigDecimal.ZERO);

            SalesReturnItem ri = new SalesReturnItem();
            ri.setSalesReturn(ret);
            ri.setSaleItem(si);
            ri.setProduct(si.getProduct());
            ri.setQuantity(qty);
            ri.setSaleable(req.saleable());
            ri.setRefundAmount(Money.money(itemRefund));
            ri.setRevenueReversed(Money.money(itemRevenue));
            ri.setUnitCost(si.getUnitCost());
            ri.setCostReversed(itemCost);
            ret.getItems().add(ri);
            si.setReturnedQuantity(Money.qty(si.getReturnedQuantity().add(qty)));
            refund = refund.add(ri.getRefundAmount());
            revenue = revenue.add(ri.getRevenueReversed());
            cost = cost.add(itemCost);
        }

        boolean fullyReturned = sale.getItems().stream().allMatch(i -> i.returnableQuantity().signum() == 0);
        BigDecimal remaining = sale.getTotalAmount().subtract(sale.getReturnedAmount());
        refund = fullyReturned ? remaining : refund.min(remaining);
        refund = Money.money(refund);
        BigDecimal creditAdjusted = refund.min(sale.getOutstandingAmount());
        BigDecimal cashRefund = Money.money(refund.subtract(creditAdjusted));

        ret.setRefundAmount(refund);
        ret.setRevenueReversed(Money.money(revenue));
        ret.setCostReversed(Money.money(cost));
        ret.setCreditAdjusted(Money.money(creditAdjusted));
        ret.setCashRefunded(cashRefund);
        ret.setRefundMethod(cashRefund.signum() > 0 ? refundMethod : null);
        repository.saveAndFlush(ret);

        List<StockChange> changes = new ArrayList<>();
        for (SalesReturnItem ri : ret.getItems()) {
            if (ri.isSaleable()) {
                changes.add(StockChange.inbound(ri.getProduct().getId(), MovementType.SALE_RETURN, ri.getQuantity(),
                        ri.getUnitCost(), REF_SALES_RETURN, ret.getId(), ret.getReturnNumber(),
                        "Return against " + sale.getInvoiceNumber() + ": " + ret.getReason()));
            }
        }
        inventoryService.applyAll(changes);

        if (cashRefund.signum() > 0) {
            paymentService.record(sale, sale.getCustomer(), ret, PaymentType.SALE_REFUND, refundMethod, PaymentDirection.OUT,
                    cashRefund, date, ret.getReturnNumber(), ret.getReason());
        }
        sale.setOutstandingAmount(Money.money(sale.getOutstandingAmount().subtract(creditAdjusted)));
        sale.setPaidAmount(Money.money(sale.getPaidAmount().subtract(cashRefund)));
        sale.setReturnedAmount(Money.money(sale.getReturnedAmount().add(refund)));
        sale.setReturnedRevenue(Money.money(sale.getReturnedRevenue().add(revenue)));
        sale.setReturnedCost(Money.money(sale.getReturnedCost().add(cost)));
        sale.setStatus(fullyReturned ? SaleStatus.FULLY_RETURNED : SaleStatus.PARTIALLY_RETURNED);

        if (hash != null) {
            idempotencyService.record(IDEMPOTENCY_OPERATION, idempotencyKey, hash, ret.getId());
        }
        return toDto(ret);
    }

    @Transactional(readOnly = true)
    public List<SalesReturnDto> forSale(Long saleId) {
        if (!saleRepository.existsById(saleId)) {
            throw ApiException.notFound("Sale", saleId);
        }
        return repository.findBySaleIdOrderByIdAsc(saleId).stream().map(SalesReturnService::toDto).toList();
    }

    /** Per sale item: [refund so far, revenue reversed so far]. */
    private Map<Long, BigDecimal[]> previousTotals(Long saleId) {
        Map<Long, BigDecimal[]> totals = new HashMap<>();
        for (SalesReturn sr : repository.findBySaleIdOrderByIdAsc(saleId)) {
            for (SalesReturnItem ri : sr.getItems()) {
                BigDecimal[] t = totals.computeIfAbsent(ri.getSaleItem().getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                t[0] = t[0].add(ri.getRefundAmount());
                t[1] = t[1].add(ri.getRevenueReversed());
            }
        }
        return totals;
    }

    private static BigDecimal prorate(BigDecimal amount, BigDecimal qty, BigDecimal totalQty) {
        return Money.money(amount.multiply(qty).divide(totalQty, 6, RoundingMode.HALF_UP));
    }

    public static SalesReturnDto toDto(SalesReturn r) {
        return new SalesReturnDto(r.getId(), r.getReturnNumber(), r.getSale().getId(), r.getSale().getInvoiceNumber(),
                r.getReturnDate(), r.getReason(), r.getRefundAmount(), r.getRevenueReversed(), r.getCostReversed(),
                r.getCreditAdjusted(), r.getCashRefunded(), r.getRefundMethod(), r.getCreatedAt(),
                r.getItems().stream().map(i -> new SalesReturnItemDto(i.getId(), i.getSaleItem().getId(),
                        i.getProduct().getId(), i.getSaleItem().getProductName(), i.getSaleItem().getUnitCode(),
                        i.getQuantity(), i.isSaleable(), i.getRefundAmount(), i.getRevenueReversed(), i.getUnitCost(),
                        i.getCostReversed())).toList());
    }
}
