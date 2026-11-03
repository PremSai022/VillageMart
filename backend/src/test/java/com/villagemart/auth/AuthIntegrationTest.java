package com.villagemart.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void loginWithUsernameOrMobileReturnsTokens() throws Exception {
        JsonNode byUsername = login("owner", "Owner@Test123");
        assertThat(byUsername.path("accessToken").asText()).isNotBlank();
        assertThat(byUsername.path("refreshToken").asText()).isNotBlank();
        assertThat(byUsername.path("user").path("role").asText()).isEqualTo("OWNER");
        assertThat(byUsername.path("user").has("passwordHash")).isFalse();
        JsonNode byMobile = login("9876543210", "Owner@Test123");
        assertThat(byMobile.path("user").path("username").asText()).isEqualTo("owner");
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        Response r = call(HttpMethod.POST, "/api/v1/auth/login", Map.of("login", "owner", "password", "wrong-password"), null, null);
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    void protectedEndpointsRequireAValidToken() throws Exception {
        assertThat(call(HttpMethod.GET, "/api/v1/products", null, null, null).status()).isEqualTo(401);
        assertThat(call(HttpMethod.GET, "/api/v1/products", null, "not-a-jwt", null).status()).isEqualTo(401);
        assertThat(call(HttpMethod.GET, "/api/v1/products", null, token, null).status()).isEqualTo(200);
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        assertThat(call(HttpMethod.GET, "/actuator/health", null, null, null).status()).isEqualTo(200);
    }

    @Test
    void noPublicRegistrationEndpoint() throws Exception {
        Response r = call(HttpMethod.POST, "/api/v1/auth/register", Map.of("login", "x", "password", "y"), null, null);
        assertThat(r.status()).isIn(401, 403, 404);
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesIt() throws Exception {
        JsonNode tokens = login("owner", "Owner@Test123");
        String refresh = tokens.path("refreshToken").asText();

        Response refreshed = call(HttpMethod.POST, "/api/v1/auth/refresh", Map.of("refreshToken", refresh), null, null).expect(200);
        String rotated = refreshed.body().path("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(refresh);
        // The old refresh token cannot be reused after rotation
        assertThat(call(HttpMethod.POST, "/api/v1/auth/refresh", Map.of("refreshToken", refresh), null, null).status()).isEqualTo(401);

        call(HttpMethod.POST, "/api/v1/auth/logout", Map.of("refreshToken", rotated),
                refreshed.body().path("accessToken").asText(), null);
        assertThat(call(HttpMethod.POST, "/api/v1/auth/refresh", Map.of("refreshToken", rotated), null, null).status()).isEqualTo(401);
    }

    @Test
    void loginValidationErrorsAreReported() throws Exception {
        Response r = call(HttpMethod.POST, "/api/v1/auth/login", Map.of("login", ""), null, null).expect(400);
        assertThat(r.body().path("fieldErrors").isArray()).isTrue();
        assertThat(r.body().path("fieldErrors").size()).isGreaterThanOrEqualTo(1);
    }
}
