package com.mart.villagemart.data

import android.util.Log
import com.mart.villagemart.BuildConfig
import com.mart.villagemart.notification.DeviceTokenStore
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Owner authentication against the VillageMart backend; tokens are persisted encrypted in [TokenStore]. */
@Singleton
class AuthRepository @Inject constructor(
    private val api: VillageMartApi,
    private val tokenStore: TokenStore,
    private val deviceTokenStore: DeviceTokenStore
) {
    fun hasSession(): Boolean = tokenStore.accessToken() != null

    suspend fun login(login: String, password: String): AuthResponse =
        startSession(api.login(LoginRequest(login.trim(), password)))

    suspend fun register(request: RegisterRequest): AuthResponse = startSession(api.register(request))

    suspend fun registrationOpen(): Boolean = api.registrationStatus().registrationOpen

    suspend fun me(): UserDto = api.me()

    suspend fun logout() {
        val refresh = tokenStore.refreshToken()
        try {
            if (refresh != null) api.logout(LogoutRequest(refresh))
        } finally {
            tokenStore.clear()
        }
    }

    fun clearSession() = tokenStore.clear()

    private suspend fun startSession(response: AuthResponse): AuthResponse {
        tokenStore.save(response.accessToken, response.refreshToken)
        val pushToken = if (BuildConfig.FIREBASE_CONFIGURED) deviceTokenStore.get() else null
        if (pushToken != null) {
            try {
                api.registerDevice(DeviceRegistrationRequest(pushToken))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Device token registration failed; dashboard notifications remain available", error)
            }
        }
        return response
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
