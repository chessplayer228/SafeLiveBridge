package com.safelivebridge.app.model

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Icon

data class NotificationActionItem(
    val title: CharSequence,
    val actionIntent: PendingIntent?,
    val icon: Icon? = null
)

data class ActiveNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String? = null,
    val progress: Int = -1,
    val progressMax: Int = -1,
    val isOngoing: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val smallIcon: Icon? = null,
    val largeIconBitmap: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
    val deleteIntent: PendingIntent? = null,
    val actions: List<NotificationActionItem> = emptyList(),
    val extractedOtp: String? = null
)
