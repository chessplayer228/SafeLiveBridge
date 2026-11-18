package com.safelivebridge.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safelivebridge.app.R
import com.safelivebridge.app.SafeLiveBridgeApp

class KeepAliveForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, SafeLiveBridgeApp.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_livebar)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Мост уведомлений Now Bar активен")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
