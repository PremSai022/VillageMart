package com.villagemart.inventory;

import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.inventory.InventoryDtos.InventorySummaryDto;
import com.villagemart.inventory.InventoryDtos.StockMovementDto;
import com.villagemart.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private static final Set<String> SORTABLE = Set.of("product.name", "quantity", "stockStatus", "lastMovementAt");
    private static final Set<String> MOVEMENT_SORTABLE = Set.of("createdAt", "id");

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemDto> search(String q, Long categoryId, StockStatus status, boolean includeInactive,
                                                 int page, int size, String sort) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(inventoryRepository.search(inventoryService.defaultLocationId(), status, like, categoryId,
                includeInactive, Pages.of(page, size, sort, SORTABLE, Sort.by("product.name"))), InventoryQueryService::toDto);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> byStatus(StockStatus status) {
        return inventoryRepository.findActiveByStatus(inventoryService.defaultLocationId(), status).stream()
                .map(InventoryQueryService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemDto get(Long productId) {
        return toDto(inventoryService.findByProduct(productId));
    }

    @Transactional(readOnly = true)
    public InventorySummaryDto summary() {
        Long loc = inventoryService.defaultLocationId();
        Object[] t = inventoryRepository.totals(loc).get(0);
        return new InventorySummaryDto(((Number) t[0]).longValue(), Money.qty(toBig(t[1])), Money.money(toBig(t[2])),
                inventoryRepository.countActiveByStatus(loc, StockStatus.LOW_STOCK),
                inventoryRepository.countActiveByStatus(loc, StockStatus.OUT_OF_STOCK));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> movements(Long productId, MovementType type, Instant from, Instant to,
                                                    int page, int size, String sort) {
        return PageResponse.of(movementRepository.search(productId, type, from, to,
                        Pages.of(page, size, sort, MOVEMENT_SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
                InventoryQueryService::toDto);
    }

    public static StockMovementDto toDto(StockMovement m) {
        return new StockMovementDto(m.getId(), m.getProduct().getId(), m.getProduct().getName(), m.getMovementType(),
                m.getQuantityChange(), m.getPreviousQuantity(), m.getNewQuantity(), m.getUnitCost(), m.getAverageCostAfter(),
                m.getReferenceType(), m.getReferenceId(), m.getReferenceNumber(), m.getReason(), m.getCreatedBy(), m.getCreatedAt());
    }

    public static InventoryItemDto toDto(Inventory i) {
        Product p = i.getProduct();
        return new InventoryItemDto(p.getId(), p.getName(), p.getSku(), p.getCategory().getId(), p.getCategory().getName(),
                p.getUnit().getCode(), p.isActive(), i.getQuantity(), p.getMinStockQuantity(), i.getAverageCost(),
                Money.money(i.getQuantity().multiply(i.getAverageCost())), p.getSellingPrice(), i.getStockStatus(),
                suggestedReorder(i.getQuantity(), p.getMinStockQuantity()), i.getLastMovementAt());
    }

    /** Suggests topping up to twice the minimum stock level. */
    public static BigDecimal suggestedReorder(BigDecimal quantity, BigDecimal minimum) {
        if (minimum == null || minimum.signum() <= 0) {
            return Money.qty(BigDecimal.ZERO);
        }
        BigDecimal target = minimum.multiply(BigDecimal.valueOf(2)).subtract(quantity);
        return Money.qty(target.signum() > 0 ? target : BigDecimal.ZERO);
    }

    private static BigDecimal toBig(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal b) {
            return b;
        }
        return new BigDecimal(o.toString());
    }
}
