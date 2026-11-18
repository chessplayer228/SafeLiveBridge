package com.safelivebridge.app.bridge

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.safelivebridge.app.model.ActiveNotificationItem
import com.safelivebridge.app.model.NotificationActionItem

object NotificationParser {

    fun parse(context: Context, sbn: StatusBarNotification): ActiveNotificationItem {
        val n = sbn.notification
        val extras = n.extras

        val packageName = sbn.packageName
        val packageManager = context.packageManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: appName

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: ""

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)

        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0

        // Извлечение действий (actions)
        val actions = mutableListOf<NotificationActionItem>()
        n.actions?.forEach { action ->
            if (action.title != null) {
                actions.add(
                    NotificationActionItem(
                        title = action.title,
                        actionIntent = action.actionIntent,
                        icon = action.getIcon()
                    )
                )
            }
        }

        // Попытка извлечь крупную иконку или иконку приложения
        val largeIconBitmap: Bitmap? = try {
            n.getLargeIcon()?.loadDrawable(context)?.toBitmap()
                ?: packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            null
        }

        // Извлечение OTP если возможно
        val fullTextForOtp = "$title $text $subText"
        val otpCode = OtpExtractor.extractOtp(fullTextForOtp)

        return ActiveNotificationItem(
            key = sbn.key,
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            progress = progress,
            progressMax = progressMax,
            isOngoing = isOngoing,
            timestamp = sbn.postTime,
            smallIcon = n.smallIcon,
            largeIconBitmap = largeIconBitmap,
            contentIntent = n.contentIntent,
            deleteIntent = n.deleteIntent,
            actions = actions,
            extractedOtp = otpCode
        )
    }
}
