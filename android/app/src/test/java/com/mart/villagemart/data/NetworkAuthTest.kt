package com.mart.villagemart.data

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkAuthTest {
    @Test
    fun apiBaseUrlAlwaysIncludesVersionedApiPath() {
        assertEquals("https://example.test/api/v1/", normalizeApiBaseUrl("https://example.test/"))
        assertEquals("https://example.test/api/v1/", normalizeApiBaseUrl("https://example.test/api/v1"))
        assertEquals("http://10.0.2.2:8080/api/v1/", normalizeApiBaseUrl("http://10.0.2.2:8080/"))
    }

    @Test
    fun accessTokenInterceptorAddsBearerAuthorization() {
        val tokenStore = mockk<TokenStore>()
        val chain = mockk<Interceptor.Chain>()
        val response = mockk<Response>()
        val request = Request.Builder().url("https://example.test/api/v1/dashboard/summary").build()
        every { tokenStore.accessToken() } returns "access-token"
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns response

        AccessTokenInterceptor(tokenStore).intercept(chain)

        verify {
            chain.proceed(match { it.header("Authorization") == "Bearer access-token" })
        }
    }

    @Test
    fun authenticatorUsesNewerAccessTokenBeforeRefreshing() {
        val tokenStore = mockk<TokenStore>()
        val response = mockk<Response>()
        val request = Request.Builder()
            .url("https://example.test/api/v1/dashboard/summary")
            .header("Authorization", "Bearer expired-token")
            .build()
        every { response.priorResponse } returns null
        every { response.request } returns request
        every { tokenStore.accessToken() } returns "new-access-token"

        val retry = TokenAuthenticator(tokenStore, Json {}, "https://example.test/api/v1/")
            .authenticate(null, response)

        assertEquals("Bearer new-access-token", retry?.header("Authorization"))
        verify(exactly = 0) { tokenStore.refreshToken() }
    }
}
