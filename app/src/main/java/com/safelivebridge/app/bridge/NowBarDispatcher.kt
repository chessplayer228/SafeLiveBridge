package com.safelivebridge.app.bridge

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safelivebridge.app.R
import com.safelivebridge.app.SafeLiveBridgeApp
import com.safelivebridge.app.data.ActiveNotificationRepository
import com.safelivebridge.app.data.BridgePreferences
import com.safelivebridge.app.model.ActiveNotificationItem
import com.safelivebridge.app.receiver.OtpCopyReceiver
import com.safelivebridge.app.ui.expander.NotificationExpanderActivity

object NowBarDispatcher {

    private const val TAG = "NowBarDispatcher"
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    fun updateNowBar(context: Context, item: ActiveNotificationItem) {
        val prefs = BridgePreferences.getInstance(context)

        // Проверяем, включен ли мост и разрешено ли приложение
        if (!prefs.isBridgeEnabled.value) return
        if (item.packageName == context.packageName) return
        if (!prefs.isAppAllowed(item.packageName)) return

        val manager = NotificationManagerCompat.from(context)
        val notificationId = getMirroredIdForPackage(item.packageName)

        // Получаем все активные уведомления этого пакета для счетчика
        val allFromPackage = ActiveNotificationRepository.getNotificationsForPackage(item.packageName)
        val count = allFromPackage.size.coerceAtLeast(1)

        val builder = NotificationCompat.Builder(context, SafeLiveBridgeApp.NOW_BAR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_livebar)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setWhen(item.timestamp)

        // Оформление заголовка и текста в стиле Now Bar
        val displayTitle = if (count > 1) "${item.appName} ($count)" else item.appName
        builder.setContentTitle(displayTitle)
        builder.setContentText(if (item.title != item.appName && item.title.isNotBlank()) "${item.title}: ${item.text}" else item.text)

        if (item.subText != null) {
            builder.setSubText(item.subText)
        }

        if (item.largeIconBitmap != null) {
            builder.setLargeIcon(item.largeIconBitmap)
        }

        // Прогресс-бар (для такси, доставки еды, скачивания)
        if (item.progressMax > 0 && item.progress >= 0) {
            builder.setProgress(item.progressMax, item.progress, false)
            builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_STATUS)
        }

        // НАЖАТИЕ НА УВЕДОМЛЕНИЕ В NOW BAR:
        // Раскрытие списка всех уведомлений из этого приложения
        val expandIntent = Intent(context, NotificationExpanderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationExpanderActivity.EXTRA_PACKAGE_NAME, item.packageName)
            putExtra(NotificationExpanderActivity.EXTRA_APP_NAME, item.appName)
        }

        val expandPendingIntent = PendingIntent.getActivity(
            context,
            item.packageName.hashCode(),
            expandIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (prefs.isExpandOnClick.value) {
            builder.setContentIntent(expandPendingIntent)
        } else if (item.contentIntent != null) {
            builder.setContentIntent(item.contentIntent)
        }

        // Добавляем действие "Все уведомления (X)"
        val expandActionTitle = if (count > 1) {
            "${context.getString(R.string.expand_all_action)} ($count)"
        } else {
            context.getString(R.string.expand_all_action)
        }

        builder.addAction(
            R.drawable.ic_stat_livebar,
            expandActionTitle,
            expandPendingIntent
        )

        // Если обнаружен OTP код — добавляем кнопку быстрого копирования в 1 клик
        if (prefs.isOtpEnabled.value && !item.extractedOtp.isNullOrBlank()) {
            val copyIntent = Intent(context, OtpCopyReceiver::class.java).apply {
                putExtra(OtpCopyReceiver.EXTRA_OTP_CODE, item.extractedOtp)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                (item.packageName + "_otp").hashCode(),
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_stat_livebar,
                "${context.getString(R.string.copy_otp_action)}: ${item.extractedOtp}",
                copyPendingIntent
            )
        }

        // Добавляем первое оригинальное действие приложения если есть (например Ответить / Пауза)
        item.actions.firstOrNull()?.let { originalAction ->
            if (originalAction.actionIntent != null) {
                builder.addAction(
                    0,
                    originalAction.title,
                    originalAction.actionIntent
                )
            }
        }

        // Применяем атрибуты Live Updates / Promoted Ongoing для Samsung Now Bar
        applyPromotedOngoingFlags(builder)

        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post Now Bar notification", e)
        }
    }

    fun dismissNowBar(context: Context, packageName: String) {
        val manager = NotificationManagerCompat.from(context)
        val notificationId = getMirroredIdForPackage(packageName)
        manager.cancel(notificationId)
    }

    private fun getMirroredIdForPackage(packageName: String): Int {
        return (packageName.hashCode() and 0x7FFFFFFF) + 10000
    }

    /**
     * Установка флагов Promoted Ongoing для Android 15/16/One UI 7/8/9
     */
    private fun applyPromotedOngoingFlags(builder: NotificationCompat.Builder) {
        // Установка через Bundle extras
        val extras = Bundle().apply {
            putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            putBoolean("requestPromotedOngoing", true)
            putString("sem_notification_type", "now_bar")
        }
        builder.addExtras(extras)

        // Попытка вызвать метод через рефлексию если доступен в AndroidX / Samsung Framework
        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, true)
        } catch (_: NoSuchMethodException) {
            // Игнорируем, если версия androidx.core еще не экспортирует метод напрямую
        } catch (e: Exception) {
            Log.w(TAG, "Reflection invocation of setRequestPromotedOngoing failed", e)
        }
    }
}
