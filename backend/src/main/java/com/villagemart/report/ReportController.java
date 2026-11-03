package com.villagemart.report;

import com.villagemart.common.ApiException;
import com.villagemart.common.BusinessClock;
import com.villagemart.inventory.StockStatus;
import com.villagemart.report.ReportDtos.*;
import com.villagemart.settings.BusinessSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * All reports accept {@code format=json|pdf|xlsx}. Date-based reports accept either an explicit
 * {@code from}/{@code to} range or a {@code period} preset (TODAY, YESTERDAY, THIS_WEEK, LAST_7_DAYS, THIS_MONTH,
 * LAST_MONTH, LAST_30_DAYS, THIS_YEAR).
 */
@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService service;
    private final ReportExporter exporter;
    private final BusinessClock clock;
    private final BusinessSettingsService settingsService;

    @Operation(summary = "Day-by-day sales (today, yesterday, weekly, custom range)")
    @GetMapping({"/daily-sales", "/sales"})
    public ResponseEntity<?> dailySales(@RequestParam(required = false) String period,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) Long villageId,
                                        @RequestParam(defaultValue = "json") String format) {
        SalesReport r = service.dailySales(range(period, from, to), villageId);
        return respond(r, format, () -> ReportTables.sales("Sales Report", r), "sales-report");
    }

    @Operation(summary = "Month-by-month sales; defaults to the current year")
    @GetMapping("/monthly-sales")
    public ResponseEntity<?> monthlySales(@RequestParam(required = false) String period,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(required = false) Long villageId,
                                          @RequestParam(defaultValue = "json") String format) {
        DateRange range = range(period == null && from == null && to == null ? "THIS_YEAR" : period, from, to);
        SalesReport r = service.monthlySales(range, villageId);
        return respond(r, format, () -> ReportTables.sales("Monthly Sales", r), "monthly-sales");
    }

    @GetMapping("/product-sales")
    public ResponseEntity<?> productSales(@RequestParam(required = false) String period,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(defaultValue = "json") String format) {
        ProductSalesReport r = service.productSales(range(period, from, to));
        return respond(r, format, () -> ReportTables.productSales(r), "product-sales");
    }

    @GetMapping("/category-sales")
    public ResponseEntity<?> categorySales(@RequestParam(required = false) String period,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(defaultValue = "json") String format) {
        CategorySalesReport r = service.categorySales(range(period, from, to));
        return respond(r, format, () -> ReportTables.categorySales(r), "category-sales");
    }

    @GetMapping("/village-sales")
    public ResponseEntity<?> villageSales(@RequestParam(required = false) String period,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(defaultValue = "json") String format) {
        VillageSalesReport r = service.villageSales(range(period, from, to));
        return respond(r, format, () -> ReportTables.villageSales(r), "village-sales");
    }

    @Operation(summary = "Date-wise sales for one village")
    @GetMapping("/village-sales/{villageId}")
    public ResponseEntity<?> villageDaily(@PathVariable Long villageId,
                                          @RequestParam(required = false) String period,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(defaultValue = "json") String format) {
        SalesReport r = service.dailySales(range(period == null && from == null && to == null ? "THIS_MONTH" : period, from, to), villageId);
        return respond(r, format, () -> ReportTables.sales("Village Sales", r), "village-" + villageId + "-sales");
    }

    @GetMapping("/purchases")
    public ResponseEntity<?> purchases(@RequestParam(required = false) String period,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(defaultValue = "json") String format) {
        PurchaseReport r = service.purchases(range(period, from, to));
        return respond(r, format, () -> ReportTables.purchases(r), "purchases");
    }

    @GetMapping("/stock")
    public ResponseEntity<?> stock(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) StockStatus status,
                                   @RequestParam(defaultValue = "json") String format) {
        StockReport r = service.stock(categoryId, status);
        return respond(r, format, () -> ReportTables.stockReport(r, clock.today().toString()), "stock");
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> lowStock(@RequestParam(defaultValue = "json") String format) {
        var items = service.byStockStatus(StockStatus.LOW_STOCK);
        return respond(items, format, () -> ReportTables.stock("Low Stock Report", "As of " + clock.today(), items, null), "low-stock");
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<?> outOfStock(@RequestParam(defaultValue = "json") String format) {
        var items = service.byStockStatus(StockStatus.OUT_OF_STOCK);
        return respond(items, format, () -> ReportTables.stock("Out of Stock Report", "As of " + clock.today(), items, null), "out-of-stock");
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> expenses(@RequestParam(required = false) String period,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(defaultValue = "json") String format) {
        ExpenseReport r = service.expenses(range(period, from, to));
        return respond(r, format, () -> ReportTables.expenses(r), "expenses");
    }

    @Operation(summary = "Gross and net profit (gross profit = net revenue excl. tax - weighted-average COGS; net = gross - expenses)")
    @GetMapping({"/profit", "/gross-profit", "/net-profit"})
    public ResponseEntity<?> profit(@RequestParam(required = false) String period,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(defaultValue = "json") String format) {
        ProfitReport r = service.profit(range(period, from, to));
        return respond(r, format, () -> ReportTables.profit(r), "profit");
    }

    @GetMapping("/customer-outstanding")
    public ResponseEntity<?> customerOutstanding(@RequestParam(defaultValue = "json") String format) {
        CustomerOutstandingReport r = service.customerOutstanding();
        return respond(r, format, () -> ReportTables.customerOutstanding(r, clock.today().toString()), "customer-outstanding");
    }

    @GetMapping("/payment-collections")
    public ResponseEntity<?> paymentCollections(@RequestParam(required = false) String period,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(defaultValue = "json") String format) {
        PaymentCollectionReport r = service.paymentCollections(range(period, from, to));
        return respond(r, format, () -> ReportTables.payments(r), "payment-collections");
    }

    private DateRange range(String period, LocalDate from, LocalDate to) {
        return DateRange.resolve(period, from, to, clock.today());
    }

    private ResponseEntity<?> respond(Object body, String format, Supplier<ReportTable> table, String baseName) {
        String f = format == null ? "json" : format.trim().toLowerCase();
        String business = settingsService.get().businessName();
        return switch (f) {
            case "json" -> ResponseEntity.ok(body);
            case "pdf" -> file(exporter.pdf(table.get(), business), MediaType.APPLICATION_PDF, baseName + ".pdf");
            case "xlsx", "excel" -> file(exporter.xlsx(table.get(), business), XLSX, baseName + ".xlsx");
            default -> throw ApiException.badRequest("INVALID_FORMAT", "Supported formats: json, pdf, xlsx");
        };
    }

    static ResponseEntity<byte[]> file(byte[] bytes, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentLength(bytes.length)
                .body(bytes);
    }
}
