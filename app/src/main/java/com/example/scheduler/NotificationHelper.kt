package com.example.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.MainActivity
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {
    private const val TAG = "DailyHydraNotification"
    const val CHANNEL_ID = "daily_hydra_reminders_channel_v2"
    private const val CHANNEL_NAME = "Intelligent Hydration Reminders"
    private const val CHANNEL_DESC = "Highly reliable regular reminder system to help you stay hydrated"
    const val NOTIFICATION_ID = 2026

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Attempt to stand out under extreme settings
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created or verified successfully")
        }
    }

    fun showHydrationNotification(context: Context, title: String, message: String) {
        try {
            createNotificationChannel(context)
            
            // Log database update or file tracking for Diagnostics
            saveLastSentNotificationTimestamp(context)

            // Intent to open Main Companion App
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_notification", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent for Drink 250ml Button action
            val drink250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "com.example.ACTION_DRINK_250"
                putExtra("amount", 250)
            }
            val pDrink250 = PendingIntent.getBroadcast(
                context,
                250,
                drink250Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent for Drink 500ml Button action
            val drink500Intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "com.example.ACTION_DRINK_500"
                putExtra("amount", 500)
            }
            val pDrink500 = PendingIntent.getBroadcast(
                context,
                500,
                drink500Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent for Swiping / Dismissing Notification
            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "com.example.ACTION_DISMISS"
            }
            val pDismiss = PendingIntent.getBroadcast(
                context,
                99,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val color = 0xFF2196F3.toInt()

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(color)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pDismiss)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_menu_add, "Drink 250ml", pDrink250)
                .addAction(android.R.drawable.ic_menu_add, "Drink 500ml", pDrink500)
                .addAction(android.R.drawable.ic_menu_view, "Open App", pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            
            Log.d(TAG, "Hydration reminder notification posted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification: ${e.message}", e)
            NotificationLogger.log(context, "failed", "Display system crash: ${e.localizedMessage}")
        }
    }

    private fun saveLastSentNotificationTimestamp(context: Context) {
        val prefs = context.getSharedPreferences("daily_hydra_diagnostics_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_notification_sent_timestamp", System.currentTimeMillis()).apply()
    }

    fun getLastSentNotificationTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences("daily_hydra_diagnostics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_notification_sent_timestamp", 0L)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun isDoNotDisturbActive(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                val filter = manager.currentInterruptionFilter
                filter != NotificationManager.INTERRUPTION_FILTER_ALL
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
