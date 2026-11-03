package com.villagemart.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SalesIntegrationTest extends IntegrationTestSupport {

    @Test
    void creditRequiresCustomerAndOutstandingIsTracked() throws Exception {
        long product = createProduct("30.00", "50.00", "1", "20");
        Map<String, Object> req = saleRequest("CREDIT", product, "3");
        assertThat(post("/api/v1/sales", req, newKey()).expect(422).code()).isEqualTo("CUSTOMER_REQUIRED_FOR_CREDIT");
        assertThat(stock(product)).isEqualByComparingTo("20");

        long customer = createCustomer();
        req.put("customerId", customer);
        req.put("payments", List.of(Map.of("method", "CASH", "amount", new BigDecimal("40.00"))));
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        assertThat(sale.path("saleType").asText()).isEqualTo("CUSTOMER");
        assertThat(sale.path("totalAmount").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(sale.path("paidAmount").decimalValue()).isEqualByComparingTo("40.00");
        assertThat(sale.path("outstandingAmount").decimalValue()).isEqualByComparingTo("110.00");
        assertThat(get("/api/v1/customers/" + customer).expect(200).body().path("outstandingBalance").decimalValue())
                .isEqualByComparingTo("110.00");

        // Collect 60 against credit; replays with the same key do not double count
        String key = newKey();
        Map<String, Object> collection = Map.of("method", "UPI", "amount", new BigDecimal("60.00"));
        JsonNode collected = post("/api/v1/customers/" + customer + "/payments", collection, key).expect(201).body();
        assertThat(collected.path("outstandingAfter").decimalValue()).isEqualByComparingTo("50.00");
        post("/api/v1/customers/" + customer + "/payments", collection, key).expect(200);
        assertThat(get("/api/v1/customers/" + customer).expect(200).body().path("outstandingBalance").decimalValue())
                .isEqualByComparingTo("50.00");
        assertThat(post("/api/v1/customers/" + customer + "/payments",
                Map.of("method", "CASH", "amount", new BigDecimal("100.00")), newKey()).expect(422).code())
                .isEqualTo("AMOUNT_EXCEEDS_OUTSTANDING");

        // A return on a credit sale first reduces the outstanding balance
        long itemId = sale.path("items").get(0).path("id").asLong();
        JsonNode ret = post("/api/v1/sales/" + sale.path("id").asLong() + "/returns", Map.of("reason", "Excess",
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 1, "saleable", true)))).expect(201).body();
        assertThat(ret.path("creditAdjusted").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(ret.path("cashRefunded").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(get("/api/v1/customers/" + customer).expect(200).body().path("outstandingBalance").decimalValue())
                .isEqualByComparingTo("0.00");

        JsonNode history = get("/api/v1/customers/" + customer + "/history").expect(200).body();
        assertThat(history.path("totalElements").asLong()).isEqualTo(1);
        assertThat(get("/api/v1/customers/" + customer + "/payments").expect(200).body().path("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void splitPaymentRecordsMultiplePayments() throws Exception {
        long product = createProduct("30.00", "50.00", "1", "20");
        Map<String, Object> req = saleRequest("SPLIT_PAYMENT", product, "3");
        req.put("payments", List.of(Map.of("method", "CASH", "amount", new BigDecimal("50.00")),
                Map.of("method", "UPI", "amount", new BigDecimal("100.00"), "reference", "UPI123")));
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        assertThat(sale.path("payments")).hasSize(2);
        assertThat(sale.path("outstandingAmount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(sale.path("saleType").asText()).isEqualTo("DIRECT");

        req.put("payments", List.of(Map.of("method", "CASH", "amount", new BigDecimal("200.00")),
                Map.of("method", "UPI", "amount", new BigDecimal("1.00"))));
        assertThat(post("/api/v1/sales", req, newKey()).expect(422).code()).isEqualTo("OVERPAYMENT");
    }

    @Test
    void cashSaleMustBePaidInFull() throws Exception {
        long product = createProduct("30.00", "50.00", "1", "20");
        Map<String, Object> req = saleRequest("CASH", product, "2");
        req.put("payments", List.of(Map.of("method", "CASH", "amount", new BigDecimal("90.00"))));
        assertThat(post("/api/v1/sales", req, newKey()).expect(422).code()).isEqualTo("PAYMENT_TOTAL_MISMATCH");
    }

    @Test
    void priceOverrideNeedsConfirmationAndRespectsMinimumPrice() throws Exception {
        Map<String, Object> p = productRequest(unique("Biscuit"), "PKT", "8.00", "10.00", "1");
        p.put("minSellingPrice", new BigDecimal("9.00"));
        p.put("openingStock", new BigDecimal("50"));
        p.put("openingStockUnitCost", new BigDecimal("8.00"));
        long product = post("/api/v1/products", p).expect(201).body().path("id").asLong();

        Map<String, Object> req = saleRequest("CASH", product, "2");
        req.put("items", List.of(Map.of("productId", product, "quantity", 2, "unitPrice", new BigDecimal("9.50"))));
        assertThat(post("/api/v1/sales", req, newKey()).expect(422).code()).isEqualTo("PRICE_OVERRIDE_NOT_CONFIRMED");
        req.put("priceOverrideConfirmed", true);
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        assertThat(sale.path("items").get(0).path("priceOverridden").asBoolean()).isTrue();
        assertThat(sale.path("items").get(0).path("listPrice").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(sale.path("totalAmount").decimalValue()).isEqualByComparingTo("19.00");

        req.put("items", List.of(Map.of("productId", product, "quantity", 2, "unitPrice", new BigDecimal("8.50"))));
        assertThat(post("/api/v1/sales", req, newKey()).expect(422).code()).isEqualTo("BELOW_MIN_PRICE");
    }

    @Test
    void billDiscountAndRoundOff() throws Exception {
        long a = createProduct("5.00", "10.50", "1", "20");
        long b = createProduct("5.00", "7.25", "1", "20");
        Map<String, Object> req = saleRequest("CASH", a, "3", b, "2");
        req.put("billDiscount", new BigDecimal("2.00"));
        req.put("roundOff", true);
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        // 31.50 + 14.50 = 46.00 - 2.00 = 44.00
        assertThat(sale.path("subtotal").decimalValue()).isEqualByComparingTo("46.00");
        assertThat(sale.path("taxableAmount").decimalValue()).isEqualByComparingTo("44.00");
        assertThat(sale.path("totalAmount").decimalValue()).isEqualByComparingTo("44.00");
        BigDecimal shares = sale.path("items").get(0).path("billDiscountShare").decimalValue()
                .add(sale.path("items").get(1).path("billDiscountShare").decimalValue());
        assertThat(shares).isEqualByComparingTo("2.00");

        Map<String, Object> odd = saleRequest("CASH", a, "1");
        odd.put("roundOff", true);
        JsonNode rounded = post("/api/v1/sales", odd, newKey()).expect(201).body();
        assertThat(rounded.path("totalAmount").decimalValue()).isEqualByComparingTo("11.00");
        assertThat(rounded.path("roundOff").decimalValue()).isEqualByComparingTo("0.50");
    }

    @Test
    void saleSnapshotsSurviveProductEditsAndVillageIsRecorded() throws Exception {
        long village = post("/api/v1/villages", Map.of("name", unique("Kothapalli"), "state", "Telangana")).expect(201).body().path("id").asLong();
        Map<String, Object> p = productRequest(unique("Tea Powder"), "PKT", "40.00", "55.00", "1");
        p.put("openingStock", new BigDecimal("10"));
        p.put("openingStockUnitCost", new BigDecimal("40.00"));
        JsonNode product = post("/api/v1/products", p).expect(201).body();
        long productId = product.path("id").asLong();

        Map<String, Object> req = saleRequest("UPI", productId, "1");
        req.put("villageId", village);
        req.put("newCustomer", Map.of("name", unique("Lakshmi"), "mobile", "9" + String.format("%09d", System.nanoTime() % 1_000_000_000L)));
        JsonNode sale = post("/api/v1/sales", req, newKey()).expect(201).body();
        assertThat(sale.path("villageId").asLong()).isEqualTo(village);
        assertThat(sale.path("customerId").asLong()).isPositive();
        assertThat(sale.path("payments").get(0).path("method").asText()).isEqualTo("UPI");

        p.put("name", "Renamed " + p.get("name"));
        p.put("sellingPrice", new BigDecimal("60.00"));
        put("/api/v1/products/" + productId, p).expect(200);
        JsonNode again = get("/api/v1/sales/" + sale.path("id").asLong()).expect(200).body();
        assertThat(again.path("items").get(0).path("productName").asText()).isEqualTo(product.path("name").asText());
        assertThat(again.path("items").get(0).path("unitPrice").decimalValue()).isEqualByComparingTo("55.00");

        JsonNode list = get("/api/v1/sales?villageId=" + village).expect(200).body();
        assertThat(list.path("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void inactiveProductsCannotBeSoldAndDuplicateLinesRejected() throws Exception {
        long product = createProduct("10.00", "12.00", "1", "10");
        post("/api/v1/sales", saleRequest("CASH", product, "1", product, "2"), newKey()).expect(400);
        patch("/api/v1/products/" + product + "/status", Map.of("active", false)).expect(200);
        assertThat(sell(product, "1").expect(422).code()).isEqualTo("INACTIVE_PRODUCT");
    }

    @Test
    void fullReturnMarksSaleFullyReturnedAndRefundsCash() throws Exception {
        long product = createProduct("10.00", "15.00", "1", "10");
        JsonNode sale = sell(product, "2").expect(201).body();
        long itemId = sale.path("items").get(0).path("id").asLong();
        JsonNode ret = post("/api/v1/sales/" + sale.path("id").asLong() + "/returns", Map.of("reason", "Wrong item",
                "refundMethod", "CASH", "items", List.of(Map.of("saleItemId", itemId, "quantity", 2, "saleable", true)))).expect(201).body();
        assertThat(ret.path("cashRefunded").decimalValue()).isEqualByComparingTo("30.00");
        JsonNode detail = get("/api/v1/sales/" + sale.path("id").asLong()).expect(200).body();
        assertThat(detail.path("status").asText()).isEqualTo("FULLY_RETURNED");
        assertThat(detail.path("grossProfit").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(stock(product)).isEqualByComparingTo("10");
        post("/api/v1/sales/" + sale.path("id").asLong() + "/returns", Map.of("reason", "again",
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 1, "saleable", true)))).expect(422);
    }
}
