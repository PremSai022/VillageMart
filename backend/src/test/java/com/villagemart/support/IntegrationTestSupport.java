package com.villagemart.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);
    private static String cachedToken;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String token;

    @BeforeEach
    void authenticate() throws Exception {
        if (cachedToken == null) {
            cachedToken = login("owner", "Owner@Test123").path("accessToken").asText();
        }
        token = cachedToken;
    }

    protected static String unique(String prefix) {
        return prefix + " " + SEQ.incrementAndGet();
    }

    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    protected JsonNode login(String login, String password) throws Exception {
        Response r = call(HttpMethod.POST, "/api/v1/auth/login", Map.of("login", login, "password", password), null, null);
        assertThat(r.status()).as(r.body().toString()).isEqualTo(200);
        return r.body();
    }

    public record Response(int status, JsonNode body) {
        public Response expect(int expected) {
            assertThat(status).as("HTTP status; body=" + body).isEqualTo(expected);
            return this;
        }

        public String code() {
            return body.path("code").asText();
        }
    }

    protected Response call(HttpMethod method, String url, Object body, String bearer, String idempotencyKey) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url).accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        if (idempotencyKey != null) {
            b.header("Idempotency-Key", idempotencyKey);
        }
        MvcResult result = mvc.perform(b).andReturn();
        String content = result.getResponse().getContentAsString();
        JsonNode node = content.isBlank() ? json.createObjectNode() : json.readTree(content);
        return new Response(result.getResponse().getStatus(), node);
    }

    protected Response get(String url) throws Exception {
        return call(HttpMethod.GET, url, null, token, null);
    }

    protected Response post(String url, Object body) throws Exception {
        return call(HttpMethod.POST, url, body, token, null);
    }

    protected Response post(String url, Object body, String key) throws Exception {
        return call(HttpMethod.POST, url, body, token, key);
    }

    protected Response put(String url, Object body) throws Exception {
        return call(HttpMethod.PUT, url, body, token, null);
    }

    protected Response patch(String url, Object body) throws Exception {
        return call(HttpMethod.PATCH, url, body, token, null);
    }

    protected Response delete(String url) throws Exception {
        return call(HttpMethod.DELETE, url, null, token, null);
    }

    // ---- domain helpers ----

    protected long categoryId(String name) throws Exception {
        for (JsonNode c : get("/api/v1/categories").expect(200).body()) {
            if (c.path("name").asText().equals(name)) {
                return c.path("id").asLong();
            }
        }
        throw new IllegalStateException("Category not found: " + name);
    }

    protected long unitId(String code) throws Exception {
        for (JsonNode u : get("/api/v1/units").expect(200).body()) {
            if (u.path("code").asText().equals(code)) {
                return u.path("id").asLong();
            }
        }
        throw new IllegalStateException("Unit not found: " + code);
    }

    protected Map<String, Object> productRequest(String name, String unit, String purchasePrice, String sellingPrice,
                                                 String minStock) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("categoryId", categoryId("Other"));
        m.put("unitId", unitId(unit));
        m.put("purchasePrice", new BigDecimal(purchasePrice));
        m.put("sellingPrice", new BigDecimal(sellingPrice));
        m.put("minStockQuantity", new BigDecimal(minStock));
        return m;
    }

    /** Creates a product, optionally with opening stock at the given unit cost. */
    protected long createProduct(String purchasePrice, String sellingPrice, String minStock, String openingStock) throws Exception {
        Map<String, Object> m = productRequest(unique("Product"), "PCS", purchasePrice, sellingPrice, minStock);
        if (openingStock != null) {
            m.put("openingStock", new BigDecimal(openingStock));
            m.put("openingStockUnitCost", new BigDecimal(purchasePrice));
        }
        return post("/api/v1/products", m).expect(201).body().path("id").asLong();
    }

    protected long createSupplier() throws Exception {
        return post("/api/v1/suppliers", Map.of("name", unique("Wholesale"))).expect(201).body().path("id").asLong();
    }

    protected long createCustomer() throws Exception {
        return post("/api/v1/customers", Map.of("name", unique("Customer"))).expect(201).body().path("id").asLong();
    }

    protected JsonNode purchase(long supplierId, long productId, String qty, String unitPrice) throws Exception {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("supplierId", supplierId);
        req.put("complete", true);
        req.put("items", List.of(Map.of("productId", productId, "quantity", new BigDecimal(qty),
                "unitPrice", new BigDecimal(unitPrice))));
        return post("/api/v1/purchases", req).expect(201).body();
    }

    protected Map<String, Object> saleRequest(String paymentMode, Object... productQtyPairs) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < productQtyPairs.length; i += 2) {
            items.add(Map.of("productId", productQtyPairs[i], "quantity", new BigDecimal(productQtyPairs[i + 1].toString())));
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("paymentMode", paymentMode);
        req.put("items", items);
        return req;
    }

    protected Response sell(long productId, String qty) throws Exception {
        return post("/api/v1/sales", saleRequest("CASH", productId, qty), newKey());
    }

    protected JsonNode inventory(long productId) throws Exception {
        return get("/api/v1/inventory/" + productId).expect(200).body();
    }

    protected BigDecimal stock(long productId) throws Exception {
        return inventory(productId).path("availableQuantity").decimalValue();
    }

    protected static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
