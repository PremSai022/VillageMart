package com.mart.villagemart.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("push_registration", Context.MODE_PRIVATE)

    fun save(token: String) {
        preferences.edit().putString(KEY, token).apply()
    }

    fun get(): String? = preferences.getString(KEY, null)

    private companion object {
        const val KEY = "fcm_token"
    }
}
