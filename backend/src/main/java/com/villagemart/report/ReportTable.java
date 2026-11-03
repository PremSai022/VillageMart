package com.villagemart.report;

import java.util.ArrayList;
import java.util.List;

/** Format-neutral tabular representation of a report used by the PDF and Excel exporters. */
public record ReportTable(String title, String subtitle, List<Column> columns, List<List<Object>> rows,
                          List<String[]> summary) {

    public enum ColumnType { TEXT, MONEY, QUANTITY, INTEGER, PERCENT }

    public record Column(String header, ColumnType type) {
    }

    public static Builder builder(String title, String subtitle) {
        return new Builder(title, subtitle);
    }

    public static final class Builder {
        private final String title;
        private final String subtitle;
        private final List<Column> columns = new ArrayList<>();
        private final List<List<Object>> rows = new ArrayList<>();
        private final List<String[]> summary = new ArrayList<>();

        private Builder(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public Builder text(String header) {
            columns.add(new Column(header, ColumnType.TEXT));
            return this;
        }

        public Builder money(String header) {
            columns.add(new Column(header, ColumnType.MONEY));
            return this;
        }

        public Builder qty(String header) {
            columns.add(new Column(header, ColumnType.QUANTITY));
            return this;
        }

        public Builder integer(String header) {
            columns.add(new Column(header, ColumnType.INTEGER));
            return this;
        }

        public Builder percent(String header) {
            columns.add(new Column(header, ColumnType.PERCENT));
            return this;
        }

        public Builder row(Object... values) {
            if (values.length != columns.size()) {
                throw new IllegalArgumentException("Row has " + values.length + " values but table has " + columns.size() + " columns");
            }
            rows.add(java.util.Arrays.asList(values));
            return this;
        }

        public Builder summary(String label, Object value) {
            summary.add(new String[]{label, ReportFormat.display(value)});
            return this;
        }

        public ReportTable build() {
            return new ReportTable(title, subtitle, List.copyOf(columns), List.copyOf(rows), List.copyOf(summary));
        }
    }
}
