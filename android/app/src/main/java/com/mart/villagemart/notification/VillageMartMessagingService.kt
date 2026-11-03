package com.mart.villagemart.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mart.villagemart.MainActivity
import com.mart.villagemart.R
import com.mart.villagemart.data.DeviceRegistrationRequest
import com.mart.villagemart.data.TokenStore
import com.mart.villagemart.data.VillageMartApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class VillageMartMessagingService : FirebaseMessagingService() {
    @Inject lateinit var deviceTokenStore: DeviceTokenStore
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var api: VillageMartApi

    override fun onNewToken(token: String) {
        deviceTokenStore.save(token)
        if (tokenStore.accessToken() != null) {
            runBlocking {
                try {
                    api.registerDevice(DeviceRegistrationRequest(token))
                } catch (error: Exception) {
                    Log.w(TAG, "FCM token registration will be retried after sign-in", error)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        ensureChannel(this)
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["message"] ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            message.messageId?.hashCode() ?: body.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(this)
                .notify(message.messageId?.hashCode() ?: body.hashCode(), notification)
        } catch (error: SecurityException) {
            Log.i(TAG, "Notification permission is not granted", error)
        }
    }

    companion object {
        const val CHANNEL_ID = "villagemart_stock_alerts"
        private const val TAG = "VillageMartFCM"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.stock_alert_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.stock_alert_channel_description)
                }
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }
    }
}
