package com.villagemart.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LowStockAlertIntegrationTest extends IntegrationTestSupport {

    @Test
    void alertsAreTransitionBasedAndDeduplicatedPerEpisode() throws Exception {
        long supplier = createSupplier();
        long product = createProduct("10.00", "15.00", "10", "15");
        assertThat(alerts(product)).isEmpty();

        sell(product, "6").expect(201); // 9 -> LOW
        List<JsonNode> a = alerts(product);
        assertThat(a).hasSize(1);
        assertThat(a.get(0).path("type").asText()).isEqualTo("LOW_STOCK");
        assertThat(a.get(0).path("alertActive").asBoolean()).isTrue();
        assertThat(a.get(0).path("pushStatus").asText()).isIn("SKIPPED", "PENDING");

        sell(product, "1").expect(201); // still LOW -> no duplicate
        assertThat(alerts(product)).hasSize(1);

        purchase(supplier, product, "20", "10.00"); // 28 -> NORMAL, alert cleared
        a = alerts(product);
        assertThat(a).hasSize(1);
        assertThat(a.get(0).path("alertActive").asBoolean()).isFalse();
        assertThat(activeAlertProductIds()).doesNotContain(product);

        sell(product, "20").expect(201); // 8 -> new LOW episode
        a = alerts(product);
        assertThat(a).hasSize(2);
        assertThat(a.stream().filter(n -> n.path("alertActive").asBoolean()).count()).isEqualTo(1);

        sell(product, "8").expect(201); // 0 -> OUT_OF_STOCK, LOW resolved
        a = alerts(product);
        assertThat(a).hasSize(3);
        JsonNode latest = a.get(0);
        assertThat(latest.path("type").asText()).isEqualTo("OUT_OF_STOCK");
        assertThat(latest.path("alertActive").asBoolean()).isTrue();
        assertThat(a.stream().filter(n -> n.path("alertActive").asBoolean()).count()).isEqualTo(1);
        assertThat(activeAlertProductIds()).contains(product);

        // Read/unread handling
        long before = get("/api/v1/notifications/unread-count").expect(200).body().path("count").asLong();
        JsonNode read = patch("/api/v1/notifications/" + latest.path("id").asLong() + "/read", null).expect(200).body();
        assertThat(read.path("read").asBoolean()).isTrue();
        long after = get("/api/v1/notifications/unread-count").expect(200).body().path("count").asLong();
        assertThat(after).isEqualTo(before - 1);
    }

    @Test
    void lowStockEndpointsReportSuggestedReorderQuantity() throws Exception {
        long product = createProduct("10.00", "15.00", "10", "4");
        JsonNode low = null;
        for (JsonNode n : get("/api/v1/inventory/low-stock").expect(200).body()) {
            if (n.path("productId").asLong() == product) {
                low = n;
            }
        }
        assertThat(low).isNotNull();
        assertThat(low.path("minStockQuantity").decimalValue()).isEqualByComparingTo("10");
        assertThat(low.path("suggestedReorderQuantity").decimalValue()).isEqualByComparingTo("16");
        JsonNode summary = get("/api/v1/inventory/summary").expect(200).body();
        assertThat(summary.path("lowStockCount").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void manualAdjustmentsRequireReasonAndAreAudited() throws Exception {
        long product = createProduct("10.00", "15.00", "2", "10");
        post("/api/v1/inventory/adjustments", Map.of("productId", product, "type", "DAMAGE", "quantity", 2)).expect(400);
        JsonNode adjusted = post("/api/v1/inventory/adjustments", Map.of("productId", product, "type", "DAMAGE",
                "quantity", 2, "reason", "Rat damage")).expect(201).body();
        assertThat(adjusted.path("availableQuantity").decimalValue()).isEqualByComparingTo("8");
        assertThat(post("/api/v1/inventory/adjustments", Map.of("productId", product, "type", "MANUAL_ADJUSTMENT",
                "direction", "DECREASE", "quantity", 50, "reason", "Count")).expect(422).code()).isEqualTo("INSUFFICIENT_STOCK");
        post("/api/v1/inventory/adjustments", Map.of("productId", product, "type", "SALE",
                "quantity", 1, "reason", "x")).expect(400);
        JsonNode movement = get("/api/v1/inventory/" + product + "/movements").expect(200).body().path("content").get(0);
        assertThat(movement.path("movementType").asText()).isEqualTo("DAMAGE");
        assertThat(movement.path("reason").asText()).isEqualTo("Rat damage");
        assertThat(movement.path("previousQuantity").decimalValue()).isEqualByComparingTo("10");
        assertThat(movement.path("newQuantity").decimalValue()).isEqualByComparingTo("8");
    }

    private List<JsonNode> alerts(long productId) throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode n : get("/api/v1/notifications?size=100").expect(200).body().path("content")) {
            if (n.path("productId").asLong() == productId) {
                result.add(n);
            }
        }
        return result;
    }

    private List<Long> activeAlertProductIds() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (JsonNode n : get("/api/v1/notifications/active-alerts").expect(200).body()) {
            ids.add(n.path("productId").asLong());
        }
        return ids;
    }
}
