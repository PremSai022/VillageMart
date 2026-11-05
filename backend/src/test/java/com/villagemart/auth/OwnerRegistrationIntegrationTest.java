package com.villagemart.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Runs against a separate, empty database with no bootstrap owner so the one-time registration is open. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:villagemart_registration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;LOCK_TIMEOUT=20000;DB_CLOSE_DELAY=-1",
        "villagemart.owner.username=",
        "villagemart.owner.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerRegistrationIntegrationTest {

    private static final String PASSWORD = "Shop1234Secure";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UserRepository userRepository;

    @Test
    void invalidRegistrationIsRejectedWithFieldErrors() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("fullName", "");
        bad.put("mobileNumber", "12345");
        bad.put("email", "not-an-email");
        bad.put("password", "short");
        bad.put("businessName", "");
        bad.put("acceptTerms", false);
        Result r = call(HttpMethod.POST, "/api/v1/auth/register", bad, null);
        assertThat(r.status()).isEqualTo(400);
        List<String> fields = new ArrayList<>();
        r.body().path("fieldErrors").forEach(f -> fields.add(f.path("field").asText()));
        assertThat(fields).contains("fullName", "mobileNumber", "email", "password", "businessName", "acceptTerms");
    }

    @Test
    void firstOwnerRegistersOnceAndThenRegistrationIsClosed() throws Exception {
        assertThat(call(HttpMethod.GET, "/api/v1/auth/registration-status", null, null).body()
                .path("registrationOpen").asBoolean()).isTrue();

        // Concurrent attempts: exactly one owner may be created.
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String mobile = "981234567" + i;
            Callable<Result> task = () -> call(HttpMethod.POST, "/api/v1/auth/register", registration(mobile), null);
            futures.add(pool.submit(task));
        }
        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) {
            results.add(f.get());
        }
        pool.shutdown();
        List<Result> created = results.stream().filter(r -> r.status() == 201).toList();
        assertThat(created).hasSize(1);
        assertThat(results.stream().filter(r -> r.status() == 403)
                .allMatch(r -> r.body().path("code").asText().equals("REGISTRATION_CLOSED"))).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);

        JsonNode auth = created.get(0).body();
        String mobile = auth.path("user").path("mobile").asText();
        assertThat(auth.path("accessToken").asText()).isNotBlank();
        assertThat(auth.path("refreshToken").asText()).isNotBlank();
        assertThat(auth.path("user").path("fullName").asText()).isEqualTo("Ravi Kumar");
        assertThat(auth.path("user").path("role").asText()).isEqualTo("OWNER");
        assertThat(auth.toString()).doesNotContain("password");

        User saved = userRepository.findByLogin(mobile).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);

        // Registration tokens authorize business APIs, and the business profile was stored.
        String access = auth.path("accessToken").asText();
        JsonNode settings = call(HttpMethod.GET, "/api/v1/settings/business", null, access).body();
        assertThat(settings.path("businessName").asText()).isEqualTo("Ravi Stores");
        assertThat(settings.path("address").asText()).isEqualTo("Main Road, Hyderabad");
        assertThat(call(HttpMethod.GET, "/api/v1/products", null, access).status()).isEqualTo(200);
        assertThat(call(HttpMethod.GET, "/api/v1/products", null, null).status()).isEqualTo(401);

        // Owner can sign in with mobile or email.
        assertThat(call(HttpMethod.POST, "/api/v1/auth/login", Map.of("login", mobile, "password", PASSWORD), null)
                .status()).isEqualTo(200);
        assertThat(call(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("login", "Owner" + mobile + "@Example.com", "password", PASSWORD), null).status()).isEqualTo(200);

        // Registration is now closed, including for the same mobile number.
        assertThat(call(HttpMethod.GET, "/api/v1/auth/registration-status", null, null).body()
                .path("registrationOpen").asBoolean()).isFalse();
        Result again = call(HttpMethod.POST, "/api/v1/auth/register", registration(mobile), null);
        assertThat(again.status()).isEqualTo(403);
        assertThat(again.body().path("code").asText()).isEqualTo("REGISTRATION_CLOSED");
    }

    private static Map<String, Object> registration(String mobile) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fullName", "Ravi Kumar");
        m.put("mobileNumber", mobile);
        m.put("email", "owner" + mobile + "@example.com");
        m.put("password", PASSWORD);
        m.put("businessName", "Ravi Stores");
        m.put("businessAddress", "Main Road, Hyderabad");
        m.put("acceptTerms", true);
        return m;
    }

    private record Result(int status, JsonNode body) {
    }

    private Result call(HttpMethod method, String url, Object body, String bearer) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url).accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        MvcResult result = mvc.perform(b).andReturn();
        String content = result.getResponse().getContentAsString();
        return new Result(result.getResponse().getStatus(),
                content.isBlank() ? json.createObjectNode() : json.readTree(content));
    }
}
