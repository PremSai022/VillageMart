package com.villagemart.report;

import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.report.ReportDtos.*;

import java.util.List;

/** Converts report DTOs into exportable tables. */
final class ReportTables {

    private ReportTables() {
    }

    private static String period(java.time.LocalDate from, java.time.LocalDate to) {
        return new DateRange(from, to).label();
    }

    static ReportTable sales(String title, SalesReport r) {
        SalesTotals t = r.totals();
        ReportTable.Builder b = ReportTable.builder(title, "Period: " + period(r.from(), r.to()))
                .summary("Invoices", t.invoiceCount())
                .summary("Billed amount (incl. tax)", t.billedAmount())
                .summary("Sales excl. tax", t.grossSales())
                .summary("Tax collected", t.taxCollected())
                .summary("Returns", t.returnedRevenue())
                .summary("Net revenue", t.netRevenue())
                .summary("Cost of goods sold", t.costOfGoodsSold())
                .summary("Gross profit", t.grossProfit())
                .summary("Gross margin %", t.grossMarginPercent())
                .text("Period").integer("Invoices").money("Billed").money("Sales excl. tax").money("Returns")
                .money("Net revenue").money("COGS").money("Gross profit");
        for (SalesPeriodRow row : r.rows()) {
            b.row(row.period(), row.invoiceCount(), row.billedAmount(), row.grossSales(), row.returnedRevenue(),
                    row.netRevenue(), row.costOfGoodsSold(), row.grossProfit());
        }
        return b.build();
    }

    static ReportTable productSales(ProductSalesReport r) {
        ReportTable.Builder b = ReportTable.builder("Product-wise Sales", "Period: " + period(r.from(), r.to()))
                .summary("Net revenue", r.totalNetRevenue()).summary("Gross profit", r.totalGrossProfit())
                .text("Product").text("SKU").text("Category").text("Unit").qty("Sold").qty("Returned").qty("Net qty")
                .money("Net revenue").money("COGS").money("Gross profit");
        for (ProductSalesRow row : r.rows()) {
            b.row(row.productName(), row.sku(), row.categoryName(), row.unitCode(), row.quantitySold(), row.quantityReturned(),
                    row.netQuantity(), row.netRevenue(), row.costOfGoodsSold(), row.grossProfit());
        }
        return b.build();
    }

    static ReportTable categorySales(CategorySalesReport r) {
        ReportTable.Builder b = ReportTable.builder("Category-wise Sales", "Period: " + period(r.from(), r.to()))
                .summary("Net revenue", r.totalNetRevenue())
                .text("Category").integer("Products").money("Net revenue").money("COGS").money("Gross profit").percent("Share");
        for (CategorySalesRow row : r.rows()) {
            b.row(row.categoryName(), row.productCount(), row.netRevenue(), row.costOfGoodsSold(), row.grossProfit(),
                    row.revenueSharePercent());
        }
        return b.build();
    }

    static ReportTable villageSales(VillageSalesReport r) {
        ReportTable.Builder b = ReportTable.builder("Village-wise Sales", "Period: " + period(r.from(), r.to()))
                .text("Village").integer("Invoices").integer("Customers").money("Billed").money("Net revenue")
                .money("COGS").money("Gross profit").money("Outstanding");
        for (VillageSalesRow row : r.rows()) {
            b.row(row.villageName(), row.invoiceCount(), row.customerCount(), row.billedAmount(), row.netRevenue(),
                    row.costOfGoodsSold(), row.grossProfit(), row.outstandingAmount());
        }
        return b.build();
    }

    static ReportTable purchases(PurchaseReport r) {
        ReportTable.Builder b = ReportTable.builder("Purchase Report", "Period: " + period(r.from(), r.to()))
                .summary("Purchases", r.purchaseCount()).summary("Total purchased", r.totalAmount())
                .summary("Returned to suppliers", r.returnedAmount()).summary("Net purchases", r.netAmount())
                .text("Date").text("Purchase #").text("Supplier").text("Supplier invoice").money("Total").money("Returned");
        for (PurchaseRow row : r.purchases()) {
            b.row(row.purchaseDate().toString(), row.purchaseNumber(), row.supplierName(), row.supplierInvoiceNumber(),
                    row.totalAmount(), row.returnedAmount());
        }
        return b.build();
    }

    static ReportTable stock(String title, String subtitle, List<InventoryItemDto> items, ReportTable.Builder seed) {
        ReportTable.Builder b = seed == null ? ReportTable.builder(title, subtitle) : seed;
        b.text("Product").text("SKU").text("Category").text("Unit").qty("Available").qty("Minimum").text("Status")
                .qty("Suggested reorder").money("Avg cost").money("Stock value");
        for (InventoryItemDto i : items) {
            b.row(i.productName(), i.sku(), i.categoryName(), i.unitCode(), i.availableQuantity(), i.minStockQuantity(),
                    i.stockStatus().name(), i.suggestedReorderQuantity(), i.averageCost(), i.stockValue());
        }
        return b.build();
    }

    static ReportTable stockReport(StockReport r, String asOf) {
        ReportTable.Builder seed = ReportTable.builder("Stock Report", "As of " + asOf)
                .summary("Products", r.summary().totalProducts())
                .summary("Total quantity", r.summary().totalQuantity())
                .summary("Inventory value (at avg cost)", r.summary().totalValue())
                .summary("Low stock", r.summary().lowStockCount())
                .summary("Out of stock", r.summary().outOfStockCount());
        return stock(null, null, r.items(), seed);
    }

    static ReportTable expenses(ExpenseReport r) {
        ReportTable.Builder b = ReportTable.builder("Expense Report", "Period: " + period(r.from(), r.to()))
                .summary("Total expenses", r.total());
        r.byCategory().forEach(c -> b.summary(c.categoryName() + " (" + c.count() + ")", c.amount()));
        b.text("Date").money("Amount");
        r.byDate().forEach(d -> b.row(d.date().toString(), d.amount()));
        return b.build();
    }

    static ReportTable profit(ProfitReport r) {
        ReportTable.Builder b = ReportTable.builder("Profit Report", "Period: " + period(r.from(), r.to()))
                .summary("Sales excl. tax", r.grossSales())
                .summary("Returns", r.returnedRevenue())
                .summary("Net revenue", r.netRevenue())
                .summary("Cost of goods sold (weighted avg)", r.costOfGoodsSold())
                .summary("Gross profit", r.grossProfit())
                .summary("Business expenses", r.expenses())
                .summary("Net profit", r.netProfit())
                .summary("Gross margin %", r.grossMarginPercent())
                .summary("Net margin %", r.netMarginPercent())
                .text("Date").money("Net revenue").money("COGS").money("Gross profit").money("Expenses").money("Net profit");
        for (ProfitDayRow d : r.daily()) {
            b.row(d.date().toString(), d.netRevenue(), d.costOfGoodsSold(), d.grossProfit(), d.expenses(), d.netProfit());
        }
        return b.build();
    }

    static ReportTable customerOutstanding(CustomerOutstandingReport r, String asOf) {
        ReportTable.Builder b = ReportTable.builder("Customer Outstanding", "As of " + asOf)
                .summary("Customers with dues", r.customerCount()).summary("Total outstanding", r.totalOutstanding())
                .text("Customer").text("Mobile").text("Village").integer("Open invoices").text("Oldest invoice")
                .money("Outstanding");
        for (CustomerOutstandingRow row : r.rows()) {
            b.row(row.customerName(), row.mobile(), row.villageName(), row.openInvoices(),
                    row.oldestInvoiceDate() == null ? "" : row.oldestInvoiceDate().toString(), row.outstandingAmount());
        }
        return b.build();
    }

    static ReportTable payments(PaymentCollectionReport r) {
        ReportTable.Builder b = ReportTable.builder("Payment Collections", "Period: " + period(r.from(), r.to()))
                .summary("Collected", r.totalCollected()).summary("Refunded", r.totalRefunds())
                .summary("Net collected", r.netCollected()).summary("Credit given (unpaid at billing)", r.creditGiven())
                .summary("Current total outstanding", r.currentOutstanding())
                .text("Method").money("Sale payments").money("Credit collections").money("Refunds").money("Net")
                .integer("Transactions");
        for (PaymentMethodRow row : r.byMethod()) {
            b.row(row.method().name(), row.saleCollections(), row.creditCollections(), row.refunds(), row.netCollected(),
                    row.transactionCount());
        }
        return b.build();
    }
}
