package com.safelivebridge.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.safelivebridge.app.bridge.NotificationParser
import com.safelivebridge.app.bridge.NowBarDispatcher
import com.safelivebridge.app.data.ActiveNotificationRepository
import com.safelivebridge.app.data.BridgePreferences

class LiveBridgeNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LiveBridgeListener"
        var isConnected: Boolean = false
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        ActiveNotificationRepository.listenerService = this
        Log.d(TAG, "Notification listener connected successfully")

        // Загружаем текущие активные уведомления при старте
        try {
            val active = activeNotifications
            if (active != null) {
                for (sbn in active) {
                    processNotification(sbn)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying initial active notifications", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        ActiveNotificationRepository.listenerService = null
        Log.w(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        ActiveNotificationRepository.remove(packageName, sbn.key)

        // Проверяем, остались ли еще уведомления от этого пакета
        val remaining = ActiveNotificationRepository.getNotificationsForPackage(packageName)
        if (remaining.isEmpty()) {
            NowBarDispatcher.dismissNowBar(applicationContext, packageName)
        } else {
            // Обновляем плашку Now Bar последним актуальным уведомлением
            NowBarDispatcher.updateNowBar(applicationContext, remaining.first())
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        val prefs = BridgePreferences.getInstance(applicationContext)
        if (!prefs.isAppAllowed(packageName)) return

        val parsedItem = NotificationParser.parse(applicationContext, sbn)
        ActiveNotificationRepository.addOrUpdate(parsedItem)

        // Отправляем в Now Bar
        NowBarDispatcher.updateNowBar(applicationContext, parsedItem)
    }
}
