package com.villagemart.purchase;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseIntegrationTest extends IntegrationTestSupport {

    @Test
    void landedCostAllocatesChargesAndDiscountsIntoWeightedAverage() throws Exception {
        long supplier = createSupplier();
        long a = createProduct("100.00", "130.00", "2", null);
        long b = createProduct("25.00", "35.00", "2", null);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("supplierId", supplier);
        req.put("supplierInvoiceNumber", "INV-" + System.nanoTime());
        req.put("transportCharges", new BigDecimal("50.00"));
        req.put("otherCharges", new BigDecimal("20.00"));
        req.put("complete", true);
        req.put("items", List.of(
                Map.of("productId", a, "quantity", 10, "unitPrice", new BigDecimal("100.00")),
                Map.of("productId", b, "quantity", 20, "unitPrice", new BigDecimal("25.00"), "discountAmount", new BigDecimal("100.00"))));
        JsonNode p = post("/api/v1/purchases", req).expect(201).body();
        assertThat(p.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(p.path("subtotal").decimalValue()).isEqualByComparingTo("1500.00");
        assertThat(p.path("discountTotal").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(p.path("totalAmount").decimalValue()).isEqualByComparingTo("1470.00");
        // Charges of 70 split by line value 1000:400 -> 50 and 20
        assertThat(p.path("items").get(0).path("landedUnitCost").decimalValue()).isEqualByComparingTo("105.0000");
        assertThat(p.path("items").get(1).path("landedUnitCost").decimalValue()).isEqualByComparingTo("21.0000");
        assertThat(inventory(a).path("averageCost").decimalValue()).isEqualByComparingTo("105.0000");
        assertThat(inventory(b).path("averageCost").decimalValue()).isEqualByComparingTo("21.0000");
        assertThat(stock(b)).isEqualByComparingTo("20");

        // Same supplier invoice cannot be entered twice
        assertThat(post("/api/v1/purchases", req).expect(409).code()).isEqualTo("DUPLICATE_SUPPLIER_INVOICE");
    }

    @Test
    void draftDoesNotAffectStockUntilCompleted() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "12.00", "1", null);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("supplierId", supplier);
        req.put("items", List.of(Map.of("productId", product, "quantity", 8, "unitPrice", new BigDecimal("10.00"))));
        JsonNode draft = post("/api/v1/purchases", req).expect(201).body();
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(stock(product)).isEqualByComparingTo("0");

        long id = draft.path("id").asLong();
        post("/api/v1/purchases/" + id + "/complete", null).expect(200);
        assertThat(stock(product)).isEqualByComparingTo("8");
        post("/api/v1/purchases/" + id + "/complete", null).expect(422);
    }

    @Test
    void cancellationReversesStockButNeverBelowZero() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "12.00", "1", null);
        long purchaseId = purchase(supplier, product, "10", "10.00").path("id").asLong();
        sell(product, "7").expect(201);

        Response blocked = post("/api/v1/purchases/" + purchaseId + "/cancel", Map.of("reason", "Wrong entry")).expect(422);
        assertThat(blocked.code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(stock(product)).isEqualByComparingTo("3");

        long second = purchase(supplier, product, "5", "10.00").path("id").asLong();
        JsonNode cancelled = post("/api/v1/purchases/" + second + "/cancel", Map.of("reason", "Duplicate entry")).expect(200).body();
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(product)).isEqualByComparingTo("3");
        // Completed purchases are never physically deleted
        get("/api/v1/purchases/" + second).expect(200);
    }

    @Test
    void purchaseReturnReducesStockAndIsBoundedByReturnableQuantity() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "12.00", "1", null);
        JsonNode p = purchase(supplier, product, "10", "10.00");
        long itemId = p.path("items").get(0).path("id").asLong();
        long id = p.path("id").asLong();

        JsonNode ret = post("/api/v1/purchases/" + id + "/returns", Map.of("reason", "Damaged in transit",
                "items", List.of(Map.of("purchaseItemId", itemId, "quantity", 3)))).expect(201).body();
        assertThat(ret.path("totalAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(stock(product)).isEqualByComparingTo("7");

        post("/api/v1/purchases/" + id + "/returns", Map.of("reason", "More",
                "items", List.of(Map.of("purchaseItemId", itemId, "quantity", 8)))).expect(422);
        assertThat(stock(product)).isEqualByComparingTo("7");
    }

    @Test
    void futureDatesAndEmptyItemsAreRejected() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "12.00", "1", null);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("supplierId", supplier);
        req.put("purchaseDate", java.time.LocalDate.now().plusDays(5).toString());
        req.put("items", List.of(Map.of("productId", product, "quantity", 1, "unitPrice", 1)));
        post("/api/v1/purchases", req).expect(400);
        post("/api/v1/purchases", Map.of("supplierId", supplier, "items", List.of())).expect(400);
    }
}
