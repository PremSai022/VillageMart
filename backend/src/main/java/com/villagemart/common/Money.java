package com.villagemart.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central BigDecimal helpers. Money is kept at 2 decimals, quantities at 3, unit costs at 4.
 */
public final class Money {

    public static final int MONEY_SCALE = 2;
    public static final int QTY_SCALE = 3;
    public static final int COST_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal qty(BigDecimal value) {
        return nz(value).setScale(QTY_SCALE, ROUNDING);
    }

    public static BigDecimal cost(BigDecimal value) {
        return nz(value).setScale(COST_SCALE, ROUNDING);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    public static BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
        return money(nz(amount).multiply(nz(ratePercent)).divide(BigDecimal.valueOf(100), 6, ROUNDING));
    }
}
