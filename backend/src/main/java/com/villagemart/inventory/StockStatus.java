package com.villagemart.inventory;

import java.math.BigDecimal;

public enum StockStatus {
    NORMAL, LOW_STOCK, OUT_OF_STOCK;

    /**
     * qty == 0 -> OUT_OF_STOCK; 0 < qty <= minimum -> LOW_STOCK; otherwise NORMAL.
     */
    public static StockStatus of(BigDecimal quantity, BigDecimal minimum) {
        if (quantity == null || quantity.signum() <= 0) {
            return OUT_OF_STOCK;
        }
        if (minimum != null && quantity.compareTo(minimum) <= 0) {
            return LOW_STOCK;
        }
        return NORMAL;
    }
}
