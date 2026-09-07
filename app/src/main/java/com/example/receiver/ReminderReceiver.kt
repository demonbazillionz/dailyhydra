package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.HydrationDatabase
import com.example.data.IntakeEntry
import com.example.data.StreakManager
import com.example.data.StreakData
import com.example.scheduler.DailyHydraScheduler
import com.example.scheduler.NotificationHelper
import com.example.scheduler.NotificationLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    private val TAG = "DailyHydra_ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm broadcast triggered! Processing event...")
        
        // Acquire WakeLock to hold CPU alive while querying the Room DB
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DailyHydra::NotificationProcessorWakeLock"
        )
        wakeLock.acquire(4200L) // Safe limit of 4.2 seconds

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HydrationDatabase.getDatabase(context)
                val dao = db.hydrationDao()

                // Check again if reminders are still enabled
                val enabled = dao.getSetting("reminders_enabled")?.value?.toBoolean() ?: true
                if (!enabled) {
                    Log.d(TAG, "Reminders toggled off. Skipping delivery.")
                    NotificationLogger.log(context, "failed", "Skipped delivery: Reminders are disabled.")
                    wakeLock.releaseSafe()
                    return@launch
                }

                val nowMs = System.currentTimeMillis()
                val dayStartStr = dao.getSetting("smart_reminder_day_start")?.value ?: "08:00 AM"
                val dayEndStr = dao.getSetting("smart_reminder_day_end")?.value ?: "10:00 PM"

                // Rule 4: Validate current time is within allowed window. Block if outside.
                val inWakeWindow = DailyHydraScheduler.isTimeInWakeWindow(nowMs, dayStartStr, dayEndStr)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                if (!inWakeWindow) {
                    val reason = "Current time ${sdf.format(Date(nowMs))} is outside allowed window [$dayStartStr to $dayEndStr]"
                    Log.w(TAG, "BLOCKED: $reason")
                    NotificationLogger.log(context, "blocked", "Notification BLOCKED: $reason (Start: $dayStartStr, End: $dayEndStr)")
                    
                    // Reschedule for next day / start time of next window
                    DailyHydraScheduler.scheduleNextReminder(context)
                    wakeLock.releaseSafe()
                    return@launch
                }

                // Gather stats for personalized rich notifications
                val startOfToday = getStartOfTodayMs()
                val entries = dao.getIntakeEntriesBetween(startOfToday, startOfToday + 24 * 60 * 60 * 1000L - 1).first()
                val totalIntakeVal = entries.sumOf { it.amountMl }
                
                val dailyGoalVal = dao.getSetting("daily_goal")?.value?.toIntOrNull() ?: 2500
                val unit = dao.getSetting("goal_unit")?.value ?: "ml"

                // Fetch streak calculation
                val allEntries = dao.getAllIntakeEntries().first()
                val streakInfo = StreakManager.calculateStreaks(allEntries, dailyGoalVal)
                val streakDays = streakInfo.currentStreak

                val percentage = if (dailyGoalVal > 0) (totalIntakeVal * 100) / dailyGoalVal else 0
                val remainingMl = maxOf(0, dailyGoalVal - totalIntakeVal)

                // Check if we should receive notifications after goal completion
                val receiveNotifsAfterGoal = dao.getSetting("notifs_after_goal_completion")?.value?.toBoolean() ?: true
                if (!receiveNotifsAfterGoal && totalIntakeVal >= dailyGoalVal) {
                    Log.d(TAG, "Goal reached and 'Receive Notifications After Goal Completion' is disabled. Skipping delivery.")
                    NotificationLogger.log(context, "blocked", "Skipped delivery: Goal is met and notifications after goal are disabled.")
                    // Automatically queue the subsequent exact reminder matching interval forwards
                    DailyHydraScheduler.scheduleNextReminder(context)
                    wakeLock.releaseSafe()
                    return@launch
                }

                // Generate smart, dynamic title & body
                val title = getSmartNotificationTitle(percentage, remainingMl)
                val body = getSmartNotificationBody(percentage, totalIntakeVal, dailyGoalVal, remainingMl, streakDays, unit)

                NotificationLogger.log(
                    context, 
                    "triggered", 
                    "Triggered reminder: '$title'. Goal is: $dailyGoalVal$unit, Progress remains: $remainingMl$unit. Current time: ${sdf.format(Date(nowMs))} (Start: $dayStartStr, End: $dayEndStr)"
                )
                NotificationHelper.showHydrationNotification(context, title, body)

                // Automatically queue the subsequent exact reminder matching interval forwards
                DailyHydraScheduler.scheduleNextReminder(context)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process reminder flow: ${e.message}", e)
                NotificationLogger.log(context, "failed", "Service loop error: ${e.localizedMessage}")
            } finally {
                wakeLock.releaseSafe()
            }
        }
    }

    private fun getSmartNotificationTitle(pct: Int, remainingMl: Int): String {
        return when {
            pct == 0 -> "Time to Hydrate 💧"
            pct < 100 -> {
                // Sequentially or pseudo-randomly switch between progress percentage or goal remaining text
                if (System.currentTimeMillis() % 2 == 0L) {
                    "You're $pct% Hydrated Today"
                } else {
                    "Only ${remainingMl}ml Left to Reach Today's Goal"
                }
            }
            else -> "Goal achieved! 🏆 You'RE 100% Hydrated!"
        }
    }

    private fun getSmartNotificationBody(pct: Int, total: Int, goal: Int, remaining: Int, streakDays: Int, unit: String): String {
        val curStr = if (unit == "L" || total >= 1000) String.format("%.1f L", total / 1000f) else "$total ml"
        val maxStr = if (unit == "L" || goal >= 1000) String.format("%.1f L", goal / 1000f) else "$goal ml"
        val remStr = if (unit == "L" || remaining >= 1000) String.format("%.1f L", remaining / 1000f) else "$remaining ml"

        return "Progress: $pct% • Consumed: $curStr / $maxStr\nRemaining: $remStr • Current Streak: $streakDays Days"
    }

    private fun getStartOfTodayMs(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun PowerManager.WakeLock.releaseSafe() {
        try {
            if (isHeld) {
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }
}
