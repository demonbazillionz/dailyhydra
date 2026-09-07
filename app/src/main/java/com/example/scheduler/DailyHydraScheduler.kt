package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.example.data.HydrationDatabase
import com.example.receiver.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DailyHydraScheduler {
    private const val TAG = "DailyHydraScheduler"
    private const val PREFS_NAME = "daily_hydra_scheduler_prefs"
    private const val KEY_NEXT_ALARM_TIME = "next_alarm_time_ms"

    fun scheduleNextReminder(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Calculating next reminder schedule...")
                val db = HydrationDatabase.getDatabase(context)
                val dao = db.hydrationDao()

                // Rule 2: Cancel ALL existing AlarmManager/WorkManager schedules first.
                cancelAlarm(context)
                try {
                    WorkManager.getInstance(context).cancelUniqueWork("daily_hydra_recovery_work")
                    Log.d(TAG, "Cancelled existing WorkManager recovery task.")
                } catch (e: Exception) {
                    Log.e(TAG, "WorkManager cancel error: ${e.message}")
                }

                // Check if reminders are enabled
                val remindersEnabledItem = dao.getSetting("reminders_enabled")
                val isEnabled = remindersEnabledItem?.value?.toBoolean() ?: true
                if (!isEnabled) {
                    Log.d(TAG, "Reminders are disabled. Saving Off state.")
                    saveNextAlarmTimestamp(context, 0L)
                    return@launch
                }

                // Fetch day start, day end, and interval
                val dayStartStr = dao.getSetting("smart_reminder_day_start")?.value ?: "08:00 AM"
                val dayEndStr = dao.getSetting("smart_reminder_day_end")?.value ?: "10:00 PM"
                val intervalMinsItem = dao.getSetting("smart_reminder_interval_mins")
                val intervalMins = intervalMinsItem?.value?.toIntOrNull() ?: 120

                val nowMs = System.currentTimeMillis()

                // Parse start and end times to today
                val startPair = parseTime(dayStartStr)
                val endPair = parseTime(dayEndStr)

                val startCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, startPair.first)
                    set(Calendar.MINUTE, startPair.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, endPair.first)
                    set(Calendar.MINUTE, endPair.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If end is technically before start on the calendar clock, it crosses midnight. Adjust endCal.
                if (endCal.before(startCal)) {
                    endCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Rule 3 & 5: Determine next schedule time
                val finalScheduleMs = when {
                    nowMs < startCal.timeInMillis -> {
                        // Before Day Start Limit -> First notification = Day Start Limit exactly
                        Log.d(TAG, "Current time before Day Start. Scheduling at start exactly.")
                        startCal.timeInMillis
                    }
                    nowMs >= endCal.timeInMillis -> {
                        // After Day End Limit -> Stop scheduling for today, schedule for tomorrow's Day Start Limit
                        Log.d(TAG, "Current time after Day End. Scheduling tomorrow's Day Start.")
                        val tomorrowStart = Calendar.getInstance().apply {
                            timeInMillis = startCal.timeInMillis
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        tomorrowStart.timeInMillis
                    }
                    else -> {
                        // Inside allowed window
                        val candidate = nowMs + (intervalMins * 60 * 1000L)
                        if (candidate > endCal.timeInMillis) {
                            Log.d(TAG, "Candidate time exceeds Day End Limit. Scheduling tomorrow's Day Start.")
                            val tomorrowStart = Calendar.getInstance().apply {
                                timeInMillis = startCal.timeInMillis
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            tomorrowStart.timeInMillis
                        } else {
                            Log.d(TAG, "Scheduling at interval inside allowed window.")
                            candidate
                        }
                    }
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Log.d(TAG, "=== Extensive Scheduler Log ===")
                Log.d(TAG, "Current time: ${sdf.format(Date(nowMs))} ($nowMs ms)")
                Log.d(TAG, "Start time: ${sdf.format(startCal.time)} (${startCal.timeInMillis} ms) [Configured: $dayStartStr]")
                Log.d(TAG, "End time: ${sdf.format(endCal.time)} (${endCal.timeInMillis} ms) [Configured: $dayEndStr]")
                Log.d(TAG, "Calculated next schedule time: ${sdf.format(Date(finalScheduleMs))} ($finalScheduleMs ms)")
                Log.d(TAG, "===============================")

                setAlarm(context, finalScheduleMs)
                saveNextAlarmTimestamp(context, finalScheduleMs)

                // Recreate physical recovery schedule immediately (Rule 2)
                enqueueRecoveryWorker(context)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating reminder: ${e.message}", e)
                NotificationLogger.log(context, "failed", "Calc error: ${e.localizedMessage}")
            }
        }
    }

    private fun setAlarm(context: Context, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    Log.d(TAG, "Scheduled EXACT wake-up alarm successfully")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    Log.d(TAG, "Scheduled ALLOW-WHILE-IDLE wakeup alarm")
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }

            // Log details of the scheduled exact alarm
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerAtMs))
            val minsRemaining = TimeUnit.MILLISECONDS.toMinutes(triggerAtMs - System.currentTimeMillis())
            NotificationLogger.log(context, "scheduled", "Scheduled exact alarm at $timeStr (in $minsRemaining mins).")

        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerAtMs))
            NotificationLogger.log(context, "scheduled", "Fallback non-exact scheduled at $timeStr due to lock constraint.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
            NotificationLogger.log(context, "failed", "Alarm registration fail: ${e.localizedMessage}")
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Active alarm cancelled in OS")
        NotificationLogger.log(context, "scheduled", "Cancelled existing scheduled reminders.")
    }

    private fun enqueueRecoveryWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val recoveryWorkRequest = PeriodicWorkRequestBuilder<RecoveryWorker>(
            15, TimeUnit.MINUTES // High-precision checking every 15 minutes to guarantee recovery
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_hydra_recovery_work",
            ExistingPeriodicWorkPolicy.KEEP,
            recoveryWorkRequest
        )
        Log.d(TAG, "WorkManager recovery check enqueued successfully")
    }

    private fun saveNextAlarmTimestamp(context: Context, timestampMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_NEXT_ALARM_TIME, timestampMs).apply()
        
        val dao = HydrationDatabase.getDatabase(context).hydrationDao()
        CoroutineScope(Dispatchers.IO).launch {
            if (timestampMs > 0L) {
                val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = format.format(Date(timestampMs))
                dao.insertSetting(com.example.data.AppSettingsItem("next_reminder_time", timeStr))
            } else {
                dao.insertSetting(com.example.data.AppSettingsItem("next_reminder_time", "Reminders Off"))
            }
        }
    }

    fun getNextAlarmTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_NEXT_ALARM_TIME, 0L)
    }

    fun isTimeInQuietHours(timestampMs: Long, enabled: Boolean, startStr: String, endStr: String): Boolean {
        if (!enabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val targetMinOfToday = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val start = parseTime(startStr)
        val end = parseTime(endStr)

        val startMin = start.first * 60 + start.second
        val endMin = end.first * 60 + end.second

        return if (startMin < endMin) {
            targetMinOfToday in startMin..endMin
        } else {
            targetMinOfToday >= startMin || targetMinOfToday <= endMin
        }
    }

    fun isTimeInWakeWindow(timestampMs: Long, startStr: String, endStr: String): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val targetMinOfToday = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val start = parseTime(startStr)
        val end = parseTime(endStr)

        val startMin = start.first * 60 + start.second
        val endMin = end.first * 60 + end.second

        return if (startMin < endMin) {
            targetMinOfToday in startMin..endMin
        } else {
            targetMinOfToday >= startMin || targetMinOfToday <= endMin
        }
    }

    fun parseTime(timeStr: String): Pair<Int, Int> {
        return try {
            val clean = timeStr.trim().uppercase()
            if (clean.contains("AM") || clean.contains("PM")) {
                val format = SimpleDateFormat("hh:mm a", Locale.US)
                val date = format.parse(clean) ?: return Pair(8, 0)
                val cal = Calendar.getInstance().apply { time = date }
                Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else {
                val parts = clean.split(":")
                val h = parts[0].toIntOrNull() ?: 8
                val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                Pair(h, m)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeStr", e)
            Pair(8, 0)
        }
    }
}

class RecoveryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.d("DailyHydraRecoveryWorker", "Verification checks executing...")
        try {
            val db = HydrationDatabase.getDatabase(context)
            val dao = db.hydrationDao()

            val remindersEnabledItem = dao.getSetting("reminders_enabled")
            val isEnabled = remindersEnabledItem?.value?.toBoolean() ?: true
            if (!isEnabled) {
                return Result.success()
            }

            val nextAlarm = DailyHydraScheduler.getNextAlarmTimestamp(context)
            val nowMs = System.currentTimeMillis()

            if (nextAlarm > 0L && nowMs > (nextAlarm + 15 * 60 * 1000L)) {
                // Late/missed scheduled alarm! Safe automatic recovery fallback!
                val scheduledStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(nextAlarm))
                NotificationLogger.log(context, "failed", "Missed scheduled alarm at $scheduledStr. Fallback triggered.")
                
                triggerRecoveryNotification(context, dao)
                DailyHydraScheduler.scheduleNextReminder(context)
            } else if (nextAlarm == 0L) {
                // Expected exact alarm but timestamp is flat
                NotificationLogger.log(context, "failed", "No registered exact alarm. Healing states.")
                DailyHydraScheduler.scheduleNextReminder(context)
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("DailyHydraRecoveryWorker", "Self-recovery failed: ${e.message}", e)
            return Result.retry()
        }
    }

    private suspend fun triggerRecoveryNotification(context: Context, dao: com.example.data.HydrationDao) {
        try {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val entries = dao.getIntakeEntriesBetween(startOfToday, startOfToday + 24 * 60 * 60 * 1000L - 1).first()
            val totalIntakeVal = entries.sumOf { it.amountMl }
            val dailyGoalVal = dao.getSetting("daily_goal")?.value?.toIntOrNull() ?: 2500
            val unit = dao.getSetting("goal_unit")?.value ?: "ml"

            val allEntries = dao.getAllIntakeEntries().first()
            val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val entriesByDay = allEntries.groupBy { formatter.format(Date(it.timestamp)) }
            val metGoalDaysSet = entriesByDay.filter { (_, dayEntries) -> dayEntries.sumOf { it.amountMl } >= dailyGoalVal }.keys

            var currentStreak = 0
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val containsToday = metGoalDaysSet.contains(sdf.format(Date()))
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val containsYesterday = metGoalDaysSet.contains(sdf.format(yesterdayCal.time))

            if (containsToday || containsYesterday) {
                val testCal = Calendar.getInstance()
                if (!containsToday && containsYesterday) {
                    testCal.add(Calendar.DAY_OF_YEAR, -1)
                }
                while (true) {
                    val dayStr = sdf.format(testCal.time)
                    if (metGoalDaysSet.contains(dayStr)) {
                        currentStreak++
                        testCal.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            }

            val percentage = if (dailyGoalVal > 0) (totalIntakeVal * 100) / dailyGoalVal else 0
            val remainingMl = maxOf(0, dailyGoalVal - totalIntakeVal)

            // Check if we should receive notifications after goal completion
            val receiveNotifsAfterGoal = dao.getSetting("notifs_after_goal_completion")?.value?.toBoolean() ?: true
            if (!receiveNotifsAfterGoal && totalIntakeVal >= dailyGoalVal) {
                Log.d("RecoveryWorker", "Goal reached and 'Receive Notifications After Goal Completion' is disabled. Skipping recovery notification.")
                NotificationLogger.log(context, "blocked", "Recovery skipped: Goal met and notifications after goal are disabled.")
                return
            }

            val title = "Time to Hydrate 💧 (Missed Recovery)"
            val curStr = if (unit == "L" || totalIntakeVal >= 1000) String.format("%.1f L", totalIntakeVal / 1000f) else "$totalIntakeVal ml"
            val maxStr = if (unit == "L" || dailyGoalVal >= 1000) String.format("%.1f L", dailyGoalVal / 1000f) else "$dailyGoalVal ml"
            val body = "Progress: $percentage% • Consumed: $curStr / $maxStr\nStreak: $currentStreak Days • Restored background reminder"

            NotificationHelper.showHydrationNotification(context, title, body)
            NotificationLogger.log(context, "triggered", "Recovery trigger displayed backup alarm successfully.")
        } catch (ex: Exception) {
            Log.e("RecoveryWorker", "Error posting recovery layout: ${ex.message}", ex)
        }
    }
}
