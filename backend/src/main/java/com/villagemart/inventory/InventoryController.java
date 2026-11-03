package com.villagemart.inventory;

import com.villagemart.common.PageResponse;
import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.inventory.InventoryDtos.InventorySummaryDto;
import com.villagemart.inventory.InventoryDtos.StockAdjustmentRequest;
import com.villagemart.inventory.InventoryDtos.StockMovementDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService queryService;
    private final InventoryService inventoryService;

    @GetMapping
    public PageResponse<InventoryItemDto> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) StockStatus status,
                                               @RequestParam(defaultValue = "false") boolean includeInactive,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String sort) {
        return queryService.search(q, categoryId, status, includeInactive, page, size, sort);
    }

    @GetMapping("/summary")
    public InventorySummaryDto summary() {
        return queryService.summary();
    }

    @GetMapping("/low-stock")
    public List<InventoryItemDto> lowStock() {
        return queryService.byStatus(StockStatus.LOW_STOCK);
    }

    @GetMapping("/out-of-stock")
    public List<InventoryItemDto> outOfStock() {
        return queryService.byStatus(StockStatus.OUT_OF_STOCK);
    }

    @GetMapping("/movements")
    public PageResponse<StockMovementDto> allMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return queryService.movements(productId, type, from, to, page, size, null);
    }

    @GetMapping("/{productId}")
    public InventoryItemDto get(@PathVariable Long productId) {
        return queryService.get(productId);
    }

    @GetMapping("/{productId}/movements")
    public PageResponse<StockMovementDto> movements(@PathVariable Long productId,
                                                    @RequestParam(required = false) MovementType type,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "30") int size) {
        queryService.get(productId);
        return queryService.movements(productId, type, null, null, page, size, null);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        inventoryService.adjust(request);
        return queryService.get(request.productId());
    }
}
