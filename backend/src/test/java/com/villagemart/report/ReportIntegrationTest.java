package com.villagemart.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a dedicated historical business date so totals are isolated from data created by other test classes.
 * Scenario: opening 100 @ 10, sell 10 @ 15 (revenue 150, COGS 100), return 2 saleable (revenue -30, COGS -20),
 * expense 20 => net revenue 120, COGS 80, gross profit 40, net profit 20.
 */
class ReportIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate DAY = LocalDate.now().minusDays(777);
    private static Long saleId;
    private static Long productId;
    private static Long villageId;

    @BeforeEach
    void seed() throws Exception {
        if (saleId != null) {
            return;
        }
        productId = createProduct("10.00", "15.00", "5", "100");
        villageId = post("/api/v1/villages", Map.of("name", unique("Report Village"), "state", "Tamil Nadu"))
                .expect(201).body().path("id").asLong();
        Map<String, Object> req = saleRequest("CASH", productId, "10");
        req.put("saleDate", DAY.toString());
        req.put("villageId", villageId);
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        saleId = sale.path("id").asLong();
        long itemId = sale.path("items").get(0).path("id").asLong();
        post("/api/v1/sales/" + saleId + "/returns", Map.of("reason", "Customer changed mind", "returnDate", DAY.toString(),
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 2, "saleable", true)))).expect(201);
        long fuel = get("/api/v1/expenses/categories").expect(200).body().get(0).path("id").asLong();
        post("/api/v1/expenses", Map.of("categoryId", fuel, "amount", 20, "expenseDate", DAY.toString(),
                "description", "Diesel")).expect(201);
    }

    private String range() {
        return "from=" + DAY + "&to=" + DAY;
    }

    @Test
    void profitUsesWeightedAverageCostAndExpenses() throws Exception {
        JsonNode p = get("/api/v1/reports/profit?" + range()).expect(200).body();
        assertThat(p.path("grossSales").decimalValue()).isEqualByComparingTo("150");
        assertThat(p.path("returnedRevenue").decimalValue()).isEqualByComparingTo("30");
        assertThat(p.path("netRevenue").decimalValue()).isEqualByComparingTo("120");
        assertThat(p.path("costOfGoodsSold").decimalValue()).isEqualByComparingTo("80");
        assertThat(p.path("grossProfit").decimalValue()).isEqualByComparingTo("40");
        assertThat(p.path("expenses").decimalValue()).isEqualByComparingTo("20");
        assertThat(p.path("netProfit").decimalValue()).isEqualByComparingTo("20");
        assertThat(p.path("daily")).hasSize(1);
    }

    @Test
    void dailyMonthlyProductCategoryAndVillageSales() throws Exception {
        JsonNode daily = get("/api/v1/reports/daily-sales?" + range()).expect(200).body();
        assertThat(daily.path("totals").path("invoiceCount").asLong()).isEqualTo(1);
        assertThat(daily.path("totals").path("billedAmount").decimalValue()).isEqualByComparingTo("150");
        assertThat(daily.path("totals").path("grossProfit").decimalValue()).isEqualByComparingTo("40");
        assertThat(daily.path("rows").get(0).path("netRevenue").decimalValue()).isEqualByComparingTo("120");

        JsonNode monthly = get("/api/v1/reports/monthly-sales?" + range()).expect(200).body();
        assertThat(monthly.path("rows")).hasSize(1);
        assertThat(monthly.path("rows").get(0).path("period").asText()).isEqualTo(DAY.toString().substring(0, 7));

        JsonNode products = get("/api/v1/reports/product-sales?" + range()).expect(200).body();
        JsonNode row = products.path("rows").get(0);
        assertThat(row.path("productId").asLong()).isEqualTo(productId);
        assertThat(row.path("quantitySold").decimalValue()).isEqualByComparingTo("10");
        assertThat(row.path("quantityReturned").decimalValue()).isEqualByComparingTo("2");
        assertThat(row.path("netQuantity").decimalValue()).isEqualByComparingTo("8");

        JsonNode categories = get("/api/v1/reports/category-sales?" + range()).expect(200).body();
        assertThat(categories.path("rows").get(0).path("categoryName").asText()).isEqualTo("Other");
        assertThat(categories.path("rows").get(0).path("revenueSharePercent").decimalValue()).isEqualByComparingTo("100");

        JsonNode villages = get("/api/v1/reports/village-sales?" + range()).expect(200).body();
        JsonNode v = villages.path("rows").get(0);
        assertThat(v.path("villageId").asLong()).isEqualTo(villageId);
        assertThat(v.path("netRevenue").decimalValue()).isEqualByComparingTo("120");
        assertThat(v.path("grossProfit").decimalValue()).isEqualByComparingTo("40");

        JsonNode villageDaily = get("/api/v1/reports/village-sales/" + villageId + "?" + range()).expect(200).body();
        assertThat(villageDaily.path("totals").path("invoiceCount").asLong()).isEqualTo(1);
    }

    @Test
    void paymentCollectionsSeparateRefunds() throws Exception {
        JsonNode r = get("/api/v1/reports/payment-collections?" + range()).expect(200).body();
        assertThat(r.path("totalCollected").decimalValue()).isEqualByComparingTo("150");
        assertThat(r.path("totalRefunds").decimalValue()).isEqualByComparingTo("30");
        assertThat(r.path("netCollected").decimalValue()).isEqualByComparingTo("120");
        assertThat(r.path("creditGiven").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void expenseAndStockReports() throws Exception {
        JsonNode e = get("/api/v1/reports/expenses?" + range()).expect(200).body();
        assertThat(e.path("total").decimalValue()).isEqualByComparingTo("20");
        JsonNode stock = get("/api/v1/reports/stock").expect(200).body();
        assertThat(stock.path("summary").path("totalProducts").asLong()).isGreaterThan(0);
        assertThat(stock.path("items").size()).isGreaterThan(0);
        get("/api/v1/reports/low-stock").expect(200);
        get("/api/v1/reports/out-of-stock").expect(200);
        get("/api/v1/reports/customer-outstanding").expect(200);
    }

    @Test
    void dashboardSummaryForDateRange() throws Exception {
        JsonNode d = get("/api/v1/dashboard/summary?" + range()).expect(200).body();
        assertThat(d.path("salesAmount").decimalValue()).isEqualByComparingTo("150");
        assertThat(d.path("grossProfit").decimalValue()).isEqualByComparingTo("40");
        assertThat(d.path("netProfit").decimalValue()).isEqualByComparingTo("20");
        assertThat(d.path("totalProducts").asLong()).isGreaterThan(0);
        assertThat(d.path("bestSellers").get(0).path("productId").asLong()).isEqualTo(productId);
        assertThat(d.path("recentSales").isArray()).isTrue();
        get("/api/v1/dashboard/summary").expect(200);
        get("/api/v1/dashboard/summary?period=THIS_MONTH").expect(200);
    }

    @Test
    void exportsAndValidation() throws Exception {
        byte[] pdf = binary("/api/v1/reports/profit?" + range() + "&format=pdf", "application/pdf");
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        byte[] xlsx = binary("/api/v1/reports/product-sales?" + range() + "&format=xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
        binary("/api/v1/reports/stock?format=pdf", "application/pdf");
        binary("/api/v1/reports/daily-sales?period=YESTERDAY&format=xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        assertThat(get("/api/v1/reports/profit?format=doc").expect(400).code()).isEqualTo("INVALID_FORMAT");
        assertThat(get("/api/v1/reports/profit?from=2025-02-01&to=2025-01-01").expect(400).code()).isEqualTo("INVALID_DATE_RANGE");
        assertThat(get("/api/v1/reports/profit?period=FOREVER").expect(400).code()).isEqualTo("INVALID_PERIOD");
    }

    @Test
    void invoicePdfIsGenerated() throws Exception {
        byte[] pdf = binary("/api/v1/sales/" + saleId + "/invoice", "application/pdf");
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1000);
    }

    private byte[] binary(String url, String contentType) throws Exception {
        MvcResult r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token)).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(r.getResponse().getContentType()).startsWith(contentType);
        assertThat(r.getResponse().getHeader("Content-Disposition")).contains("filename=");
        return r.getResponse().getContentAsByteArray();
    }
}
