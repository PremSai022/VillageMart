package com.villagemart.dashboard;

import com.villagemart.dashboard.DashboardDtos.DashboardSummaryDto;
import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.inventory.InventoryDtos.InventorySummaryDto;
import com.villagemart.inventory.InventoryQueryService;
import com.villagemart.inventory.StockStatus;
import com.villagemart.purchase.PurchaseRepository;
import com.villagemart.purchase.PurchaseStatus;
import com.villagemart.report.DateRange;
import com.villagemart.report.ReportDtos.PaymentCollectionReport;
import com.villagemart.report.ReportDtos.ProfitReport;
import com.villagemart.report.ReportDtos.PurchaseRow;
import com.villagemart.report.ReportDtos.SalesTotals;
import com.villagemart.report.ReportService;
import com.villagemart.sales.SaleRepository;
import com.villagemart.sales.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int ALERT_LIMIT = 10;

    private final ReportService reportService;
    private final InventoryQueryService inventoryQueryService;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;

    public DashboardSummaryDto summary(DateRange range) {
        SalesTotals sales = reportService.salesTotals(range);
        ProfitReport profit = reportService.profit(range);
        PaymentCollectionReport payments = reportService.paymentCollections(range);
        InventorySummaryDto stock = inventoryQueryService.summary();
        List<InventoryItemDto> low = top(inventoryQueryService.byStatus(StockStatus.LOW_STOCK));
        List<InventoryItemDto> out = top(inventoryQueryService.byStatus(StockStatus.OUT_OF_STOCK));
        var recentSales = saleRepository.search(null, null, null, null, null, null, null,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(SaleService::toSummary).getContent();
        List<PurchaseRow> recentPurchases = purchaseRepository.findTop5ByStatusOrderByCompletedAtDesc(PurchaseStatus.COMPLETED)
                .stream()
                .map(p -> new PurchaseRow(p.getId(), p.getPurchaseNumber(), p.getPurchaseDate(), p.getSupplier().getName(),
                        p.getSupplierInvoiceNumber(), p.getStatus(), p.getTotalAmount(), p.getReturnedAmount()))
                .toList();
        return new DashboardSummaryDto(range.from(), range.to(), sales,
                sales.billedAmount(), profit.grossProfit(), profit.expenses(), profit.netProfit(),
                payments.netCollected(), payments.creditGiven(), payments.currentOutstanding(),
                stock.totalProducts(), stock.totalQuantity(), stock.totalValue(),
                stock.lowStockCount(), stock.outOfStockCount(), low, out,
                recentSales, recentPurchases, profit.expensesByCategory(),
                reportService.bestSellers(range, 5), reportService.villageSales(range).rows());
    }

    private static List<InventoryItemDto> top(List<InventoryItemDto> items) {
        return items.stream()
                .sorted(Comparator.comparing(InventoryItemDto::availableQuantity).thenComparing(InventoryItemDto::productName))
                .limit(ALERT_LIMIT)
                .toList();
    }
}
