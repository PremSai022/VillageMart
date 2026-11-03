package com.villagemart.dashboard;

import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.report.ReportDtos.ExpenseCategoryRow;
import com.villagemart.report.ReportDtos.ProductSalesRow;
import com.villagemart.report.ReportDtos.PurchaseRow;
import com.villagemart.report.ReportDtos.SalesTotals;
import com.villagemart.report.ReportDtos.VillageSalesRow;
import com.villagemart.sales.SaleDtos.SaleSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummaryDto(
            LocalDate from, LocalDate to,
            SalesTotals sales,
            BigDecimal salesAmount, BigDecimal grossProfit, BigDecimal expenses, BigDecimal netProfit,
            BigDecimal netCollected, BigDecimal creditGiven, BigDecimal totalOutstanding,
            long totalProducts, BigDecimal totalInventoryQuantity, BigDecimal totalInventoryValue,
            long lowStockCount, long outOfStockCount,
            List<InventoryItemDto> lowStockItems, List<InventoryItemDto> outOfStockItems,
            List<SaleSummaryDto> recentSales, List<PurchaseRow> recentPurchases,
            List<ExpenseCategoryRow> expensesByCategory, List<ProductSalesRow> bestSellers,
            List<VillageSalesRow> villageSummary) {
    }
}
