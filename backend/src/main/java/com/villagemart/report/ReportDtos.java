package com.villagemart.report;

import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.inventory.InventoryDtos.InventorySummaryDto;
import com.villagemart.payment.PaymentMethod;
import com.villagemart.purchase.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    /**
     * Sales figures for a period. Revenue figures exclude tax; returns are attributed to the return date;
     * cancelled sales are excluded entirely.
     */
    public record SalesTotals(long invoiceCount, BigDecimal billedAmount, BigDecimal grossSales, BigDecimal taxCollected,
                              long returnCount, BigDecimal returnedRevenue, BigDecimal refundAmount,
                              BigDecimal netRevenue, BigDecimal costOfGoodsSold, BigDecimal grossProfit,
                              BigDecimal grossMarginPercent) {
    }

    public record SalesPeriodRow(String period, LocalDate from, LocalDate to, long invoiceCount, BigDecimal billedAmount,
                                 BigDecimal grossSales, BigDecimal returnedRevenue, BigDecimal netRevenue,
                                 BigDecimal costOfGoodsSold, BigDecimal grossProfit) {
    }

    public record SalesReport(LocalDate from, LocalDate to, Long villageId, SalesTotals totals, List<SalesPeriodRow> rows) {
    }

    public record ProductSalesRow(Long productId, String productName, String sku, String categoryName, String unitCode,
                                  BigDecimal quantitySold, BigDecimal quantityReturned, BigDecimal netQuantity,
                                  BigDecimal netRevenue, BigDecimal costOfGoodsSold, BigDecimal grossProfit) {
    }

    public record ProductSalesReport(LocalDate from, LocalDate to, BigDecimal totalNetRevenue, BigDecimal totalGrossProfit,
                                     List<ProductSalesRow> rows) {
    }

    public record CategorySalesRow(String categoryName, long productCount, BigDecimal netRevenue,
                                   BigDecimal costOfGoodsSold, BigDecimal grossProfit, BigDecimal revenueSharePercent) {
    }

    public record CategorySalesReport(LocalDate from, LocalDate to, BigDecimal totalNetRevenue, List<CategorySalesRow> rows) {
    }

    public record VillageSalesRow(Long villageId, String villageName, long invoiceCount, long customerCount,
                                  BigDecimal billedAmount, BigDecimal netRevenue, BigDecimal costOfGoodsSold,
                                  BigDecimal grossProfit, BigDecimal outstandingAmount) {
    }

    public record VillageSalesReport(LocalDate from, LocalDate to, List<VillageSalesRow> rows) {
    }

    public record SupplierPurchaseRow(Long supplierId, String supplierName, long purchaseCount, BigDecimal totalAmount,
                                      BigDecimal returnedAmount, BigDecimal netAmount) {
    }

    public record PurchaseRow(Long id, String purchaseNumber, LocalDate purchaseDate, String supplierName,
                              String supplierInvoiceNumber, PurchaseStatus status, BigDecimal totalAmount,
                              BigDecimal returnedAmount) {
    }

    public record PurchaseReport(LocalDate from, LocalDate to, long purchaseCount, BigDecimal totalAmount,
                                 BigDecimal returnedAmount, BigDecimal netAmount, List<SupplierPurchaseRow> suppliers,
                                 List<PurchaseRow> purchases) {
    }

    public record StockReport(InventorySummaryDto summary, List<InventoryItemDto> items) {
    }

    public record ExpenseCategoryRow(Long categoryId, String categoryName, BigDecimal amount, long count) {
    }

    public record AmountByDate(LocalDate date, BigDecimal amount) {
    }

    public record ExpenseReport(LocalDate from, LocalDate to, BigDecimal total, List<ExpenseCategoryRow> byCategory,
                                List<AmountByDate> byDate) {
    }

    public record ProfitDayRow(LocalDate date, BigDecimal netRevenue, BigDecimal costOfGoodsSold, BigDecimal grossProfit,
                               BigDecimal expenses, BigDecimal netProfit) {
    }

    public record ProfitReport(LocalDate from, LocalDate to, BigDecimal grossSales, BigDecimal returnedRevenue,
                               BigDecimal netRevenue, BigDecimal costOfGoodsSold, BigDecimal grossProfit,
                               BigDecimal expenses, BigDecimal netProfit, BigDecimal grossMarginPercent,
                               BigDecimal netMarginPercent, List<ExpenseCategoryRow> expensesByCategory,
                               List<ProfitDayRow> daily) {
    }

    public record CustomerOutstandingRow(Long customerId, String customerName, String mobile, String villageName,
                                         BigDecimal outstandingAmount, long openInvoices, LocalDate oldestInvoiceDate) {
    }

    public record CustomerOutstandingReport(BigDecimal totalOutstanding, long customerCount,
                                            List<CustomerOutstandingRow> rows) {
    }

    public record PaymentMethodRow(PaymentMethod method, BigDecimal saleCollections, BigDecimal creditCollections,
                                   BigDecimal refunds, BigDecimal netCollected, long transactionCount) {
    }

    /** Only money actually received/refunded is counted; unpaid credit is reported separately and never mixed in. */
    public record PaymentCollectionReport(LocalDate from, LocalDate to, BigDecimal totalCollected, BigDecimal totalRefunds,
                                          BigDecimal netCollected, BigDecimal creditGiven,
                                          BigDecimal currentOutstanding, List<PaymentMethodRow> byMethod) {
    }
}
