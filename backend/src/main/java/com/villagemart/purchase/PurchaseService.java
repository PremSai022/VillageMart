package com.villagemart.purchase;

import com.villagemart.common.ApiException;
import com.villagemart.common.BusinessClock;
import com.villagemart.common.DocumentNumberService;
import com.villagemart.common.IdempotencyService;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.inventory.InventoryService;
import com.villagemart.inventory.MovementType;
import com.villagemart.inventory.StockChange;
import com.villagemart.product.Product;
import com.villagemart.product.ProductRepository;
import com.villagemart.purchase.PurchaseDtos.CancelRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseItemDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseItemRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnItemDto;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnItemRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseReturnRequest;
import com.villagemart.purchase.PurchaseDtos.PurchaseSummaryDto;
import com.villagemart.security.CurrentUser;
import com.villagemart.supplier.Supplier;
import com.villagemart.supplier.SupplierService;
import com.villagemart.unit.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wholesale purchases. Only COMPLETED purchases affect inventory. Transport and other charges are allocated to lines
 * in proportion to their net line totals, producing a landed unit cost that feeds the weighted average cost.
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    public static final String REF_PURCHASE = "PURCHASE";
    public static final String REF_PURCHASE_RETURN = "PURCHASE_RETURN";
    private static final String IDEMPOTENCY_OPERATION = "PURCHASE_CREATE";
    private static final Set<String> SORTABLE = Set.of("purchaseDate", "createdAt", "totalAmount", "purchaseNumber");

    private final PurchaseRepository repository;
    private final PurchaseReturnRepository returnRepository;
    private final SupplierService supplierService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final IdempotencyService idempotencyService;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryDto> search(Long supplierId, PurchaseStatus status, LocalDate from, LocalDate to,
                                                   String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(repository.search(supplierId, status, from, to, like,
                Pages.of(page, size, null, SORTABLE, Sort.by(Sort.Direction.DESC, "purchaseDate", "id"))), PurchaseService::toSummary);
    }

    @Transactional(readOnly = true)
    public PurchaseDto get(Long id) {
        Purchase p = repository.findDetailed(id).orElseThrow(() -> ApiException.notFound("Purchase", id));
        return toDto(p);
    }

    @Transactional
    public PurchaseDto create(PurchaseRequest r, String idempotencyKey) {
        String hash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.validateKey(idempotencyKey);
            hash = idempotencyService.hash(r);
            var existing = idempotencyService.findExisting(IDEMPOTENCY_OPERATION, idempotencyKey, hash);
            if (existing.isPresent()) {
                return get(existing.get());
            }
        }
        Purchase p = new Purchase();
        p.setStatus(PurchaseStatus.DRAFT);
        p.setPurchaseNumber(documentNumberService.next(DocumentNumberService.PURCHASE));
        populate(p, r);
        repository.saveAndFlush(p);
        if (r.complete()) {
            completeLocked(p);
        }
        if (hash != null) {
            idempotencyService.record(IDEMPOTENCY_OPERATION, idempotencyKey, hash, p.getId());
        }
        return toDto(p);
    }

    @Transactional
    public PurchaseDto updateDraft(Long id, PurchaseRequest r) {
        Purchase p = lock(id);
        if (p.getStatus() != PurchaseStatus.DRAFT) {
            throw ApiException.business("PURCHASE_NOT_EDITABLE", "Only draft purchases can be edited");
        }
        p.getItems().clear();
        repository.flush();
        populate(p, r);
        repository.flush();
        if (r.complete()) {
            completeLocked(p);
        }
        return toDto(p);
    }

    @Transactional
    public PurchaseDto complete(Long id) {
        Purchase p = lock(id);
        if (p.getStatus() != PurchaseStatus.DRAFT) {
            throw ApiException.business("INVALID_PURCHASE_STATUS", "Only draft purchases can be completed (current: " + p.getStatus() + ")");
        }
        completeLocked(p);
        return toDto(p);
    }

    @Transactional
    public PurchaseDto cancel(Long id, CancelRequest r) {
        Purchase p = lock(id);
        switch (p.getStatus()) {
            case CANCELLED -> throw ApiException.business("ALREADY_CANCELLED", "Purchase is already cancelled");
            case DRAFT -> { }
            case COMPLETED -> {
                List<StockChange> changes = new ArrayList<>();
                for (PurchaseItem item : p.getItems()) {
                    BigDecimal remaining = item.returnableQuantity();
                    if (remaining.signum() > 0) {
                        changes.add(StockChange.purchaseReversal(item.getProduct().getId(), MovementType.CANCELLATION_REVERSAL,
                                remaining, item.getLandedUnitCost(), REF_PURCHASE, p.getId(), p.getPurchaseNumber(),
                                "Purchase cancelled: " + r.reason().trim()));
                    }
                }
                inventoryService.applyAll(changes);
            }
        }
        p.setStatus(PurchaseStatus.CANCELLED);
        p.setCancelledAt(Instant.now());
        p.setCancelReason(r.reason().trim());
        return toDto(p);
    }

    @Transactional
    public PurchaseReturnDto createReturn(Long id, PurchaseReturnRequest r) {
        Purchase p = lock(id);
        if (p.getStatus() != PurchaseStatus.COMPLETED) {
            throw ApiException.business("INVALID_PURCHASE_STATUS", "Returns are allowed only for completed purchases");
        }
        Map<Long, PurchaseItem> itemsById = p.getItems().stream().collect(Collectors.toMap(PurchaseItem::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        PurchaseReturn ret = new PurchaseReturn();
        ret.setReturnNumber(documentNumberService.next(DocumentNumberService.PURCHASE_RETURN));
        ret.setPurchase(p);
        ret.setReturnDate(r.returnDate() == null ? clock.today() : r.returnDate());
        validateDate(ret.getReturnDate(), "Return date");
        if (ret.getReturnDate().isBefore(p.getPurchaseDate())) {
            throw ApiException.badRequest("INVALID_DATE", "Return date cannot be before the purchase date");
        }
        ret.setReason(r.reason().trim());
        ret.setSupplierReference(blankToNull(r.supplierReference()));
        ret.setCreatedBy(CurrentUser.idOrNull());

        BigDecimal total = BigDecimal.ZERO;
        List<StockChange> changes = new ArrayList<>();
        for (PurchaseReturnItemRequest req : r.items()) {
            PurchaseItem item = itemsById.get(req.purchaseItemId());
            if (item == null) {
                throw ApiException.badRequest("INVALID_ITEM", "Item " + req.purchaseItemId() + " does not belong to purchase " + p.getPurchaseNumber());
            }
            if (!seen.add(item.getId())) {
                throw ApiException.badRequest("DUPLICATE_ITEM", "Each purchase item can appear only once in a return");
            }
            BigDecimal qty = Money.qty(req.quantity());
            UnitService.validateQuantity(item.getProduct().getUnit(), qty, item.getProductName());
            if (qty.compareTo(item.returnableQuantity()) > 0) {
                throw ApiException.business("RETURN_EXCEEDS_QUANTITY", "Cannot return %s of %s; only %s remaining returnable"
                        .formatted(plain(qty), item.getProductName(), plain(item.returnableQuantity())));
            }
            BigDecimal amount = Money.money(qty.multiply(item.getLandedUnitCost()));
            item.setReturnedQuantity(Money.qty(item.getReturnedQuantity().add(qty)));
            PurchaseReturnItem ri = new PurchaseReturnItem();
            ri.setPurchaseReturn(ret);
            ri.setPurchaseItem(item);
            ri.setProduct(item.getProduct());
            ri.setQuantity(qty);
            ri.setUnitCost(item.getLandedUnitCost());
            ri.setAmount(amount);
            ret.getItems().add(ri);
            total = total.add(amount);
        }
        ret.setTotalAmount(Money.money(total));
        returnRepository.saveAndFlush(ret);
        for (PurchaseReturnItem ri : ret.getItems()) {
            changes.add(StockChange.purchaseReversal(ri.getProduct().getId(), MovementType.PURCHASE_RETURN, ri.getQuantity(),
                    ri.getUnitCost(), REF_PURCHASE_RETURN, ret.getId(), ret.getReturnNumber(), "Returned to supplier: " + ret.getReason()));
        }
        inventoryService.applyAll(changes);
        p.setReturnedAmount(Money.money(p.getReturnedAmount().add(total)));
        return toReturnDto(ret);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturnDto> returns(Long purchaseId) {
        repository.findById(purchaseId).orElseThrow(() -> ApiException.notFound("Purchase", purchaseId));
        return returnRepository.findByPurchaseIdOrderByIdAsc(purchaseId).stream().map(PurchaseService::toReturnDto).toList();
    }

    private Purchase lock(Long id) {
        return repository.lockById(id).orElseThrow(() -> ApiException.notFound("Purchase", id));
    }

    private void completeLocked(Purchase p) {
        List<StockChange> changes = new ArrayList<>();
        for (PurchaseItem item : p.getItems()) {
            changes.add(StockChange.inbound(item.getProduct().getId(), MovementType.PURCHASE, item.getQuantity(),
                    item.getLandedUnitCost(), REF_PURCHASE, p.getId(), p.getPurchaseNumber(),
                    "Purchase from " + p.getSupplier().getName()));
        }
        inventoryService.applyAll(changes);
        for (PurchaseItem item : p.getItems()) {
            item.getProduct().setPurchasePrice(Money.money(item.getLandedUnitCost()));
        }
        p.setStatus(PurchaseStatus.COMPLETED);
        p.setCompletedAt(Instant.now());
    }

    private void populate(Purchase p, PurchaseRequest r) {
        Supplier supplier = supplierService.get(r.supplierId());
        if (!supplier.isActive()) {
            throw ApiException.business("SUPPLIER_INACTIVE", "Supplier " + supplier.getName() + " is inactive");
        }
        String invoice = blankToNull(r.supplierInvoiceNumber());
        if (invoice != null && repository.existsActiveInvoice(supplier.getId(), invoice, p.getId())) {
            throw ApiException.conflict("DUPLICATE_SUPPLIER_INVOICE",
                    "Invoice " + invoice + " from " + supplier.getName() + " has already been recorded");
        }
        LocalDate date = r.purchaseDate() == null ? clock.today() : r.purchaseDate();
        validateDate(date, "Purchase date");

        Set<Long> productIds = r.items().stream().map(PurchaseItemRequest::productId).collect(Collectors.toSet());
        if (productIds.size() != r.items().size()) {
            throw ApiException.badRequest("DUPLICATE_ITEM", "Each product can appear only once in a purchase; combine the quantities");
        }
        Map<Long, Product> products = productRepository.findAllDetailed(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        List<PurchaseItem> items = new ArrayList<>();
        for (PurchaseItemRequest req : r.items()) {
            Product product = products.get(req.productId());
            if (product == null) {
                throw ApiException.notFound("Product", req.productId());
            }
            BigDecimal qty = Money.qty(req.quantity());
            UnitService.validateQuantity(product.getUnit(), qty, product.getName());
            BigDecimal price = Money.money(req.unitPrice());
            BigDecimal gross = Money.money(qty.multiply(price));
            BigDecimal discount = Money.money(req.discountAmount());
            if (discount.compareTo(gross) > 0) {
                throw ApiException.business("INVALID_DISCOUNT", "Discount for " + product.getName() + " exceeds the line amount");
            }
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(p);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitCode(product.getUnit().getCode());
            item.setQuantity(qty);
            item.setUnitPrice(price);
            item.setDiscountAmount(discount);
            item.setLineTotal(gross.subtract(discount));
            item.setReturnedQuantity(Money.qty(BigDecimal.ZERO));
            items.add(item);
            subtotal = subtotal.add(gross);
            discountTotal = discountTotal.add(discount);
            netTotal = netTotal.add(item.getLineTotal());
        }
        BigDecimal transport = Money.money(r.transportCharges());
        BigDecimal other = Money.money(r.otherCharges());
        allocateCharges(items, transport.add(other), netTotal);

        p.setSupplier(supplier);
        p.setSupplierInvoiceNumber(invoice);
        p.setPurchaseDate(date);
        p.setNotes(blankToNull(r.notes()));
        p.setSubtotal(Money.money(subtotal));
        p.setDiscountTotal(Money.money(discountTotal));
        p.setTransportCharges(transport);
        p.setOtherCharges(other);
        p.setTotalAmount(Money.money(netTotal.add(transport).add(other)));
        p.setReturnedAmount(Money.money(p.getReturnedAmount()));
        p.getItems().addAll(items);
    }

    /** Distributes charges by net line value (by quantity if all lines are free); the last line absorbs rounding. */
    static void allocateCharges(List<PurchaseItem> items, BigDecimal charges, BigDecimal netTotal) {
        BigDecimal totalQty = items.stream().map(PurchaseItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean byValue = netTotal.signum() > 0;
        BigDecimal remaining = charges;
        for (int i = 0; i < items.size(); i++) {
            PurchaseItem item = items.get(i);
            BigDecimal share;
            if (i == items.size() - 1) {
                share = remaining;
            } else {
                BigDecimal weight = byValue ? item.getLineTotal().divide(netTotal, 10, RoundingMode.HALF_UP)
                        : item.getQuantity().divide(totalQty, 10, RoundingMode.HALF_UP);
                share = Money.money(charges.multiply(weight));
                remaining = remaining.subtract(share);
            }
            item.setAllocatedCharges(Money.money(share));
            item.setLandedUnitCost(item.getLineTotal().add(item.getAllocatedCharges())
                    .divide(item.getQuantity(), Money.COST_SCALE, RoundingMode.HALF_UP));
        }
    }

    private void validateDate(LocalDate date, String label) {
        if (date.isAfter(clock.today())) {
            throw ApiException.badRequest("INVALID_DATE", label + " cannot be in the future");
        }
    }

    static PurchaseSummaryDto toSummary(Purchase p) {
        return new PurchaseSummaryDto(p.getId(), p.getPurchaseNumber(), p.getSupplier().getId(), p.getSupplier().getName(),
                p.getSupplierInvoiceNumber(), p.getPurchaseDate(), p.getStatus(), p.getTotalAmount(), p.getReturnedAmount(),
                p.getCreatedAt());
    }

    private PurchaseDto toDto(Purchase p) {
        List<PurchaseItemDto> items = p.getItems().stream().map(i -> new PurchaseItemDto(i.getId(), i.getProduct().getId(),
                i.getProductName(), i.getUnitCode(), i.getQuantity(), i.getUnitPrice(), i.getDiscountAmount(), i.getLineTotal(),
                i.getAllocatedCharges(), i.getLandedUnitCost(), i.getReturnedQuantity(), i.returnableQuantity())).toList();
        List<PurchaseReturnDto> returns = p.getId() == null ? List.of()
                : returnRepository.findByPurchaseIdOrderByIdAsc(p.getId()).stream().map(PurchaseService::toReturnDto).toList();
        return new PurchaseDto(p.getId(), p.getPurchaseNumber(), p.getSupplier().getId(), p.getSupplier().getName(),
                p.getSupplierInvoiceNumber(), p.getPurchaseDate(), p.getStatus(), p.getSubtotal(), p.getDiscountTotal(),
                p.getTransportCharges(), p.getOtherCharges(), p.getTotalAmount(), p.getReturnedAmount(), p.getNotes(),
                p.getCompletedAt(), p.getCancelledAt(), p.getCancelReason(), p.getCreatedAt(), items, returns);
    }

    static PurchaseReturnDto toReturnDto(PurchaseReturn r) {
        return new PurchaseReturnDto(r.getId(), r.getReturnNumber(), r.getPurchase().getId(), r.getReturnDate(), r.getReason(),
                r.getTotalAmount(), r.getSupplierReference(), r.getCreatedAt(),
                r.getItems().stream().map(i -> new PurchaseReturnItemDto(i.getId(), i.getPurchaseItem().getId(),
                        i.getProduct().getId(), i.getPurchaseItem().getProductName(), i.getQuantity(), i.getUnitCost(),
                        i.getAmount())).toList());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
