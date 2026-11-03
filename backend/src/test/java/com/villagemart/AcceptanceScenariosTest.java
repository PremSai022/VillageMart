package com.villagemart;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The acceptance scenarios from the master specification (section 26), executed end-to-end through the REST API. */
class AcceptanceScenariosTest extends IntegrationTestSupport {

    @Test
    void scenarios1to6_stockLifecycleAndStatusTransitions() throws Exception {
        long supplier = createSupplier();
        // Scenario 1: opening stock 20 + purchase 50 = 70
        long product = createProduct("10.00", "15.00", "10", "20");
        assertThat(stock(product)).isEqualByComparingTo("20");
        purchase(supplier, product, "50", "10.00");
        assertThat(stock(product)).isEqualByComparingTo("70");

        // Scenario 2: sell 15 -> 55
        sell(product, "15").expect(201);
        assertThat(stock(product)).isEqualByComparingTo("55");

        // Scenario 3: min 10, available 5 -> LOW_STOCK
        sell(product, "50").expect(201);
        JsonNode inv = inventory(product);
        assertThat(inv.path("availableQuantity").decimalValue()).isEqualByComparingTo("5");
        assertThat(inv.path("stockStatus").asText()).isEqualTo("LOW_STOCK");
        assertThat(ids(get("/api/v1/inventory/low-stock").expect(200).body())).contains(product);

        // Scenario 4: sell remaining 5 -> 0, OUT_OF_STOCK
        sell(product, "5").expect(201);
        inv = inventory(product);
        assertThat(inv.path("availableQuantity").decimalValue()).isEqualByComparingTo("0");
        assertThat(inv.path("stockStatus").asText()).isEqualTo("OUT_OF_STOCK");
        assertThat(ids(get("/api/v1/inventory/out-of-stock").expect(200).body())).contains(product);

        // Scenario 5: purchase 20 -> 20, NORMAL
        purchase(supplier, product, "20", "10.00");
        inv = inventory(product);
        assertThat(inv.path("availableQuantity").decimalValue()).isEqualByComparingTo("20");
        assertThat(inv.path("stockStatus").asText()).isEqualTo("NORMAL");

        // Scenario 6: selling 25 with stock 20 is rejected, stock unchanged
        Response rejected = sell(product, "25").expect(422);
        assertThat(rejected.code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(rejected.body().path("message").asText()).contains("20");
        assertThat(stock(product)).isEqualByComparingTo("20");

        // Every change is audited
        JsonNode movements = get("/api/v1/inventory/" + product + "/movements?size=50").expect(200).body().path("content");
        assertThat(movements).hasSize(6);
    }

    @Test
    void scenario7_sameIdempotencyKeyCreatesOneInvoiceAndOneDeduction() throws Exception {
        long product = createProduct("10.00", "15.00", "2", "20");
        String key = newKey();
        Map<String, Object> req = saleRequest("CASH", product, "4");
        JsonNode first = post("/api/v1/sales", req, key).expect(201).body();
        JsonNode second = post("/api/v1/sales", req, key).expect(200).body();
        assertThat(second.path("id").asLong()).isEqualTo(first.path("id").asLong());
        assertThat(second.path("invoiceNumber").asText()).isEqualTo(first.path("invoiceNumber").asText());
        assertThat(second.path("replayed").asBoolean()).isTrue();
        assertThat(stock(product)).isEqualByComparingTo("16");
        assertThat(first.path("payments")).hasSize(1);

        // Same key with a different payload is a conflict, never a second sale
        Response conflict = post("/api/v1/sales", saleRequest("CASH", product, "5"), key).expect(409);
        assertThat(conflict.code()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(stock(product)).isEqualByComparingTo("16");

        // Missing key is rejected
        post("/api/v1/sales", req, null).expect(400);
    }

    @Test
    void scenario8_cancelRestoresStockAndReversesPayments() throws Exception {
        long product = createProduct("10.00", "15.00", "2", "20");
        JsonNode sale = sell(product, "6").expect(201).body();
        assertThat(stock(product)).isEqualByComparingTo("14");
        long saleId = sale.path("id").asLong();

        JsonNode cancelled = post("/api/v1/sales/" + saleId + "/cancel", Map.of("reason", "Customer changed mind")).expect(200).body();
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(product)).isEqualByComparingTo("20");
        JsonNode payments = cancelled.path("payments");
        assertThat(payments).hasSize(2);
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (JsonNode p : payments) {
            if (p.path("direction").asText().equals("IN")) {
                in = in.add(p.path("amount").decimalValue());
                assertThat(p.path("reversed").asBoolean()).isTrue();
            } else {
                out = out.add(p.path("amount").decimalValue());
                assertThat(p.path("paymentType").asText()).isEqualTo("CANCELLATION_REFUND");
            }
        }
        assertThat(in).isEqualByComparingTo("90.00");
        assertThat(out).isEqualByComparingTo(in);
        assertThat(cancelled.path("grossProfit").decimalValue()).isEqualByComparingTo("0");

        post("/api/v1/sales/" + saleId + "/cancel", Map.of("reason", "again")).expect(409);
    }

    @Test
    void scenario9_damagedReturnDoesNotIncreaseStock() throws Exception {
        long product = createProduct("10.00", "15.00", "2", "20");
        JsonNode sale = sell(product, "5").expect(201).body();
        long saleId = sale.path("id").asLong();
        long itemId = sale.path("items").get(0).path("id").asLong();

        JsonNode damaged = post("/api/v1/sales/" + saleId + "/returns", Map.of("reason", "Packet torn",
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 2, "saleable", false)))).expect(201).body();
        assertThat(damaged.path("refundAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(damaged.path("costReversed").decimalValue()).isEqualByComparingTo("0");
        assertThat(stock(product)).isEqualByComparingTo("15");

        post("/api/v1/sales/" + saleId + "/returns", Map.of("reason", "Not needed",
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 1, "saleable", true)))).expect(201);
        assertThat(stock(product)).isEqualByComparingTo("16");

        // Cannot return more than remaining returnable quantity (2 left)
        Response tooMany = post("/api/v1/sales/" + saleId + "/returns", Map.of("reason", "x",
                "items", List.of(Map.of("saleItemId", itemId, "quantity", 3, "saleable", true)))).expect(422);
        assertThat(tooMany.code()).isEqualTo("RETURN_EXCEEDS_SOLD");

        JsonNode detail = get("/api/v1/sales/" + saleId).expect(200).body();
        assertThat(detail.path("status").asText()).isEqualTo("PARTIALLY_RETURNED");
        assertThat(detail.path("returnedAmount").decimalValue()).isEqualByComparingTo("45.00");
        // Revenue 75 - 45 returned = 30; cost 50 - 10 (only the saleable unit) = 40 -> gross profit -10 (damage loss)
        assertThat(detail.path("grossProfit").decimalValue()).isEqualByComparingTo("-10.00");
        // Cancelling a sale with returns is not allowed
        post("/api/v1/sales/" + saleId + "/cancel", Map.of("reason", "x")).expect(422);
    }

    @Test
    void scenario10_weightedAverageCostAndProfit() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "20.00", "5", "20");
        purchase(supplier, product, "50", "13.00");
        // ((20 x 10) + (50 x 13)) / 70 = 12.142857 -> 12.1429
        assertThat(inventory(product).path("averageCost").decimalValue()).isEqualByComparingTo("12.1429");

        JsonNode sale = sell(product, "15").expect(201).body();
        assertThat(sale.path("totalAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(sale.path("items").get(0).path("unitCost").decimalValue()).isEqualByComparingTo("12.1429");
        assertThat(sale.path("costTotal").decimalValue()).isEqualByComparingTo("182.14");
        assertThat(sale.path("grossProfit").decimalValue()).isEqualByComparingTo("117.86");
        // Selling does not change the weighted average cost
        assertThat(inventory(product).path("averageCost").decimalValue()).isEqualByComparingTo("12.1429");
    }

    private static List<Long> ids(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false).map(n -> n.path("productId").asLong()).toList();
    }
}
