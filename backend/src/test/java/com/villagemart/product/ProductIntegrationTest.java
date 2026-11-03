package com.villagemart.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductIntegrationTest extends IntegrationTestSupport {

    @Test
    void createReadUpdateAndToggleStatus() throws Exception {
        String name = unique("Sunflower Oil");
        Map<String, Object> req = productRequest(name, "BTL", "120.00", "140.00", "10");
        req.put("barcode", "890" + System.nanoTime() % 1_000_000_000L);
        req.put("minSellingPrice", new BigDecimal("130.00"));
        JsonNode created = post("/api/v1/products", req).expect(201).body();
        long id = created.path("id").asLong();
        assertThat(created.path("sku").asText()).startsWith("VM");
        assertThat(created.path("stockStatus").asText()).isEqualTo("OUT_OF_STOCK");
        assertThat(created.path("availableQuantity").decimalValue()).isEqualByComparingTo("0");

        req.put("sellingPrice", new BigDecimal("145.00"));
        JsonNode updated = put("/api/v1/products/" + id, req).expect(200).body();
        assertThat(updated.path("sellingPrice").decimalValue()).isEqualByComparingTo("145.00");

        JsonNode inactive = patch("/api/v1/products/" + id + "/status", Map.of("active", false)).expect(200).body();
        assertThat(inactive.path("active").asBoolean()).isFalse();

        JsonNode search = get("/api/v1/products?q=" + name).expect(200).body();
        assertThat(search.path("totalElements").asLong()).isEqualTo(1);
        JsonNode byBarcode = get("/api/v1/products/barcode/" + req.get("barcode")).expect(200).body();
        assertThat(byBarcode.path("id").asLong()).isEqualTo(id);
    }

    @Test
    void duplicateSkuAndBarcodeAreRejected() throws Exception {
        String sku = "DUP-" + System.nanoTime() % 1_000_000;
        Map<String, Object> a = productRequest(unique("Soap"), "PCS", "20", "25", "5");
        a.put("sku", sku);
        a.put("barcode", "B" + System.nanoTime() % 1_000_000_000L);
        post("/api/v1/products", a).expect(201);

        Map<String, Object> b = productRequest(unique("Soap"), "PCS", "20", "25", "5");
        b.put("sku", sku.toLowerCase());
        assertThat(post("/api/v1/products", b).expect(409).code()).isEqualTo("DUPLICATE_SKU");

        Map<String, Object> c = productRequest(unique("Soap"), "PCS", "20", "25", "5");
        c.put("barcode", a.get("barcode"));
        assertThat(post("/api/v1/products", c).expect(409).code()).isEqualTo("DUPLICATE_BARCODE");
    }

    @Test
    void validationRejectsBadInput() throws Exception {
        Map<String, Object> req = productRequest("", "PCS", "-1", "0", "-5");
        Response r = post("/api/v1/products", req).expect(400);
        assertThat(r.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(r.body().path("fieldErrors").size()).isGreaterThanOrEqualTo(3);

        Map<String, Object> minAbove = productRequest(unique("Rice"), "KG", "40", "50", "10");
        minAbove.put("minSellingPrice", new BigDecimal("60"));
        post("/api/v1/products", minAbove).expect(422);

        Map<String, Object> fractionalPieces = productRequest(unique("Comb"), "PCS", "5", "10", "1");
        fractionalPieces.put("openingStock", new BigDecimal("1.5"));
        post("/api/v1/products", fractionalPieces).expect(422);
    }

    @Test
    void decimalQuantitiesAllowedForWeightUnits() throws Exception {
        Map<String, Object> req = productRequest(unique("Toor Dal"), "KG", "100", "120", "5");
        req.put("openingStock", new BigDecimal("12.750"));
        req.put("openingStockUnitCost", new BigDecimal("100"));
        JsonNode p = post("/api/v1/products", req).expect(201).body();
        assertThat(p.path("availableQuantity").decimalValue()).isEqualByComparingTo("12.750");
    }

    @Test
    void onlyUnusedProductsCanBeDeleted() throws Exception {
        long unused = post("/api/v1/products", productRequest(unique("Unused"), "PCS", "1", "2", "0")).expect(201).body().path("id").asLong();
        delete("/api/v1/products/" + unused).expect(204);
        get("/api/v1/products/" + unused).expect(404);

        long used = createProduct("10", "12", "1", "5");
        assertThat(delete("/api/v1/products/" + used).expect(409).code()).isEqualTo("PRODUCT_IN_USE");
    }

    @Test
    void notFoundReturnsConsistentError() throws Exception {
        Response r = get("/api/v1/products/999999").expect(404);
        assertThat(r.code()).isEqualTo("NOT_FOUND");
        assertThat(r.body().path("message").asText()).isNotBlank();
    }
}
