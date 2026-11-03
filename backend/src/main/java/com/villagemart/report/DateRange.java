package com.villagemart.report;

import com.villagemart.common.ApiException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Inclusive date range used by every report. */
public record DateRange(LocalDate from, LocalDate to) {

    public static final long MAX_DAYS = 3660;

    /**
     * Resolves a report period. Explicit from/to wins (CUSTOM); otherwise the preset is applied relative to today.
     * Supported presets: TODAY, YESTERDAY, THIS_WEEK, LAST_7_DAYS, THIS_MONTH, LAST_MONTH, LAST_30_DAYS, THIS_YEAR.
     */
    public static DateRange resolve(String period, LocalDate from, LocalDate to, LocalDate today) {
        if (from != null || to != null) {
            LocalDate f = from != null ? from : to;
            LocalDate t = to != null ? to : from;
            if (f.isAfter(t)) {
                throw ApiException.badRequest("INVALID_DATE_RANGE", "'from' date must be on or before 'to' date");
            }
            if (ChronoUnit.DAYS.between(f, t) > MAX_DAYS) {
                throw ApiException.badRequest("INVALID_DATE_RANGE", "Date range cannot exceed 10 years");
            }
            return new DateRange(f, t);
        }
        String p = period == null || period.isBlank() ? "TODAY" : period.trim().toUpperCase();
        return switch (p) {
            case "TODAY" -> new DateRange(today, today);
            case "YESTERDAY" -> new DateRange(today.minusDays(1), today.minusDays(1));
            case "THIS_WEEK", "WEEK" -> new DateRange(today.with(DayOfWeek.MONDAY), today);
            case "LAST_7_DAYS" -> new DateRange(today.minusDays(6), today);
            case "THIS_MONTH", "MONTH" -> new DateRange(today.withDayOfMonth(1), today);
            case "LAST_MONTH" -> {
                LocalDate start = today.minusMonths(1).withDayOfMonth(1);
                yield new DateRange(start, start.withDayOfMonth(start.lengthOfMonth()));
            }
            case "LAST_30_DAYS" -> new DateRange(today.minusDays(29), today);
            case "THIS_YEAR", "YEAR" -> new DateRange(today.withDayOfYear(1), today);
            default -> throw ApiException.badRequest("INVALID_PERIOD", "Unknown period " + period);
        };
    }

    public String label() {
        return from.equals(to) ? from.toString() : from + " to " + to;
    }
}
