package com.safelivebridge.app.data

import com.safelivebridge.app.model.ActiveNotificationItem
import com.safelivebridge.app.service.LiveBridgeNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object ActiveNotificationRepository {

    // Хранилище: packageName -> Map<key, ActiveNotificationItem>
    private val notificationsByPackage = ConcurrentHashMap<String, ConcurrentHashMap<String, ActiveNotificationItem>>()

    private val _activeNotifications = MutableStateFlow<Map<String, List<ActiveNotificationItem>>>(emptyMap())
    val activeNotifications: StateFlow<Map<String, List<ActiveNotificationItem>>> = _activeNotifications.asStateFlow()

    // Ссылка на активный listener для возможности закрывать уведомления из интерфейса
    var listenerService: LiveBridgeNotificationListenerService? = null

    fun addOrUpdate(item: ActiveNotificationItem) {
        val packageMap = notificationsByPackage.computeIfAbsent(item.packageName) { ConcurrentHashMap() }
        packageMap[item.key] = item
        updateFlow()
    }

    fun remove(packageName: String, key: String) {
        val packageMap = notificationsByPackage[packageName] ?: return
        packageMap.remove(key)
        if (packageMap.isEmpty()) {
            notificationsByPackage.remove(packageName)
        }
        updateFlow()
    }

    fun removeByKey(key: String) {
        for ((pkg, map) in notificationsByPackage) {
            if (map.containsKey(key)) {
                map.remove(key)
                if (map.isEmpty()) {
                    notificationsByPackage.remove(pkg)
                }
                break
            }
        }
        updateFlow()
    }

    fun getNotificationsForPackage(packageName: String): List<ActiveNotificationItem> {
        return notificationsByPackage[packageName]?.values?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    fun dismissNotification(key: String) {
        listenerService?.cancelNotification(key)
        removeByKey(key)
    }

    fun dismissAllForPackage(packageName: String) {
        val items = notificationsByPackage[packageName]?.values?.toList() ?: return
        items.forEach { item ->
            listenerService?.cancelNotification(item.key)
        }
        notificationsByPackage.remove(packageName)
        updateFlow()
    }

    fun clearAll() {
        notificationsByPackage.clear()
        updateFlow()
    }

    private fun updateFlow() {
        val snapshot = notificationsByPackage.mapValues { entry ->
            entry.value.values.sortedByDescending { it.timestamp }
        }
        _activeNotifications.value = snapshot
    }
}
