package com.villagemart.sales;

import com.villagemart.common.ApiException;
import com.villagemart.common.BusinessClock;
import com.villagemart.common.DocumentNumberService;
import com.villagemart.common.IdempotencyService;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.customer.Customer;
import com.villagemart.customer.CustomerService;
import com.villagemart.inventory.AppliedMovement;
import com.villagemart.inventory.InventoryService;
import com.villagemart.inventory.MovementType;
import com.villagemart.inventory.StockChange;
import com.villagemart.payment.Payment;
import com.villagemart.payment.PaymentDirection;
import com.villagemart.payment.PaymentDtos.PaymentLine;
import com.villagemart.payment.PaymentMethod;
import com.villagemart.payment.PaymentService;
import com.villagemart.payment.PaymentType;
import com.villagemart.product.Product;
import com.villagemart.product.ProductRepository;
import com.villagemart.sales.SaleDtos.CancelSaleRequest;
import com.villagemart.sales.SaleDtos.SaleDto;
import com.villagemart.sales.SaleDtos.SaleItemDto;
import com.villagemart.sales.SaleDtos.SaleItemRequest;
import com.villagemart.sales.SaleDtos.SaleRequest;
import com.villagemart.sales.SaleDtos.SaleSummaryDto;
import com.villagemart.sales.SaleDtos.SalesReturnDto;
import com.villagemart.security.CurrentUser;
import com.villagemart.settings.BusinessSettingsService;
import com.villagemart.unit.UnitService;
import com.villagemart.village.Village;
import com.villagemart.village.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Point-of-sale workflow. A sale is created atomically: validate, lock stock, create invoice and items, deduct stock
 * with movements, record payments and compute cost of goods sold from the weighted average cost at sale time.
 */
@Service
@RequiredArgsConstructor
public class SaleService {

    public static final String REF_SALE = "SALE";
    static final String IDEMPOTENCY_OPERATION = "SALE_CREATE";
    private static final Set<String> SORTABLE = Set.of("saleDate", "createdAt", "totalAmount", "invoiceNumber");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SaleRepository repository;
    private final SalesReturnRepository returnRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final CustomerService customerService;
    private final VillageService villageService;
    private final BusinessSettingsService settingsService;
    private final DocumentNumberService documentNumberService;
    private final IdempotencyService idempotencyService;
    private final BusinessClock clock;

    @Transactional
    public SaleDto create(SaleRequest r, String idempotencyKey) {
        idempotencyService.validateKey(idempotencyKey);
        String hash = idempotencyService.hash(r);
        var existing = idempotencyService.findExisting(IDEMPOTENCY_OPERATION, idempotencyKey, hash);
        if (existing.isPresent()) {
            return toDto(getDetailed(existing.get()), true);
        }

        LocalDate date = r.saleDate() == null ? clock.today() : r.saleDate();
        if (date.isAfter(clock.today())) {
            throw ApiException.business("FUTURE_DATE", "Sale date cannot be in the future");
        }
        if (r.customerId() != null && r.newCustomer() != null) {
            throw ApiException.badRequest("AMBIGUOUS_CUSTOMER", "Provide either an existing customer or new customer details, not both");
        }
        Customer customer = null;
        if (r.customerId() != null) {
            customer = customerService.get(r.customerId());
            if (!customer.isActive()) {
                throw ApiException.business("INACTIVE_CUSTOMER", "Customer " + customer.getName() + " is inactive");
            }
        } else if (r.newCustomer() != null) {
            customer = customerService.createEntity(r.newCustomer());
        }
        Village village = null;
        if (r.villageId() != null) {
            village = villageService.get(r.villageId());
        } else if (customer != null && customer.getVillage() != null) {
            village = customer.getVillage();
        }

        Sale sale = new Sale();
        sale.setInvoiceNumber(documentNumberService.next(DocumentNumberService.SALE));
        sale.setSaleDate(date);
        sale.setSaleType(customer == null ? SaleType.DIRECT : SaleType.CUSTOMER);
        sale.setCustomer(customer);
        sale.setVillage(village);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentMode(r.paymentMode());
        sale.setNotes(blankToNull(r.notes()));
        sale.setIdempotencyKey(idempotencyKey);
        sale.setCreatedBy(CurrentUser.idOrNull());
        buildItems(sale, r);

        List<PaymentLine> payments = resolvePayments(r, sale.getTotalAmount());
        BigDecimal paid = payments.stream().map(l -> Money.money(l.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = Money.money(sale.getTotalAmount().subtract(paid));
        if (outstanding.signum() > 0 && customer == null) {
            throw ApiException.business("CUSTOMER_REQUIRED_FOR_CREDIT",
                    "Select or add a customer to give credit of ₹" + outstanding.toPlainString());
        }
        sale.setPaidAmount(Money.money(paid));
        sale.setOutstandingAmount(outstanding);
        sale.setCostTotal(Money.money(BigDecimal.ZERO));
        sale.setReturnedAmount(Money.money(BigDecimal.ZERO));
        sale.setReturnedRevenue(Money.money(BigDecimal.ZERO));
        sale.setReturnedCost(Money.money(BigDecimal.ZERO));
        repository.saveAndFlush(sale);

        List<StockChange> changes = sale.getItems().stream()
                .map(i -> StockChange.outbound(i.getProduct().getId(), MovementType.SALE, i.getQuantity(), REF_SALE,
                        sale.getId(), sale.getInvoiceNumber(), "Sale " + sale.getInvoiceNumber()))
                .toList();
        List<AppliedMovement> applied = inventoryService.applyAll(changes);
        BigDecimal costTotal = BigDecimal.ZERO;
        for (int i = 0; i < applied.size(); i++) {
            SaleItem item = sale.getItems().get(i);
            BigDecimal unitCost = Money.cost(applied.get(i).unitCost());
            item.setUnitCost(unitCost);
            item.setCostTotal(Money.money(unitCost.multiply(item.getQuantity())));
            costTotal = costTotal.add(item.getCostTotal());
        }
        sale.setCostTotal(Money.money(costTotal));

        for (PaymentLine line : payments) {
            paymentService.record(sale, customer, null, PaymentType.SALE_PAYMENT, line.method(), PaymentDirection.IN,
                    line.amount(), date, line.reference(), null);
        }
        idempotencyService.record(IDEMPOTENCY_OPERATION, idempotencyKey, hash, sale.getId());
        return toDto(sale, false);
    }

    @Transactional
    public SaleDto cancel(Long id, CancelSaleRequest r) {
        Sale sale = repository.lockById(id).orElseThrow(() -> ApiException.notFound("Sale", id));
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw ApiException.conflict("ALREADY_CANCELLED", "Sale " + sale.getInvoiceNumber() + " is already cancelled");
        }
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw ApiException.business("SALE_HAS_RETURNS",
                    "Sale " + sale.getInvoiceNumber() + " has returns; record further returns instead of cancelling");
        }
        String reason = "Cancelled " + sale.getInvoiceNumber() + ": " + r.reason().trim();
        List<StockChange> changes = sale.getItems().stream()
                .map(i -> StockChange.inbound(i.getProduct().getId(), MovementType.CANCELLATION_REVERSAL, i.getQuantity(),
                        i.getUnitCost(), REF_SALE, sale.getId(), sale.getInvoiceNumber(), reason))
                .toList();
        inventoryService.applyAll(changes);

        LocalDate today = clock.today();
        for (Payment p : paymentService.forSale(sale.getId())) {
            if (p.getDirection() == PaymentDirection.IN && !p.isReversed()) {
                p.setReversed(true);
                paymentService.record(sale, sale.getCustomer(), null, PaymentType.CANCELLATION_REFUND, p.getMethod(),
                        PaymentDirection.OUT, p.getAmount(), today, p.getPaymentNumber(), reason);
            }
        }
        sale.setOutstandingAmount(Money.money(BigDecimal.ZERO));
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(Instant.now());
        sale.setCancelReason(r.reason().trim());
        return toDto(sale, false);
    }

    @Transactional(readOnly = true)
    public SaleDto get(Long id) {
        return toDto(getDetailed(id), false);
    }

    @Transactional(readOnly = true)
    public Sale getDetailed(Long id) {
        return repository.findDetailed(id).orElseThrow(() -> ApiException.notFound("Sale", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryDto> search(Long customerId, Long villageId, SaleStatus status, PaymentMode mode,
                                               LocalDate from, LocalDate to, String q, int page, int size) {
        return search(customerId, villageId, status, mode, from, to, q, page, size, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryDto> search(Long customerId, Long villageId, SaleStatus status, PaymentMode mode,
                                               LocalDate from, LocalDate to, String q, int page, int size, String sort) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(repository.search(customerId, villageId, status, mode, from, to, like,
                        Pages.of(page, size, sort, SORTABLE, Sort.by(Sort.Direction.DESC, "saleDate", "id"))),
                SaleService::toSummary);
    }

    private void buildItems(Sale sale, SaleRequest r) {
        Set<Long> ids = r.items().stream().map(SaleItemRequest::productId).collect(Collectors.toSet());
        if (ids.size() != r.items().size()) {
            throw ApiException.badRequest("DUPLICATE_ITEM", "Each product can appear only once in a bill; combine the quantities");
        }
        Map<Long, Product> products = productRepository.findAllDetailed(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        boolean gst = settingsService.getEntity().isGstEnabled();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal itemDiscounts = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        for (SaleItemRequest req : r.items()) {
            Product product = products.get(req.productId());
            if (product == null) {
                throw ApiException.notFound("Product", req.productId());
            }
            if (!product.isActive()) {
                throw ApiException.business("INACTIVE_PRODUCT", product.getName() + " is inactive and cannot be sold");
            }
            BigDecimal qty = Money.qty(req.quantity());
            UnitService.validateQuantity(product.getUnit(), qty, product.getName());
            BigDecimal listPrice = Money.money(product.getSellingPrice());
            BigDecimal price = req.unitPrice() == null ? listPrice : Money.money(req.unitPrice());
            boolean overridden = price.compareTo(listPrice) != 0;
            if (overridden && !r.priceOverrideConfirmed()) {
                throw ApiException.business("PRICE_OVERRIDE_NOT_CONFIRMED",
                        "Price of " + product.getName() + " was changed from ₹" + listPrice.toPlainString() + "; confirm the new price");
            }
            if (product.getMinSellingPrice() != null && price.compareTo(product.getMinSellingPrice()) < 0) {
                throw ApiException.business("BELOW_MIN_PRICE", "Price of " + product.getName()
                        + " cannot be below the minimum selling price ₹" + Money.money(product.getMinSellingPrice()).toPlainString());
            }
            BigDecimal gross = Money.money(qty.multiply(price));
            BigDecimal discount = Money.money(Money.nz(req.discountAmount()));
            if (discount.compareTo(gross) > 0) {
                throw ApiException.business("INVALID_DISCOUNT", "Discount for " + product.getName() + " exceeds the line amount");
            }
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
            item.setUnitCode(product.getUnit().getCode());
            item.setQuantity(qty);
            item.setListPrice(listPrice);
            item.setUnitPrice(price);
            item.setPriceOverridden(overridden);
            item.setDiscountAmount(discount);
            item.setTaxableAmount(gross.subtract(discount));
            item.setTaxRate(gst ? Money.money(Money.nz(product.getTaxRate())) : Money.money(BigDecimal.ZERO));
            item.setUnitCost(Money.cost(BigDecimal.ZERO));
            item.setCostTotal(Money.money(BigDecimal.ZERO));
            item.setReturnedQuantity(Money.qty(BigDecimal.ZERO));
            sale.getItems().add(item);
            subtotal = subtotal.add(gross);
            itemDiscounts = itemDiscounts.add(discount);
            netTotal = netTotal.add(item.getTaxableAmount());
        }

        BigDecimal billDiscount = Money.money(Money.nz(r.billDiscount()));
        if (billDiscount.compareTo(netTotal) > 0) {
            throw ApiException.business("INVALID_DISCOUNT", "Bill discount exceeds the bill amount");
        }
        allocateBillDiscount(sale.getItems(), billDiscount, netTotal);

        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (SaleItem item : sale.getItems()) {
            BigDecimal lineTaxable = Money.money(item.getTaxableAmount().subtract(item.getBillDiscountShare()));
            BigDecimal lineTax = Money.money(lineTaxable.multiply(item.getTaxRate()).divide(HUNDRED, 6, RoundingMode.HALF_UP));
            item.setTaxableAmount(lineTaxable);
            item.setTaxAmount(lineTax);
            item.setLineTotal(Money.money(lineTaxable.add(lineTax)));
            taxable = taxable.add(lineTaxable);
            tax = tax.add(lineTax);
        }
        BigDecimal raw = Money.money(taxable.add(tax));
        BigDecimal total = r.roundOff() ? raw.setScale(0, RoundingMode.HALF_UP).setScale(2) : raw;
        sale.setSubtotal(Money.money(subtotal));
        sale.setItemDiscountTotal(Money.money(itemDiscounts));
        sale.setBillDiscount(billDiscount);
        sale.setTaxableAmount(Money.money(taxable));
        sale.setTaxAmount(Money.money(tax));
        sale.setRoundOff(Money.money(total.subtract(raw)));
        sale.setTotalAmount(total);
    }

    /** Spreads the bill discount over lines in proportion to their net amount; the last line absorbs rounding. */
    private static void allocateBillDiscount(List<SaleItem> items, BigDecimal discount, BigDecimal netTotal) {
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            BigDecimal share;
            if (discount.signum() == 0 || netTotal.signum() == 0) {
                share = BigDecimal.ZERO;
            } else if (i == items.size() - 1) {
                share = discount.subtract(allocated);
            } else {
                share = Money.money(discount.multiply(item.getTaxableAmount()).divide(netTotal, 6, RoundingMode.HALF_UP));
            }
            item.setBillDiscountShare(Money.money(share));
            allocated = allocated.add(item.getBillDiscountShare());
        }
    }

    private static List<PaymentLine> resolvePayments(SaleRequest r, BigDecimal total) {
        List<PaymentLine> lines = r.payments() == null ? List.of() : r.payments();
        BigDecimal sum = lines.stream().map(l -> Money.money(l.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        switch (r.paymentMode()) {
            case CASH, UPI -> {
                PaymentMethod method = PaymentMethod.valueOf(r.paymentMode().name());
                if (lines.isEmpty()) {
                    return total.signum() == 0 ? List.of() : List.of(new PaymentLine(method, total, null));
                }
                if (lines.stream().anyMatch(l -> l.method() != method)) {
                    throw ApiException.badRequest("PAYMENT_METHOD_MISMATCH", "All payments must be " + method + " for a " + method + " sale; use SPLIT_PAYMENT to mix methods");
                }
                if (sum.compareTo(total) != 0) {
                    throw ApiException.business("PAYMENT_TOTAL_MISMATCH", "Payment ₹" + sum.toPlainString()
                            + " must equal the bill total ₹" + total.toPlainString() + "; use CREDIT or SPLIT_PAYMENT for partial payment");
                }
                return lines;
            }
            case SPLIT_PAYMENT -> {
                if (lines.size() < 2 && sum.compareTo(total) == 0) {
                    throw ApiException.badRequest("SPLIT_REQUIRES_MULTIPLE", "Split payment needs at least two payment entries");
                }
                if (lines.isEmpty()) {
                    throw ApiException.badRequest("PAYMENTS_REQUIRED", "Enter the payment amounts for a split payment");
                }
            }
            case CREDIT -> {
                // Optional advance payment; remainder becomes customer credit.
            }
        }
        if (sum.compareTo(total) > 0) {
            throw ApiException.business("OVERPAYMENT", "Payments ₹" + sum.toPlainString() + " exceed the bill total ₹" + total.toPlainString());
        }
        return lines;
    }

    SaleDto toDto(Sale s, boolean replayed) {
        List<SalesReturnDto> returns = returnRepository.findBySaleIdOrderByIdAsc(s.getId()).stream()
                .map(SalesReturnService::toDto).toList();
        return new SaleDto(s.getId(), s.getInvoiceNumber(), s.getSaleDate(), s.getSaleType(),
                s.getCustomer() == null ? null : s.getCustomer().getId(),
                s.getCustomer() == null ? null : s.getCustomer().getName(),
                s.getCustomer() == null ? null : s.getCustomer().getMobile(),
                s.getVillage() == null ? null : s.getVillage().getId(),
                s.getVillage() == null ? null : s.getVillage().getName(),
                s.getStatus(), s.getPaymentMode(), s.getSubtotal(), s.getItemDiscountTotal(), s.getBillDiscount(),
                s.getTaxableAmount(), s.getTaxAmount(), s.getRoundOff(), s.getTotalAmount(), s.getPaidAmount(),
                s.getOutstandingAmount(), s.getReturnedAmount(), s.getCostTotal(), grossProfit(s), s.getNotes(),
                s.getCancelledAt(), s.getCancelReason(), s.getCreatedAt(),
                s.getItems().stream().map(SaleService::toItemDto).toList(),
                paymentService.forSale(s.getId()).stream().map(PaymentService::toDto).toList(),
                returns, replayed);
    }

    public static BigDecimal grossProfit(Sale s) {
        if (s.getStatus() == SaleStatus.CANCELLED) {
            return Money.money(BigDecimal.ZERO);
        }
        BigDecimal revenue = s.getTaxableAmount().subtract(s.getReturnedRevenue());
        BigDecimal cost = s.getCostTotal().subtract(s.getReturnedCost());
        return Money.money(revenue.subtract(cost));
    }

    static SaleItemDto toItemDto(SaleItem i) {
        return new SaleItemDto(i.getId(), i.getProduct().getId(), i.getProductName(), i.getSku(), i.getCategoryName(),
                i.getUnitCode(), i.getQuantity(), i.getListPrice(), i.getUnitPrice(), i.isPriceOverridden(),
                i.getDiscountAmount(), i.getBillDiscountShare(), i.getTaxableAmount(), i.getTaxRate(), i.getTaxAmount(),
                i.getLineTotal(), i.getUnitCost(), i.getCostTotal(), i.getReturnedQuantity(), i.returnableQuantity());
    }

    public static SaleSummaryDto toSummary(Sale s) {
        return new SaleSummaryDto(s.getId(), s.getInvoiceNumber(), s.getSaleDate(), s.getSaleType(),
                s.getCustomer() == null ? null : s.getCustomer().getId(),
                s.getCustomer() == null ? null : s.getCustomer().getName(),
                s.getVillage() == null ? null : s.getVillage().getId(),
                s.getVillage() == null ? null : s.getVillage().getName(),
                s.getStatus(), s.getPaymentMode(), s.getTotalAmount(), s.getPaidAmount(), s.getOutstandingAmount(),
                s.getReturnedAmount(), s.getCreatedAt());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
