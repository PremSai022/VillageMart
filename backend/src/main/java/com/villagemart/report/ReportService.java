package com.villagemart.report;

import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.expense.ExpenseRepository;
import com.villagemart.inventory.InventoryDtos.InventoryItemDto;
import com.villagemart.inventory.InventoryQueryService;
import com.villagemart.inventory.StockStatus;
import com.villagemart.payment.PaymentDirection;
import com.villagemart.payment.PaymentMethod;
import com.villagemart.payment.PaymentRepository;
import com.villagemart.payment.PaymentType;
import com.villagemart.purchase.Purchase;
import com.villagemart.purchase.PurchaseRepository;
import com.villagemart.report.ReportDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportQueryRepository queries;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryQueryService inventoryQueryService;

    // ---------------------------------------------------------------- sales

    public SalesReport dailySales(DateRange range, Long villageId) {
        Map<LocalDate, Acc> byDate = salesAccumulators(range, villageId);
        List<SalesPeriodRow> rows = new ArrayList<>();
        Acc total = new Acc();
        byDate.forEach((date, acc) -> {
            rows.add(acc.row(date.toString(), date, date));
            total.add(acc);
        });
        return new SalesReport(range.from(), range.to(), villageId, total.totals(), rows);
    }

    public SalesReport monthlySales(DateRange range, Long villageId) {
        Map<LocalDate, Acc> byDate = salesAccumulators(range, villageId);
        Map<YearMonth, Acc> byMonth = new TreeMap<>();
        Acc total = new Acc();
        byDate.forEach((date, acc) -> {
            byMonth.computeIfAbsent(YearMonth.from(date), k -> new Acc()).add(acc);
            total.add(acc);
        });
        List<SalesPeriodRow> rows = new ArrayList<>();
        byMonth.forEach((month, acc) -> {
            LocalDate start = month.atDay(1).isBefore(range.from()) ? range.from() : month.atDay(1);
            LocalDate end = month.atEndOfMonth().isAfter(range.to()) ? range.to() : month.atEndOfMonth();
            rows.add(acc.row(month.toString(), start, end));
        });
        return new SalesReport(range.from(), range.to(), villageId, total.totals(), rows);
    }

    public SalesTotals salesTotals(DateRange range) {
        Acc total = new Acc();
        salesAccumulators(range, null).values().forEach(total::add);
        return total.totals();
    }

    private Map<LocalDate, Acc> salesAccumulators(DateRange range, Long villageId) {
        Map<LocalDate, Acc> byDate = new TreeMap<>();
        for (Object[] r : queries.salesByDate(range.from(), range.to(), villageId)) {
            Acc a = byDate.computeIfAbsent((LocalDate) r[0], k -> new Acc());
            a.invoices += ((Number) r[1]).longValue();
            a.taxable = a.taxable.add(bd(r[2]));
            a.tax = a.tax.add(bd(r[3]));
            a.billed = a.billed.add(bd(r[4]));
            a.cost = a.cost.add(bd(r[5]));
        }
        for (Object[] r : queries.returnsByDate(range.from(), range.to(), villageId)) {
            Acc a = byDate.computeIfAbsent((LocalDate) r[0], k -> new Acc());
            a.returns += ((Number) r[1]).longValue();
            a.returnedRevenue = a.returnedRevenue.add(bd(r[2]));
            a.returnedCost = a.returnedCost.add(bd(r[3]));
            a.refunds = a.refunds.add(bd(r[4]));
        }
        return byDate;
    }

    public ProductSalesReport productSales(DateRange range) {
        List<ProductSalesRow> rows = productRows(range);
        BigDecimal revenue = rows.stream().map(ProductSalesRow::netRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = rows.stream().map(ProductSalesRow::grossProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductSalesReport(range.from(), range.to(), Money.money(revenue), Money.money(profit), rows);
    }

    public List<ProductSalesRow> bestSellers(DateRange range, int limit) {
        return productRows(range).stream()
                .sorted(Comparator.comparing(ProductSalesRow::netQuantity).reversed()
                        .thenComparing(ProductSalesRow::netRevenue, Comparator.reverseOrder()))
                .filter(r -> r.netQuantity().signum() > 0)
                .limit(limit)
                .toList();
    }

    private List<ProductSalesRow> productRows(DateRange range) {
        Map<Long, Object[]> meta = new LinkedHashMap<>();
        Map<Long, BigDecimal[]> sold = new HashMap<>();
        Map<Long, BigDecimal[]> returned = new HashMap<>();
        for (Object[] r : queries.productSales(range.from(), range.to())) {
            Long id = (Long) r[0];
            meta.put(id, r);
            sold.put(id, new BigDecimal[]{bd(r[5]), bd(r[6]), bd(r[7])});
        }
        for (Object[] r : queries.productReturns(range.from(), range.to())) {
            Long id = (Long) r[0];
            meta.putIfAbsent(id, r);
            returned.put(id, new BigDecimal[]{bd(r[5]), bd(r[6]), bd(r[7])});
        }
        BigDecimal[] zero = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        List<ProductSalesRow> rows = new ArrayList<>();
        meta.forEach((id, m) -> {
            BigDecimal[] s = sold.getOrDefault(id, zero);
            BigDecimal[] r = returned.getOrDefault(id, zero);
            BigDecimal revenue = s[1].subtract(r[1]);
            BigDecimal cost = s[2].subtract(r[2]);
            rows.add(new ProductSalesRow(id, (String) m[1], (String) m[2], (String) m[3], (String) m[4],
                    Money.qty(s[0]), Money.qty(r[0]), Money.qty(s[0].subtract(r[0])),
                    Money.money(revenue), Money.money(cost), Money.money(revenue.subtract(cost))));
        });
        rows.sort(Comparator.comparing(ProductSalesRow::netRevenue).reversed().thenComparing(ProductSalesRow::productName));
        return rows;
    }

    public CategorySalesReport categorySales(DateRange range) {
        Map<String, List<ProductSalesRow>> grouped = new TreeMap<>();
        for (ProductSalesRow r : productRows(range)) {
            String category = r.categoryName() == null ? "Uncategorised" : r.categoryName();
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(r);
        }
        BigDecimal totalRevenue = grouped.values().stream().flatMap(List::stream)
                .map(ProductSalesRow::netRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategorySalesRow> rows = new ArrayList<>();
        grouped.forEach((name, list) -> {
            BigDecimal revenue = sum(list, ProductSalesRow::netRevenue);
            BigDecimal cost = sum(list, ProductSalesRow::costOfGoodsSold);
            rows.add(new CategorySalesRow(name, list.size(), Money.money(revenue), Money.money(cost),
                    Money.money(revenue.subtract(cost)), percent(revenue, totalRevenue)));
        });
        rows.sort(Comparator.comparing(CategorySalesRow::netRevenue).reversed());
        return new CategorySalesReport(range.from(), range.to(), Money.money(totalRevenue), rows);
    }

    public VillageSalesReport villageSales(DateRange range) {
        Map<Long, BigDecimal[]> returns = new LinkedHashMap<>();
        Map<Long, String> returnVillageNames = new HashMap<>();
        for (Object[] r : queries.villageReturns(range.from(), range.to())) {
            returns.put((Long) r[0], new BigDecimal[]{bd(r[2]), bd(r[3])});
            returnVillageNames.put((Long) r[0], (String) r[1]);
        }
        List<VillageSalesRow> rows = new ArrayList<>();
        for (Object[] r : queries.villageSales(range.from(), range.to())) {
            Long villageId = (Long) r[0];
            BigDecimal[] ret = returns.remove(villageId);
            BigDecimal revenue = bd(r[5]).subtract(ret == null ? BigDecimal.ZERO : ret[0]);
            BigDecimal cost = bd(r[6]).subtract(ret == null ? BigDecimal.ZERO : ret[1]);
            rows.add(new VillageSalesRow(villageId, villageId == null ? "No village" : (String) r[1],
                    ((Number) r[2]).longValue(), ((Number) r[3]).longValue(), Money.money(bd(r[4])),
                    Money.money(revenue), Money.money(cost), Money.money(revenue.subtract(cost)), Money.money(bd(r[7]))));
        }
        returns.forEach((villageId, ret) -> rows.add(new VillageSalesRow(villageId, villageId == null ? "No village" : returnVillageNames.get(villageId),
                0, 0, Money.money(BigDecimal.ZERO), Money.money(ret[0].negate()), Money.money(ret[1].negate()),
                Money.money(ret[1].subtract(ret[0])), Money.money(BigDecimal.ZERO))));
        rows.sort(Comparator.comparing(VillageSalesRow::netRevenue).reversed());
        return new VillageSalesReport(range.from(), range.to(), rows);
    }

    // ---------------------------------------------------------------- purchases & stock

    public PurchaseReport purchases(DateRange range) {
        List<Purchase> list = purchaseRepository.findCompletedBetween(range.from(), range.to());
        Map<Long, SupplierAcc> suppliers = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        List<PurchaseRow> rows = new ArrayList<>();
        for (Purchase p : list) {
            BigDecimal ret = Money.nz(p.getReturnedAmount());
            total = total.add(p.getTotalAmount());
            returned = returned.add(ret);
            SupplierAcc acc = suppliers.computeIfAbsent(p.getSupplier().getId(), k -> new SupplierAcc(p.getSupplier().getName()));
            acc.count++;
            acc.total = acc.total.add(p.getTotalAmount());
            acc.returned = acc.returned.add(ret);
            rows.add(new PurchaseRow(p.getId(), p.getPurchaseNumber(), p.getPurchaseDate(), p.getSupplier().getName(),
                    p.getSupplierInvoiceNumber(), p.getStatus(), p.getTotalAmount(), ret));
        }
        List<SupplierPurchaseRow> supplierRows = new ArrayList<>();
        suppliers.forEach((id, a) -> supplierRows.add(new SupplierPurchaseRow(id, a.name, a.count, Money.money(a.total),
                Money.money(a.returned), Money.money(a.total.subtract(a.returned)))));
        supplierRows.sort(Comparator.comparing(SupplierPurchaseRow::netAmount).reversed());
        return new PurchaseReport(range.from(), range.to(), rows.size(), Money.money(total), Money.money(returned),
                Money.money(total.subtract(returned)), supplierRows, rows);
    }

    public StockReport stock(Long categoryId, StockStatus status) {
        List<InventoryItemDto> items = new ArrayList<>();
        int page = 0;
        PageResponse<InventoryItemDto> chunk;
        do {
            chunk = inventoryQueryService.search(null, categoryId, status, false, page++, 100, null);
            items.addAll(chunk.content());
        } while (!chunk.last());
        return new StockReport(inventoryQueryService.summary(), items);
    }

    public List<InventoryItemDto> byStockStatus(StockStatus status) {
        return inventoryQueryService.byStatus(status);
    }

    // ---------------------------------------------------------------- expenses & profit

    public ExpenseReport expenses(DateRange range) {
        return new ExpenseReport(range.from(), range.to(),
                Money.money(expenseRepository.totalBetween(range.from(), range.to())),
                expenseCategories(range),
                expenseRepository.totalsByDate(range.from(), range.to()).stream()
                        .map(r -> new AmountByDate((LocalDate) r[0], Money.money(bd(r[1])))).toList());
    }

    private List<ExpenseCategoryRow> expenseCategories(DateRange range) {
        return expenseRepository.totalsByCategory(range.from(), range.to()).stream()
                .map(r -> new ExpenseCategoryRow((Long) r[0], (String) r[1], Money.money(bd(r[2])), ((Number) r[3]).longValue()))
                .toList();
    }

    public ProfitReport profit(DateRange range) {
        Map<LocalDate, Acc> byDate = salesAccumulators(range, null);
        Map<LocalDate, BigDecimal> expensesByDate = new TreeMap<>();
        for (Object[] r : expenseRepository.totalsByDate(range.from(), range.to())) {
            expensesByDate.put((LocalDate) r[0], bd(r[1]));
        }
        TreeMap<LocalDate, Boolean> dates = new TreeMap<>();
        byDate.keySet().forEach(d -> dates.put(d, true));
        expensesByDate.keySet().forEach(d -> dates.put(d, true));
        Acc total = new Acc();
        BigDecimal totalExpenses = BigDecimal.ZERO;
        List<ProfitDayRow> daily = new ArrayList<>();
        for (LocalDate d : dates.keySet()) {
            Acc a = byDate.getOrDefault(d, new Acc());
            BigDecimal exp = expensesByDate.getOrDefault(d, BigDecimal.ZERO);
            total.add(a);
            totalExpenses = totalExpenses.add(exp);
            BigDecimal gp = a.grossProfit();
            daily.add(new ProfitDayRow(d, Money.money(a.netRevenue()), Money.money(a.netCost()), Money.money(gp),
                    Money.money(exp), Money.money(gp.subtract(exp))));
        }
        BigDecimal gross = total.grossProfit();
        BigDecimal net = gross.subtract(totalExpenses);
        return new ProfitReport(range.from(), range.to(), Money.money(total.taxable), Money.money(total.returnedRevenue),
                Money.money(total.netRevenue()), Money.money(total.netCost()), Money.money(gross), Money.money(totalExpenses),
                Money.money(net), percent(gross, total.netRevenue()), percent(net, total.netRevenue()),
                expenseCategories(range), daily);
    }

    // ---------------------------------------------------------------- customers & payments

    public CustomerOutstandingReport customerOutstanding() {
        List<CustomerOutstandingRow> rows = queries.customerOutstanding().stream()
                .map(r -> new CustomerOutstandingRow((Long) r[0], (String) r[1], (String) r[2], (String) r[3],
                        Money.money(bd(r[4])), ((Number) r[5]).longValue(), (LocalDate) r[6]))
                .toList();
        return new CustomerOutstandingReport(Money.money(sum(rows, CustomerOutstandingRow::outstandingAmount)), rows.size(), rows);
    }

    public PaymentCollectionReport paymentCollections(DateRange range) {
        Map<PaymentMethod, long[]> counts = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, BigDecimal[]> sums = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            counts.put(m, new long[1]);
            sums.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] r : paymentRepository.totalsBetween(range.from(), range.to())) {
            PaymentMethod method = (PaymentMethod) r[0];
            PaymentDirection direction = (PaymentDirection) r[1];
            PaymentType type = (PaymentType) r[2];
            BigDecimal amount = bd(r[3]);
            BigDecimal[] s = sums.get(method);
            if (direction == PaymentDirection.OUT) {
                s[2] = s[2].add(amount);
            } else if (type == PaymentType.CREDIT_COLLECTION) {
                s[1] = s[1].add(amount);
            } else {
                s[0] = s[0].add(amount);
            }
            counts.get(method)[0] += ((Number) r[4]).longValue();
        }
        List<PaymentMethodRow> rows = new ArrayList<>();
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        for (PaymentMethod m : PaymentMethod.values()) {
            BigDecimal[] s = sums.get(m);
            collected = collected.add(s[0]).add(s[1]);
            refunds = refunds.add(s[2]);
            rows.add(new PaymentMethodRow(m, Money.money(s[0]), Money.money(s[1]), Money.money(s[2]),
                    Money.money(s[0].add(s[1]).subtract(s[2])), counts.get(m)[0]));
        }
        return new PaymentCollectionReport(range.from(), range.to(), Money.money(collected), Money.money(refunds),
                Money.money(collected.subtract(refunds)), Money.money(queries.creditGiven(range.from(), range.to())),
                Money.money(queries.totalOutstanding()), rows);
    }

    // ---------------------------------------------------------------- helpers

    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal bd(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal b) {
            return b;
        }
        return new BigDecimal(o.toString());
    }

    private static final class SupplierAcc {
        final String name;
        long count;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;

        SupplierAcc(String name) {
            this.name = name;
        }
    }

    /** Mutable accumulator for sales/returns figures. */
    static final class Acc {
        long invoices;
        long returns;
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal returnedRevenue = BigDecimal.ZERO;
        BigDecimal returnedCost = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;

        void add(Acc o) {
            invoices += o.invoices;
            returns += o.returns;
            billed = billed.add(o.billed);
            taxable = taxable.add(o.taxable);
            tax = tax.add(o.tax);
            cost = cost.add(o.cost);
            returnedRevenue = returnedRevenue.add(o.returnedRevenue);
            returnedCost = returnedCost.add(o.returnedCost);
            refunds = refunds.add(o.refunds);
        }

        BigDecimal netRevenue() {
            return taxable.subtract(returnedRevenue);
        }

        BigDecimal netCost() {
            return cost.subtract(returnedCost);
        }

        BigDecimal grossProfit() {
            return netRevenue().subtract(netCost());
        }

        SalesPeriodRow row(String label, LocalDate from, LocalDate to) {
            return new SalesPeriodRow(label, from, to, invoices, Money.money(billed), Money.money(taxable),
                    Money.money(returnedRevenue), Money.money(netRevenue()), Money.money(netCost()), Money.money(grossProfit()));
        }

        SalesTotals totals() {
            return new SalesTotals(invoices, Money.money(billed), Money.money(taxable), Money.money(tax), returns,
                    Money.money(returnedRevenue), Money.money(refunds), Money.money(netRevenue()), Money.money(netCost()),
                    Money.money(grossProfit()), percent(grossProfit(), netRevenue()));
        }
    }
}
