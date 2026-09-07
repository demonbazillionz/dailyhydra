package com.example.receiver

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.data.HydrationDatabase
import com.example.data.IntakeEntry
import com.example.scheduler.DailyHydraScheduler
import com.example.scheduler.NotificationLogger
import com.example.widget.HydrationWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    private val TAG = "NotificationAction"

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Notification action received: $action")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            "com.example.ACTION_DRINK_250" -> {
                // Cancel notification right away
                notificationManager.cancel(com.example.scheduler.NotificationHelper.NOTIFICATION_ID)
                logWaterIntake(context, 250)
            }
            "com.example.ACTION_DRINK_500" -> {
                // Cancel notification right away
                notificationManager.cancel(com.example.scheduler.NotificationHelper.NOTIFICATION_ID)
                logWaterIntake(context, 500)
            }
            "com.example.ACTION_DISMISS" -> {
                NotificationLogger.log(context, "dismissed", "Notification swiped/dismissed by user.")
            }
        }
    }

    private fun logWaterIntake(context: Context, amount: Int) {
        NotificationLogger.log(context, "triggered", "User logged ${amount}ml via notification action button.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HydrationDatabase.getDatabase(context)
                val dao = db.hydrationDao()

                // Insert water intake entry
                val entry = IntakeEntry(amountMl = amount, timestamp = System.currentTimeMillis())
                dao.insertIntakeEntry(entry)

                // Push new dynamic notification reschedule forwards
                DailyHydraScheduler.scheduleNextReminder(context)

                // Force widget status updates
                val widgetIntent = Intent(context, HydrationWidgetProvider::class.java).apply {
                    setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, HydrationWidgetProvider::class.java)
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(widgetIntent)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Logged ${amount}ml of water! 💧", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed logging action from intent: ${e.message}", e)
                NotificationLogger.log(context, "failed", "Failed button logging: ${e.localizedMessage}")
            }
        }
    }
}
