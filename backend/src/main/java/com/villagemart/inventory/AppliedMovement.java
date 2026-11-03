package com.villagemart.inventory;

import java.math.BigDecimal;

/**
 * Result of an applied stock change. For outbound changes {@code unitCost} is the weighted average cost
 * at the time of the movement and is used as cost of goods sold.
 */
public record AppliedMovement(
        Long movementId,
        Long productId,
        BigDecimal previousQuantity,
        BigDecimal newQuantity,
        BigDecimal unitCost,
        BigDecimal averageCostAfter) {
}
