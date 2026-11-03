package com.villagemart.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class ReportFormat {

    private ReportFormat() {
    }

    static String money(BigDecimal v) {
        return v == null ? "" : new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }

    static String qty(BigDecimal v) {
        return v == null ? "" : new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }

    static String display(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal b) {
            return money(b);
        }
        return v.toString();
    }

    static String cell(Object v, ReportTable.ColumnType type) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal b) {
            return switch (type) {
                case MONEY -> money(b);
                case QUANTITY -> qty(b);
                case PERCENT -> money(b) + "%";
                default -> b.toPlainString();
            };
        }
        return v.toString();
    }
}
