package com.villagemart.inventory;

import java.math.BigDecimal;

/**
 * A requested stock change. {@code quantity} is always a positive magnitude; {@code inbound} defines direction.
 * For inbound changes {@code unitCost} feeds the weighted average cost. {@code reverseCost} marks an outbound change
 * that must also remove its cost contribution (purchase return / purchase cancellation).
 */
public record StockChange(
        Long productId,
        MovementType type,
        BigDecimal quantity,
        boolean inbound,
        BigDecimal unitCost,
        boolean reverseCost,
        String referenceType,
        Long referenceId,
        String referenceNumber,
        String reason) {

    public static StockChange inbound(Long productId, MovementType type, BigDecimal quantity, BigDecimal unitCost,
                                      String refType, Long refId, String refNumber, String reason) {
        return new StockChange(productId, type, quantity, true, unitCost, false, refType, refId, refNumber, reason);
    }

    public static StockChange outbound(Long productId, MovementType type, BigDecimal quantity,
                                       String refType, Long refId, String refNumber, String reason) {
        return new StockChange(productId, type, quantity, false, null, false, refType, refId, refNumber, reason);
    }

    public static StockChange purchaseReversal(Long productId, MovementType type, BigDecimal quantity, BigDecimal unitCost,
                                               String refType, Long refId, String refNumber, String reason) {
        return new StockChange(productId, type, quantity, false, unitCost, true, refType, refId, refNumber, reason);
    }
}
