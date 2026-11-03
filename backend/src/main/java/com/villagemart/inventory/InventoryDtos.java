package com.villagemart.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record InventoryItemDto(
            Long productId, String productName, String sku, Long categoryId, String categoryName,
            String unitCode, boolean active, BigDecimal availableQuantity, BigDecimal minStockQuantity,
            BigDecimal averageCost, BigDecimal stockValue, BigDecimal sellingPrice, StockStatus stockStatus,
            BigDecimal suggestedReorderQuantity, Instant lastMovementAt) {
    }

    public record StockMovementDto(
            Long id, Long productId, String productName, MovementType movementType,
            BigDecimal quantityChange, BigDecimal previousQuantity, BigDecimal newQuantity,
            BigDecimal unitCost, BigDecimal averageCostAfter,
            String referenceType, Long referenceId, String referenceNumber, String reason,
            Long createdBy, Instant createdAt) {
    }

    public enum AdjustmentDirection { INCREASE, DECREASE }

    public record StockAdjustmentRequest(
            @NotNull(message = "Product is required") Long productId,
            @NotNull(message = "Adjustment type is required") MovementType type,
            AdjustmentDirection direction,
            @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
            @Digits(integer = 11, fraction = 3) BigDecimal quantity,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 4) BigDecimal unitCost,
            @NotBlank(message = "Reason is mandatory for stock adjustments") @Size(max = 300) String reason) {
    }

    public record InventorySummaryDto(
            long totalProducts, BigDecimal totalQuantity, BigDecimal totalValue,
            long lowStockCount, long outOfStockCount) {
    }
}
