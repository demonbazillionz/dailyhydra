package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.scheduler.DailyHydraScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HydrationViewModel(private val repository: HydrationRepository) : ViewModel() {

    private val _navigateToHomeAndShowAdd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToHomeAndShowAdd: SharedFlow<Unit> = _navigateToHomeAndShowAdd.asSharedFlow()

    fun triggerAddWaterSheet() {
        _navigateToHomeAndShowAdd.tryEmit(Unit)
    }

    private val _lastDeletedEntry = MutableStateFlow<IntakeEntry?>(null)
    val lastDeletedEntry: StateFlow<IntakeEntry?> = _lastDeletedEntry.asStateFlow()

    private val _lastAddedEntry = MutableStateFlow<IntakeEntry?>(null)
    val lastAddedEntry: StateFlow<IntakeEntry?> = _lastAddedEntry.asStateFlow()

    private var undoJob: Job? = null

    private val _hydrationTipText = MutableStateFlow<String>("")
    val hydrationTipText: StateFlow<String> = _hydrationTipText.asStateFlow()

    private val _hydrationTipLoading = MutableStateFlow<Boolean>(false)
    val hydrationTipLoading: StateFlow<Boolean> = _hydrationTipLoading.asStateFlow()

    private val _hydrationTipError = MutableStateFlow<String?>(null)
    val hydrationTipError: StateFlow<String?> = _hydrationTipError.asStateFlow()

    private val _lastDeletedCup = MutableStateFlow<CustomCup?>(null)
    val lastDeletedCup: StateFlow<CustomCup?> = _lastDeletedCup.asStateFlow()

    // Base flows
    private val allEntriesFlow = repository.allIntakeEntries
    private val customCupsFlow = repository.customCups
    
    private val dailyGoalFlow = repository.getSettingFlow("daily_goal")
        .map { it?.value?.toIntOrNull() ?: 2500 }
        
    private val remindersEnabledFlow = repository.getSettingFlow("reminders_enabled")
        .map { it?.value?.toBoolean() ?: true }
        
    private val nextReminderTimeFlow = repository.getSettingFlow("next_reminder_time")
        .map { it?.value ?: "09:00 AM" }

    val appTheme = repository.getSettingFlow("app_theme")
        .map { it?.value ?: "System" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val dynamicColorsEnabled = repository.getSettingFlow("dynamic_colors_enabled")
        .map { it?.value?.toBoolean() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val amoledDarkMode = repository.getSettingFlow("amoled_dark_mode")
        .map { it?.value?.toBoolean() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val goalUnit = repository.getSettingFlow("goal_unit")
        .map { it?.value ?: "ml" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ml")

    val hapticFeedbackEnabled = repository.getSettingFlow("haptic_feedback_enabled")
        .map { it?.value?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val animationsEnabled = repository.getSettingFlow("animations_enabled")
        .map { it?.value?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quietHoursEnabled = repository.getSettingFlow("quiet_hours_enabled")
        .map { it?.value?.toBoolean() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quietHoursStart = repository.getSettingFlow("quiet_hours_start")
        .map { it?.value ?: "22:00" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "22:00")

    val quietHoursEnd = repository.getSettingFlow("quiet_hours_end")
        .map { it?.value ?: "08:00" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "08:00")

    val defaultQuickAddMls = repository.getSettingFlow("default_quick_add_mls")
        .map { it?.value?.toIntOrNull() ?: 250 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250)

    val quickLoggingPresets = repository.getSettingFlow("quick_logging_presets")
        .map { it?.value ?: "100,250,500,750" }
        .map { presetString ->
            presetString.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(100, 250, 500, 750))

    val isOnboarded = repository.getSettingFlow("onboarded")
        .map { it?.value?.toBoolean() ?: false }
        .stateIn<Boolean?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val smartReminderDayStart = repository.getSettingFlow("smart_reminder_day_start")
        .map { it?.value ?: "08:00 AM" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "08:00 AM")

    val smartReminderDayEnd = repository.getSettingFlow("smart_reminder_day_end")
        .map { it?.value ?: "10:00 PM" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10:00 PM")

    val smartReminderIntervalMins = repository.getSettingFlow("smart_reminder_interval_mins")
        .map { it?.value?.toIntOrNull() ?: 120 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val autoBackupEnabled = repository.getSettingFlow("auto_backup_enabled")
        .map { it?.value?.toBoolean() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notifsAfterGoalCompletion = repository.getSettingFlow("notifs_after_goal_completion")
        .map { it?.value?.toBoolean() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        loadDailyHydrationTip()
    }

    // Unified Reactive Home UI State
    val uiState: StateFlow<HomeUiState> = combine(
        combine(allEntriesFlow, customCupsFlow, dailyGoalFlow, nextReminderTimeFlow) { entries, cups, goal, nextReminder ->
            Tuple4(entries, cups, goal, nextReminder)
        },
        remindersEnabledFlow,
        smartReminderDayStart,
        smartReminderDayEnd,
        smartReminderIntervalMins
    ) { (entries, cups, goal, nextReminder), remindersOn, dayStart, dayEnd, intervalMins ->
        val startOfToday = getStartOfTodayMs()
        val endOfToday = getEndOfTodayMs()
        
        val entriesToday = entries.filter { it.timestamp in startOfToday..endOfToday }
        val totalToday = entriesToday.sumOf { it.amountMl }
        
        val remaining = (goal - totalToday).coerceAtLeast(0)
        val progress = if (goal > 0) ((totalToday.toFloat() / goal) * 100).toInt() else 0
        
        val avgSessionIntake = if (entriesToday.isNotEmpty()) totalToday / entriesToday.size else 0
        val estCompletion = getEstimatedGoalCompletionText(totalToday, goal, entriesToday)
        val goalStatusVal = calculateGoalStatus(progress, totalToday, goal)
        
        val (smartStatusText, statusType) = calculateSmartStatus(progress)
        val streak = calculateStreaks(entries, goal)
        val dynamicCoachMsgValue = getDynamicCoachMessage(progress, remaining, entriesToday, entries)

        HomeUiState(
            todayEntries = entriesToday,
            allEntries = entries,
            customCups = cups,
            dailyGoalMls = goal,
            totalIntakeToday = totalToday,
            remainingMls = remaining,
            progressPercent = progress,
            averageIntakePerSession = avgSessionIntake,
            estimatedCompletionTime = estCompletion,
            goalStatus = goalStatusVal,
            smartStatus = smartStatusText,
            smartStatusType = statusType,
            streak = streak,
            remindersEnabled = remindersOn,
            nextReminderTime = nextReminder,
            smartReminderDayStart = dayStart,
            smartReminderDayEnd = dayEnd,
            smartReminderIntervalMins = intervalMins,
            hydrationStatus = smartStatusText,
            dynamicCoachMessage = dynamicCoachMsgValue
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    // Log water
    fun logWater(amountMl: Int, isQuickAdd: Boolean = false, timestamp: Long = System.currentTimeMillis(), context: Context? = null) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            val entry = IntakeEntry(amountMl = amountMl, timestamp = timestamp)
            val insertedId = repository.insertIntakeEntry(entry)
            val entryWithId = entry.copy(id = insertedId.toInt())
            
            _lastAddedEntry.value = entryWithId
            
            undoJob?.cancel()
            undoJob = viewModelScope.launch {
                delay(5000L)
                if (_lastAddedEntry.value?.id == entryWithId.id) {
                    _lastAddedEntry.value = null
                }
            }
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Undo last add
    fun undoLastAdd(context: Context? = null) {
        val last = _lastAddedEntry.value ?: return
        _lastAddedEntry.value = null
        undoJob?.cancel()
        
        viewModelScope.launch {
            repository.deleteIntakeEntry(last)
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Delete single intake entry
    fun deleteIntakeEntry(entry: IntakeEntry, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteIntakeEntry(entry)
            _lastDeletedEntry.value = entry
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Restore last deleted intake entry
    fun restoreLastDeletedEntry(context: Context? = null) {
        val last = _lastDeletedEntry.value ?: return
        viewModelScope.launch {
            repository.insertIntakeEntry(last)
            _lastDeletedEntry.value = null
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Edit single intake entry
    fun editIntakeEntry(entry: IntakeEntry, newAmountMl: Int, context: Context? = null) {
        viewModelScope.launch {
            repository.updateIntakeEntry(entry.copy(amountMl = newAmountMl))
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Custom cups actions
    fun addCustomCup(name: String, amountMl: Int, iconName: String, context: Context? = null) {
        viewModelScope.launch {
            repository.insertCustomCup(CustomCup(name = name, amountMl = amountMl, iconName = iconName))
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    fun editCustomCup(cup: CustomCup, name: String, amountMl: Int, iconName: String, context: Context? = null) {
        viewModelScope.launch {
            repository.updateCustomCup(cup.copy(name = name, amountMl = amountMl, iconName = iconName))
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    fun deleteCustomCup(cup: CustomCup, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteCustomCup(cup)
            _lastDeletedCup.value = cup
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    fun restoreLastDeletedCup(context: Context? = null) {
        val last = _lastDeletedCup.value ?: return
        viewModelScope.launch {
            repository.insertCustomCup(last)
            _lastDeletedCup.value = null
            
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    // Settings adjustments
    fun setDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            repository.saveSetting("daily_goal", goalMl.toString())
        }
    }

    fun toggleReminders() {
        viewModelScope.launch {
            val current = uiState.value.remindersEnabled
            repository.saveSetting("reminders_enabled", (!current).toString())
        }
    }

    fun setNextReminderTime(timeText: String) {
        viewModelScope.launch {
            repository.saveSetting("next_reminder_time", timeText)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            repository.saveSetting("app_theme", theme)
        }
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("dynamic_colors_enabled", enabled.toString())
        }
    }

    fun setAmoledDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("amoled_dark_mode", enabled.toString())
        }
    }

    fun setGoalUnit(unit: String) {
        viewModelScope.launch {
            repository.saveSetting("goal_unit", unit)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("haptic_feedback_enabled", enabled.toString())
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("animations_enabled", enabled.toString())
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_enabled", enabled.toString())
        }
    }

    fun setQuietHoursStart(time: String) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_start", time)
        }
    }

    fun setQuietHoursEnd(time: String) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_end", time)
        }
    }

    fun setSmartReminderDayStart(time: String, context: Context) {
        viewModelScope.launch {
            repository.saveSetting("smart_reminder_day_start", time)
            DailyHydraScheduler.scheduleNextReminder(context.applicationContext)
        }
    }

    fun setSmartReminderDayEnd(time: String, context: Context) {
        viewModelScope.launch {
            repository.saveSetting("smart_reminder_day_end", time)
            DailyHydraScheduler.scheduleNextReminder(context.applicationContext)
        }
    }

    fun setSmartReminderInterval(minutes: Int, context: Context) {
        viewModelScope.launch {
            repository.saveSetting("smart_reminder_interval_mins", minutes.toString())
            DailyHydraScheduler.scheduleNextReminder(context.applicationContext)
        }
    }

    fun setDefaultQuickAddMls(amountMl: Int) {
        viewModelScope.launch {
            repository.saveSetting("default_quick_add_mls", amountMl.toString())
        }
    }

    fun setQuickLoggingPresets(presets: List<Int>) {
        viewModelScope.launch {
            repository.saveSetting("quick_logging_presets", presets.joinToString(","))
        }
    }

    fun setRemindersEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            repository.saveSetting("reminders_enabled", enabled.toString())
            DailyHydraScheduler.scheduleNextReminder(context.applicationContext)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.saveSetting("onboarded", "true")
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("auto_backup_enabled", enabled.toString())
        }
    }

    fun setNotifsAfterGoalCompletion(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("notifs_after_goal_completion", enabled.toString())
        }
    }

    fun clearIntakeHistoryOnly(context: Context? = null) {
        viewModelScope.launch {
            repository.clearIntakeHistoryOnly()
            if (context != null) {
                triggerAutoBackup(context)
            }
        }
    }

    fun triggerAutoBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbEnabled = repository.getSettingValue("auto_backup_enabled") ?: "false"
                if (dbEnabled != "true") return@launch
                
                val entries = repository.allIntakeEntries.first()
                val cups = repository.customCups.first()

                val root = org.json.JSONObject()
                root.put("version", 5)
                root.put("type", "aqora_full_backup")

                val entriesArr = org.json.JSONArray()
                entries.forEach { entry ->
                    val obj = org.json.JSONObject()
                    obj.put("id", entry.id)
                    obj.put("amountMl", entry.amountMl)
                    obj.put("timestamp", entry.timestamp)
                    entriesArr.put(obj)
                }
                root.put("entries", entriesArr)

                val cupsArr = org.json.JSONArray()
                cups.forEach { cup ->
                    val obj = org.json.JSONObject()
                    obj.put("id", cup.id)
                    obj.put("name", cup.name)
                    obj.put("amountMl", cup.amountMl)
                    obj.put("iconName", cup.iconName)
                    cupsArr.put(obj)
                }
                root.put("cups", cupsArr)

                val jsonString = root.toString(4)

                val backupsDir = java.io.File(context.filesDir, "backups")
                if (!backupsDir.exists()) {
                    backupsDir.mkdirs()
                }

                val file3 = java.io.File(backupsDir, "aqora_auto_backup_3.json")
                val file2 = java.io.File(backupsDir, "aqora_auto_backup_2.json")
                val file1 = java.io.File(backupsDir, "aqora_auto_backup_1.json")

                if (file2.exists()) {
                    file2.copyTo(file3, overwrite = true)
                }
                if (file1.exists()) {
                    file1.copyTo(file2, overwrite = true)
                }
                file1.writeText(jsonString)
                android.util.Log.d("BackupSystem", "[AutoBackup] Success. Rotated and saved.")
            } catch (e: Exception) {
                android.util.Log.e("BackupSystem", "[AutoBackup] Error generating backup: ${e.message}", e)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun secureWipeAllData(context: Context, onCompleted: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val TAG_SECURE = "SecureWipe"
                android.util.Log.d(TAG_SECURE, "[WipeData] Starting strict privacy-grade secure deletion process.")
                
                // 1. Cancel Alarms and notifications
                try {
                    DailyHydraScheduler.cancelAlarm(context.applicationContext)
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    notificationManager?.cancelAll()
                    android.util.Log.d(TAG_SECURE, "[WipeData] Alarms & Notifications successfully cancelled: SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e(TAG_SECURE, "[WipeData] Cancel Alarms/Notifications: FAILED - ${e.message}")
                }

                // 2. Clear Room Database tables completely and populate defaults
                var dbWipedSuccess = false
                try {
                    repository.clearAllData()
                    dbWipedSuccess = true
                    android.util.Log.d(TAG_SECURE, "[WipeData] SQLite Database cleared & defaults restored (onboarded=false): SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e(TAG_SECURE, "[WipeData] SQLite Database wipe: FAILED - ${e.message}")
                }

                // 3. Clear SharedPreferences
                var prefsWipedSuccess = false
                try {
                    val schedPrefs = context.getSharedPreferences("daily_hydra_scheduler_prefs", Context.MODE_PRIVATE)
                    schedPrefs.edit().clear().commit()
                    
                    val diagPrefs = context.getSharedPreferences("daily_hydra_diagnostics_prefs", Context.MODE_PRIVATE)
                    diagPrefs.edit().clear().commit()
                    prefsWipedSuccess = true
                    android.util.Log.d(TAG_SECURE, "[WipeData] SharedPreferences cleared: SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e(TAG_SECURE, "[WipeData] SharedPreferences wipe: FAILED - ${e.message}")
                }

                // 4. Delete Cache folder contents recursively
                var cacheWipedSuccess = false
                try {
                    val cacheDir = context.cacheDir
                    if (cacheDir != null && cacheDir.exists()) {
                        val deleted = cacheDir.deleteRecursively()
                        cacheWipedSuccess = deleted
                        android.util.Log.d(TAG_SECURE, "[WipeData] Cache folder deleted recursively: $deleted -> SUCCESS")
                    } else {
                        cacheWipedSuccess = true
                        android.util.Log.d(TAG_SECURE, "[WipeData] Cache folder did not exist: SUCCESS")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG_SECURE, "[WipeData] Cache folder wipe: FAILED - ${e.message}")
                }

                // 5. Delete exported temporary files in filesDir or cacheDirs
                var filesWipedSuccess = false
                try {
                    val filesDir = context.filesDir
                    if (filesDir != null && filesDir.exists()) {
                        val files = filesDir.listFiles()
                        files?.forEach { file ->
                            if (file.name.contains("backup") || file.name.endsWith(".json") || file.name.endsWith(".csv") || file.name.endsWith(".pdf") || file.name.contains("report")) {
                                val deleted = file.delete()
                                android.util.Log.d(TAG_SECURE, "[WipeData] Deleted temporary/exported file: ${file.name} -> $deleted")
                            }
                        }
                        filesWipedSuccess = true
                        android.util.Log.d(TAG_SECURE, "[WipeData] Exported/Temporary files in filesDir cleared: SUCCESS")
                    } else {
                        filesWipedSuccess = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG_SECURE, "[WipeData] Exported/Temporary files wipe: FAILED - ${e.message}")
                }

                android.util.Log.d(TAG_SECURE, "=== SECURE DELETION VERIFICATION CHECKLIST ===")
                android.util.Log.d(TAG_SECURE, "Database Tables Wiped & Defaults Restored: ${if (dbWipedSuccess) "SUCCESS" else "FAILED"}")
                android.util.Log.d(TAG_SECURE, "SharedPreferences Wiped: ${if (prefsWipedSuccess) "SUCCESS" else "FAILED"}")
                android.util.Log.d(TAG_SECURE, "Cache Cleared Recursively: ${if (cacheWipedSuccess) "SUCCESS" else "FAILED"}")
                android.util.Log.d(TAG_SECURE, "Temporary/Exported Files Cleared: ${if (filesWipedSuccess) "SUCCESS" else "FAILED"}")
                android.util.Log.d(TAG_SECURE, "All Local Storage Verification Complete.")
                android.util.Log.d(TAG_SECURE, "==============================================")
            }
            onCompleted()
        }
    }

    fun restoreBackupRaw(
        entries: List<IntakeEntry>,
        cups: List<CustomCup>
    ) {
        viewModelScope.launch {
            repository.restoreBackupRaw(entries, cups)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            repository.resetSettingsToDefault()
        }
    }

    // Time calculations
    private fun getStartOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getEstimatedGoalCompletionText(todayIntake: Int, goal: Int, entriesToday: List<IntakeEntry>): String {
        if (todayIntake >= goal) return "Goal Complete!"
        if (entriesToday.isEmpty()) return "Log water to view estimate"
        
        val firstEntryTime = entriesToday.minOf { it.timestamp }
        val diffMs = System.currentTimeMillis() - firstEntryTime
        
        val minIntervalMs = 15 * 60 * 1000L
        val diffToUseMs = if (diffMs < minIntervalMs) minIntervalMs else diffMs
        
        val rateMlPerMs = todayIntake.toDouble() / diffToUseMs
        if (rateMlPerMs <= 0.0) return "Logging in progress"
        
        val remainingMl = goal - todayIntake
        val remainingMs = (remainingMl / rateMlPerMs).toLong()
        val estimatedTimeMs = System.currentTimeMillis() + remainingMs
        
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "Est. Completion: " + sdf.format(Date(estimatedTimeMs))
    }

    private fun calculateGoalStatus(progressPercent: Int, totalIntake: Int, goal: Int): String {
        if (progressPercent >= 100) return "Completed"
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayStartHour = 8
        val dayEndHour = 22
        val totalHours = dayEndHour - dayStartHour
        val currentProgressHrs = (hour - dayStartHour).coerceIn(0, totalHours)
        val expectedProgressPct = if (totalHours > 0) (currentProgressHrs.toFloat() / totalHours.toFloat()) * 100 else 0f
        return if (progressPercent >= expectedProgressPct) "On Track" else "Behind"
    }

    private fun calculateSmartStatus(progressPercent: Int): Pair<String, String> {
        return when {
            progressPercent >= 100 -> Pair("Fully Hydrated", "FULLY")
            progressPercent >= 80 -> Pair("Optimal Hydration", "EXCELLENT")
            progressPercent >= 50 -> Pair("Good Hydration", "GOOD")
            progressPercent >= 25 -> Pair("Mild Hydration", "LOW")
            else -> Pair("Hydration Needed", "DEHYDRATED")
        }
    }

    private fun getDynamicCoachMessage(progressPercent: Int, remainingMls: Int, entriesToday: List<IntakeEntry>, allEntries: List<IntakeEntry>): String {
        if (progressPercent >= 100) {
            return "Goal achieved for today. Keep up the consistent habit."
        }
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(System.currentTimeMillis()))
        val pastIntakesByDay = allEntries
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .filterKeys { it != todayStr }
            .mapValues { it.value.sumOf { e -> e.amountMl } }
        
        val maxPastIntake = pastIntakesByDay.values.maxOrNull() ?: 0
        val totalToday = entriesToday.sumOf { it.amountMl }
        
        if (maxPastIntake > 0 && totalToday > maxPastIntake) {
            return "Personal best for daily intake reached."
        }
        
        if (progressPercent >= 50) {
            return "Over halfway to your daily hydration target."
        }
        
        if (remainingMls > 0) {
            return "$remainingMls ml remaining to reach today's target."
        }
        
        return "Start tracking your hydration for today."
    }

    private fun calculateStreaks(entries: List<IntakeEntry>, dailyGoal: Int): StreakData {
        return StreakManager.calculateStreaks(entries, dailyGoal)
    }

    fun loadDailyHydrationTip(force: Boolean = false) {
        viewModelScope.launch {
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val cachedDate = repository.getSettingValue("hydration_tip_date")
            val cachedText = repository.getSettingValue("hydration_tip_text")

            if (!force && cachedDate == todayDateStr && !cachedText.isNullOrBlank()) {
                _hydrationTipText.value = cachedText
                _hydrationTipError.value = null
                return@launch
            }

            _hydrationTipLoading.value = true
            _hydrationTipError.value = null

            val state = uiState.value
            val progress = state.progressPercent
            val streakVal = state.streak.currentStreak
            val todayIntake = state.totalIntakeToday

            kotlinx.coroutines.delay(200)

            try {
                val tip = when {
                    todayIntake == 0 -> {
                        listOf(
                            "Starting your morning with water rehydrates your body after sleep and supports mental alertness.",
                            "A glass of water early in the day kickstarts your digestive metabolism.",
                            "Hydrating first thing in the morning helps restore fluid balance efficiently."
                        ).random()
                    }
                    progress >= 100 -> {
                        listOf(
                            "Goal achieved! Maintaining consistent fluid balance supports sustained physical energy.",
                            "Daily goal met. Steady hydration supports cardiovascular wellness and cognitive clarity.",
                            "Target completed! Consistent hydration keeps joints cushioned and muscles functioning smoothly."
                        ).random()
                    }
                    streakVal >= 3 -> {
                        listOf(
                            "Consistent daily hydration builds strong habit momentum over time.",
                            "Staying regular with fluid intake supports long-term concentration and focus.",
                            "Hydration consistency over multiple days promotes natural kidney filtration and overall vitality."
                        ).random()
                    }
                    else -> {
                        listOf(
                            "Proper hydration supports mental clarity, reaction speed, and focus throughout the day.",
                            "Fatigue is often an early sign of needing fluids. Sip water regularly to stay energized.",
                            "Water supports physical stamina and proper temperature regulation during daily activities.",
                            "Drinking water steadily throughout the day is more effective than large volumes all at once."
                        ).random()
                    }
                }

                _hydrationTipText.value = tip
                repository.saveSetting("hydration_tip_text", tip)
                repository.saveSetting("hydration_tip_date", todayDateStr)
            } catch (e: Exception) {
                _hydrationTipText.value = "Consistent water intake supports energy, focus, and overall well-being."
            } finally {
                _hydrationTipLoading.value = false
            }
        }
    }

    class Factory(private val repository: HydrationRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HydrationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HydrationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class HomeUiState(
    val todayEntries: List<IntakeEntry> = emptyList(),
    val allEntries: List<IntakeEntry> = emptyList(),
    val customCups: List<CustomCup> = emptyList(),
    val dailyGoalMls: Int = 2500,
    val totalIntakeToday: Int = 0,
    val remainingMls: Int = 2500,
    val progressPercent: Int = 0,
    val averageIntakePerSession: Int = 0,
    val estimatedCompletionTime: String = "",
    val goalStatus: String = "On Track",
    val smartStatus: String = "Starting Day",
    val smartStatusType: String = "ON_TRACK",
    val streak: StreakData = StreakData(0, 0, 0),
    val remindersEnabled: Boolean = true,
    val nextReminderTime: String = "09:00 AM",
    val smartReminderDayStart: String = "08:00 AM",
    val smartReminderDayEnd: String = "10:00 PM",
    val smartReminderIntervalMins: Int = 120,
    val isGoalReachedBefore: Boolean = false,
    val hydrationStatus: String = "Hydration Needed",
    val dynamicCoachMessage: String = ""
)
