package com.mart.villagemart.data

import android.util.Log
import com.mart.villagemart.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideBaseUrl(): String = normalizeApiBaseUrl(BuildConfig.DEFAULT_API_BASE_URL)

    @Provides
    @Singleton
    fun provideAuthenticator(tokenStore: TokenStore, json: Json, baseUrl: String): Authenticator =
        TokenAuthenticator(tokenStore, json, baseUrl)

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenStore, authenticator: Authenticator): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
        return OkHttpClient.Builder()
            .addInterceptor(AccessTokenInterceptor(tokenStore))
            .authenticator(authenticator)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): VillageMartApi = retrofit.create(VillageMartApi::class.java)
}

internal fun normalizeApiBaseUrl(url: String): String {
    val normalized = url.trimEnd('/')
    return if (normalized.endsWith("/api/v1")) "$normalized/" else "$normalized/api/v1/"
}

class AccessTokenInterceptor(private val tokenStore: TokenStore) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            tokenStore.accessToken()?.let { header("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val json: Json,
    private val baseUrl: String
) : Authenticator {
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val currentAccess = tokenStore.accessToken()
        val requestAccess = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (currentAccess != null && requestAccess != currentAccess) {
            return response.request.newBuilder().header("Authorization", "Bearer $currentAccess").build()
        }
        val refresh = tokenStore.refreshToken() ?: return null
        val payload = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh))
        val request = Request.Builder()
            .url("${baseUrl}auth/refresh")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        try {
            refreshClient.newCall(request).execute().use { result ->
                if (!result.isSuccessful) {
                    if (result.code == 401 || result.code == 403) tokenStore.clear()
                    return null
                }
                val body = result.body.string()
                val auth = json.decodeFromString(AuthResponse.serializer(), body)
                tokenStore.save(auth.accessToken, auth.refreshToken)
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${auth.accessToken}")
                    .build()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Token refresh failed; the session will require sign-in", error)
            if (error !is IOException) tokenStore.clear()
            return null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val TAG = "VillageMartAuth"
    }
}
