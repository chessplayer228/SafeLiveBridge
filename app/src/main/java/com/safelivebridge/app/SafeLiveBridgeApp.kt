package com.safelivebridge.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class SafeLiveBridgeApp : Application() {

    companion object {
        const val NOW_BAR_CHANNEL_ID = "safe_livebridge_now_bar"
        const val SERVICE_CHANNEL_ID = "safe_livebridge_service"
        lateinit var instance: SafeLiveBridgeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Канал для Now Bar живых уведомлений (высокий приоритет, отображение на экране блокировки)
            val nowBarChannel = NotificationChannel(
                NOW_BAR_CHANNEL_ID,
                getString(R.string.now_bar_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.now_bar_channel_desc)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                setShowBadge(true)
            }

            // Канал для фонового сервиса поддержания активности
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.foreground_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.foreground_service_desc)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(nowBarChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}
