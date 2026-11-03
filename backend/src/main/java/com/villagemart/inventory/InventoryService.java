package com.villagemart.inventory;

import com.villagemart.common.ApiException;
import com.villagemart.common.Money;
import com.villagemart.inventory.InventoryDtos.AdjustmentDirection;
import com.villagemart.inventory.InventoryDtos.StockAdjustmentRequest;
import com.villagemart.notification.StockAlertService;
import com.villagemart.product.Product;
import com.villagemart.security.CurrentUser;
import com.villagemart.unit.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The single entry point for every stock mutation. Rows are locked with SELECT ... FOR UPDATE in product-id order
 * to avoid deadlocks; every change writes an immutable {@link StockMovement}; negative stock is never allowed.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final String REF_ADJUSTMENT = "ADJUSTMENT";

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final StockLocationRepository locationRepository;
    private final StockAlertService stockAlertService;

    private volatile Long defaultLocationId;

    public Long defaultLocationId() {
        Long id = defaultLocationId;
        if (id == null) {
            id = locationRepository.findFirstByDefaultLocationTrue()
                    .orElseThrow(() -> new IllegalStateException("No default stock location configured"))
                    .getId();
            defaultLocationId = id;
        }
        return id;
    }

    @Transactional
    public Inventory initialize(Product product) {
        Inventory inv = new Inventory();
        inv.setProduct(product);
        inv.setLocation(locationRepository.getReferenceById(defaultLocationId()));
        inv.setQuantity(Money.qty(BigDecimal.ZERO));
        inv.setAverageCost(Money.cost(product.getPurchasePrice()));
        inv.setStockStatus(StockStatus.of(BigDecimal.ZERO, product.getMinStockQuantity()));
        return inventoryRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public Inventory findByProduct(Long productId) {
        return inventoryRepository.findByProductIdAndLocationId(productId, defaultLocationId())
                .orElseThrow(() -> ApiException.notFound("Inventory for product", productId));
    }

    @Transactional(readOnly = true)
    public Map<Long, Inventory> findByProducts(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return inventoryRepository.findByProductIdInAndLocationId(productIds, defaultLocationId()).stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), Function.identity()));
    }

    /** Locks inventory rows (FOR UPDATE) for the given products, ordered by product id. */
    @Transactional
    public Map<Long, Inventory> lock(Collection<Long> productIds) {
        List<Long> ordered = new ArrayList<>(new TreeSet<>(productIds));
        Map<Long, Inventory> locked = new LinkedHashMap<>();
        for (Inventory inv : inventoryRepository.lockForUpdate(ordered, defaultLocationId())) {
            locked.put(inv.getProduct().getId(), inv);
        }
        for (Long id : ordered) {
            if (!locked.containsKey(id)) {
                throw ApiException.notFound("Inventory for product", id);
            }
        }
        return locked;
    }

    @Transactional
    public AppliedMovement apply(StockChange change) {
        return applyAll(List.of(change)).get(0);
    }

    /**
     * Applies all changes atomically. Results are returned in input order.
     */
    @Transactional
    public List<AppliedMovement> applyAll(List<StockChange> changes) {
        if (changes.isEmpty()) {
            return List.of();
        }
        Map<Long, Inventory> locked = lock(changes.stream().map(StockChange::productId).toList());
        Map<Long, StockStatus> before = new HashMap<>();
        locked.forEach((id, inv) -> before.put(id, inv.getStockStatus()));

        Instant now = Instant.now();
        Long userId = CurrentUser.idOrNull();
        List<AppliedMovement> results = new ArrayList<>(changes.size());
        for (StockChange change : changes) {
            Inventory inv = locked.get(change.productId());
            results.add(applyLocked(inv, change, now, userId));
        }
        for (Inventory inv : locked.values()) {
            Product product = inv.getProduct();
            StockStatus newStatus = StockStatus.of(inv.getQuantity(), product.getMinStockQuantity());
            inv.setStockStatus(newStatus);
            StockStatus old = before.get(product.getId());
            if (old != newStatus) {
                stockAlertService.onStatusTransition(product, old, newStatus, inv.getQuantity());
            }
        }
        inventoryRepository.flush();
        return results;
    }

    private AppliedMovement applyLocked(Inventory inv, StockChange change, Instant now, Long userId) {
        Product product = inv.getProduct();
        BigDecimal qty = change.quantity();
        if (qty == null || qty.signum() <= 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "Quantity must be greater than zero for " + product.getName());
        }
        qty = Money.qty(qty);
        UnitService.validateQuantity(product.getUnit(), qty, product.getName());

        BigDecimal prevQty = inv.getQuantity();
        BigDecimal prevAvg = inv.getAverageCost();
        BigDecimal newQty;
        BigDecimal newAvg = prevAvg;
        BigDecimal movementCost;

        if (change.inbound()) {
            BigDecimal cost = change.unitCost() != null ? change.unitCost()
                    : (prevAvg.signum() > 0 ? prevAvg : product.getPurchasePrice());
            cost = Money.cost(cost);
            if (cost.signum() < 0) {
                throw ApiException.badRequest("INVALID_COST", "Unit cost cannot be negative");
            }
            newQty = prevQty.add(qty);
            newAvg = prevQty.signum() <= 0 ? cost
                    : prevQty.multiply(prevAvg).add(qty.multiply(cost)).divide(newQty, Money.COST_SCALE, RoundingMode.HALF_UP);
            movementCost = cost;
        } else {
            if (prevQty.compareTo(qty) < 0) {
                throw ApiException.business("INSUFFICIENT_STOCK", "Insufficient stock for %s: available %s %s, requested %s"
                        .formatted(product.getName(), prevQty.stripTrailingZeros().toPlainString(),
                                product.getUnit().getCode(), qty.stripTrailingZeros().toPlainString()));
            }
            newQty = prevQty.subtract(qty);
            if (change.reverseCost()) {
                BigDecimal cost = Money.cost(change.unitCost() != null ? change.unitCost() : prevAvg);
                movementCost = cost;
                if (newQty.signum() > 0) {
                    BigDecimal remainingValue = prevQty.multiply(prevAvg).subtract(qty.multiply(cost));
                    BigDecimal candidate = remainingValue.divide(newQty, Money.COST_SCALE, RoundingMode.HALF_UP);
                    if (candidate.signum() > 0) {
                        newAvg = candidate;
                    }
                }
            } else {
                movementCost = prevAvg;
            }
        }

        inv.setQuantity(Money.qty(newQty));
        inv.setAverageCost(Money.cost(newAvg));
        inv.setLastMovementAt(now);

        StockMovement m = new StockMovement();
        m.setProduct(product);
        m.setLocation(inv.getLocation());
        m.setMovementType(change.type());
        m.setQuantityChange(change.inbound() ? qty : qty.negate());
        m.setPreviousQuantity(prevQty);
        m.setNewQuantity(inv.getQuantity());
        m.setUnitCost(movementCost);
        m.setAverageCostAfter(inv.getAverageCost());
        m.setReferenceType(change.referenceType());
        m.setReferenceId(change.referenceId());
        m.setReferenceNumber(change.referenceNumber());
        m.setReason(change.reason());
        m.setCreatedBy(userId);
        m.setCreatedAt(now);
        movementRepository.save(m);

        return new AppliedMovement(m.getId(), product.getId(), prevQty, inv.getQuantity(), movementCost, inv.getAverageCost());
    }

    /** Re-evaluates stock status after a product's minimum stock changes. */
    @Transactional
    public void refreshStatus(Long productId) {
        Inventory inv = lock(List.of(productId)).get(productId);
        Product product = inv.getProduct();
        StockStatus old = inv.getStockStatus();
        StockStatus now = StockStatus.of(inv.getQuantity(), product.getMinStockQuantity());
        if (old != now) {
            inv.setStockStatus(now);
            stockAlertService.onStatusTransition(product, old, now, inv.getQuantity());
        }
    }

    @Transactional
    public void deleteForUnusedProduct(Long productId) {
        if (movementRepository.existsByProductId(productId)) {
            throw ApiException.conflict("PRODUCT_IN_USE", "Product has stock history and cannot be deleted");
        }
        inventoryRepository.findByProductIdAndLocationId(productId, defaultLocationId())
                .ifPresent(inventoryRepository::delete);
    }

    @Transactional
    public AppliedMovement adjust(StockAdjustmentRequest r) {
        String reason = r.reason().trim();
        return switch (r.type()) {
            case OPENING_STOCK -> {
                if (movementRepository.existsByProductId(r.productId())) {
                    throw ApiException.business("OPENING_STOCK_NOT_ALLOWED",
                            "Opening stock can only be recorded before any other stock movement. Use a manual adjustment instead.");
                }
                yield apply(StockChange.inbound(r.productId(), MovementType.OPENING_STOCK, r.quantity(), r.unitCost(),
                        REF_ADJUSTMENT, null, null, reason));
            }
            case DAMAGE -> apply(StockChange.outbound(r.productId(), MovementType.DAMAGE, r.quantity(),
                    REF_ADJUSTMENT, null, null, reason));
            case MANUAL_ADJUSTMENT -> {
                if (r.direction() == null) {
                    throw ApiException.badRequest("DIRECTION_REQUIRED", "Direction (INCREASE or DECREASE) is required for manual adjustments");
                }
                yield r.direction() == AdjustmentDirection.INCREASE
                        ? apply(StockChange.inbound(r.productId(), MovementType.MANUAL_ADJUSTMENT, r.quantity(), r.unitCost(),
                        REF_ADJUSTMENT, null, null, reason))
                        : apply(StockChange.outbound(r.productId(), MovementType.MANUAL_ADJUSTMENT, r.quantity(),
                        REF_ADJUSTMENT, null, null, reason));
            }
            default -> throw ApiException.badRequest("INVALID_ADJUSTMENT_TYPE",
                    "Adjustment type must be OPENING_STOCK, DAMAGE or MANUAL_ADJUSTMENT. Other movements are created by transactions.");
        };
    }
}
