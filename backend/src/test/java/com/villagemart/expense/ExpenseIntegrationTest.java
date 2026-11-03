package com.villagemart.expense;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseIntegrationTest extends IntegrationTestSupport {

    @Test
    void expenseLifecycleWithSoftDelete() throws Exception {
        long fuel = 0;
        for (JsonNode c : get("/api/v1/expenses/categories").expect(200).body()) {
            if (c.path("name").asText().equals("Fuel")) {
                fuel = c.path("id").asLong();
            }
        }
        assertThat(fuel).isPositive();
        String date = LocalDate.now().minusDays(400).toString();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("categoryId", fuel);
        req.put("amount", new BigDecimal("450.50"));
        req.put("expenseDate", date);
        req.put("description", "Diesel");
        JsonNode created = post("/api/v1/expenses", req).expect(201).body();
        long id = created.path("id").asLong();
        assertThat(created.path("categoryName").asText()).isEqualTo("Fuel");

        req.put("amount", new BigDecimal("500.00"));
        assertThat(put("/api/v1/expenses/" + id, req).expect(200).body().path("amount").decimalValue()).isEqualByComparingTo("500.00");

        JsonNode list = get("/api/v1/expenses?from=" + date + "&to=" + date).expect(200).body();
        assertThat(list.path("totalElements").asLong()).isEqualTo(1);

        delete("/api/v1/expenses/" + id).expect(204);
        get("/api/v1/expenses/" + id).expect(404);
        assertThat(get("/api/v1/expenses?from=" + date + "&to=" + date).expect(200).body().path("totalElements").asLong()).isZero();
    }

    @Test
    void expenseValidation() throws Exception {
        post("/api/v1/expenses", Map.of("amount", 0)).expect(400);
        long categoryId = get("/api/v1/expenses/categories").expect(200).body().get(0).path("id").asLong();
        Map<String, Object> future = Map.of("categoryId", categoryId, "amount", 10,
                "expenseDate", LocalDate.now().plusDays(3).toString());
        assertThat(post("/api/v1/expenses", future).expect(422).code()).isEqualTo("FUTURE_DATE");
    }

    @Test
    void customCategoriesAreUnique() throws Exception {
        String name = unique("Toll");
        post("/api/v1/expenses/categories", Map.of("name", name)).expect(201);
        assertThat(post("/api/v1/expenses/categories", Map.of("name", name.toUpperCase())).expect(409).code())
                .isEqualTo("DUPLICATE_CATEGORY");
    }
}
