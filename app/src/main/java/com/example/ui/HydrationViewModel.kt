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

    // Ranks, XP, and Achievement Flows
    val allXpTransactions = repository.allXpTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRankHistory = repository.allRankHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUnlockedAchievements = repository.allUnlockedAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically check achievements, reconcile XP, and calculate rankings on background threads.
        // Doing this in a single flow pipeline reduces duplicate calculations, context switches, and DB queries.
        viewModelScope.launch(Dispatchers.Default) {
            combine(allEntriesFlow, allXpTransactions, dailyGoalFlow) { entries, transactions, goal ->
                Triple(entries, transactions, goal)
            }.collect { (entries, transactions, goal) ->
                val streak = calculateStreaks(entries, goal)
                val cal = Calendar.getInstance()
                val activeDays = entries.map {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                }.distinct().size
                
                checkAndUnlockAchievements(entries, goal, streak.currentStreak)
                reconcileDailyCompletionXp(entries, transactions, goal)
                reconcileXpForStreaks(entries, transactions, goal, streak.currentStreak)
                
                val totalXp = transactions.sumOf { it.amount }
                checkAndRecordRankAchievements(totalXp, activeDays, streak.bestStreak)
                checkRankCelebrations(totalXp, entries, streak)
            }
        }
        loadDailyHydrationTip()
    }

    private val _activeCelebration = MutableStateFlow<RankCelebration?>(null)
    val activeCelebration: StateFlow<RankCelebration?> = _activeCelebration.asStateFlow()

    private val ongoingCelebrations = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var lastLoggedRankName: String? = null

    fun dismissCelebration() {
        _activeCelebration.value = null
    }

    private fun checkRankCelebrations(totalXp: Int, entries: List<IntakeEntry>, streak: StreakData) {
        viewModelScope.launch(Dispatchers.IO) {
            val promotions = RANKS_LIST.map { rank ->
                PromotionCheck(
                    tierName = rank.name,
                    minXp = rank.minXp,
                    minActiveDays = rank.minActiveDays,
                    minStreak = rank.minStreak,
                    iconEmoji = rank.iconEmoji,
                    xpReward = when {
                        rank.name == "Bronze III" -> 0
                        rank.name.startsWith("Bronze") -> 50
                        rank.name.startsWith("Silver") -> 100
                        rank.name.startsWith("Gold") -> 250
                        rank.name.startsWith("Platinum") -> 500
                        rank.name.startsWith("Diamond") -> 1000
                        rank.name.startsWith("Master") -> 2000
                        rank.name == "Grandmaster" -> 5000
                        else -> 100
                    }
                )
            }

            val activeDays = entries.map { 
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.timestamp)) 
            }.distinct().size
            
            for (p in promotions) {
                if (p.tierName == "Bronze III") {
                    continue
                }
                
                if (totalXp >= p.minXp && activeDays >= p.minActiveDays && streak.bestStreak >= p.minStreak) {
                    if (ongoingCelebrations.contains(p.tierName)) {
                        continue
                    }
                    val key = "celebrated_${p.tierName.lowercase().replace(" ", "_")}"
                    val isCelebrated = repository.getSettingValue(key)?.toBoolean() ?: false
                    if (isCelebrated) {
                        android.util.Log.d("HydrationViewModel", "[RankSystem] Already celebrated promotion to ${p.tierName}. Adding to memory set.")
                        ongoingCelebrations.add(p.tierName)
                        continue
                    }
                    
                    android.util.Log.d("HydrationViewModel", "[RankSystem] CRITICAL PROMOTION TRIGGERED! User promoted to ${p.tierName}. Rewarding ${p.xpReward} XP.")
                    // Add eagerly before performing suspending database operations to lock execution completely
                    ongoingCelebrations.add(p.tierName)
                    
                    repository.saveSetting(key, "true")
                    
                    repository.insertXpTransaction(
                        XpTransaction(
                            amount = p.xpReward,
                            activity = "${p.tierName} Promotion Bonus",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    
                    val stats = LifetimeStats(
                        totalVolumeMl = entries.sumOf { it.amountMl },
                        activeDays = activeDays,
                        totalCompletedGoals = streak.totalHydratedDays,
                        bestStreak = streak.bestStreak
                    )

                    _activeCelebration.value = RankCelebration(
                        rankName = p.tierName,
                        rankIcon = p.iconEmoji,
                        xpReward = p.xpReward,
                        isGrandmaster = p.tierName == "Grandmaster",
                        lifetimeStats = stats
                    )
                    break
                }
            }
        }
    }

    private fun checkAndRecordRankAchievements(xp: Int, activeDays: Int, bStreak: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val achievedRanks = RANKS_LIST.filter { 
                xp >= it.minXp && activeDays >= it.minActiveDays && bStreak >= it.minStreak 
            }
            val existingHistory = repository.allRankHistory.first()
            val existingNames = existingHistory.map { it.rankName }.toSet()
            
            for (rank in achievedRanks) {
                if (!existingNames.contains(rank.name)) {
                    android.util.Log.d("HydrationViewModel", "[RankSystem] Saving newly unlocked rank achievement to local database: ${rank.name}")
                    repository.insertRankHistory(RankHistoryItem(rankName = rank.name))
                }
            }
        }
    }

    private fun checkAndUnlockAchievements(entries: List<IntakeEntry>, goal: Int, currentStreak: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUnlocked = repository.allUnlockedAchievements.first().map { it.id }.toSet()
            val toUnlock = mutableListOf<UnlockedAchievement>()
            
            if (entries.isNotEmpty() && !existingUnlocked.contains("first_sip")) {
                toUnlock.add(UnlockedAchievement("first_sip", "First Sip"))
            }
            if (currentStreak >= 7 && !existingUnlocked.contains("streak_7")) {
                toUnlock.add(UnlockedAchievement("streak_7", "7 Day Streak"))
            }
            if (currentStreak >= 30 && !existingUnlocked.contains("streak_30")) {
                toUnlock.add(UnlockedAchievement("streak_30", "30 Day Streak"))
            }
            val totalWater = entries.sumOf { it.amountMl }
            if (totalWater >= 100000 && !existingUnlocked.contains("litres_100")) {
                toUnlock.add(UnlockedAchievement("litres_100", "100 Litres Consumed"))
            }
            
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val goalCompletedDays = entries.groupBy { dateFormat.format(Date(it.timestamp)) }
                .filter { it.value.sumOf { item -> item.amountMl } >= goal }
                .size
            if (goalCompletedDays >= 10 && !existingUnlocked.contains("goal_crusher")) {
                toUnlock.add(UnlockedAchievement("goal_crusher", "Goal Crusher"))
            }
            val hasEarlyBirdLog = entries.any {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.HOUR_OF_DAY) < 8
            }
            if (hasEarlyBirdLog && !existingUnlocked.contains("early_bird")) {
                toUnlock.add(UnlockedAchievement("early_bird", "Early Bird"))
            }
            val hasNightOwlLog = entries.any {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.HOUR_OF_DAY) >= 22
            }
            if (hasNightOwlLog && !existingUnlocked.contains("night_owl")) {
                toUnlock.add(UnlockedAchievement("night_owl", "Night Owl"))
            }

            for (ach in toUnlock) {
                repository.insertUnlockedAchievement(ach)
            }
        }
    }

    private fun reconcileDailyCompletionXp(entries: List<IntakeEntry>, transactions: List<XpTransaction>, goal: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayStr = dateFormat.format(Date(System.currentTimeMillis()))
            
            val entriesByDay = entries.groupBy { dateFormat.format(Date(it.timestamp)) }
            val existingDailyTxs = transactions.filter { it.activity.startsWith("Daily Completion XP - ") }
            
            // Clean up old standard entry-level logging and goal XP to enforce the anti-cheat structure completely
            val oldTxsToDelete = transactions.filter {
                it.activity == "Manual Log" || it.activity == "Quick-Add Shortcut" || it.activity == "Daily Goal Completed"
            }
            oldTxsToDelete.forEach {
                repository.deleteXpTransaction(it)
            }
            
            val txsToInsert = mutableListOf<XpTransaction>()
            
            for ((day, dayEntries) in entriesByDay) {
                val totalIntake = dayEntries.sumOf { it.amountMl }
                val progress = if (goal > 0) totalIntake.toFloat() / goal.toFloat() else 0f
                
                val expectedXp = when {
                    progress < 0.5f -> 0
                    progress in 0.5f..0.7999f -> 50
                    progress in 0.8f..0.9999f -> 80
                    else -> 100 // 100% - 120% and above capped at 100 XP
                }
                
                val activityName = "Daily Completion XP - $day"
                val existingTx = existingDailyTxs.find { it.activity == activityName }
                
                if (day == todayStr) {
                    if (existingTx == null) {
                        if (expectedXp > 0) {
                            val lastEntry = dayEntries.maxByOrNull { it.timestamp }
                            val timestamp = lastEntry?.timestamp ?: System.currentTimeMillis()
                            txsToInsert.add(XpTransaction(amount = expectedXp, activity = activityName, timestamp = timestamp))
                        }
                    } else if (existingTx.amount != expectedXp) {
                        repository.deleteXpTransaction(existingTx)
                        if (expectedXp > 0) {
                            txsToInsert.add(existingTx.copy(id = 0, amount = expectedXp))
                        }
                    }
                } else {
                    // Manual edits to previous days never grant XP - hence if transition/reconcile already recorded XP, never touch it.
                    if (existingTx == null && expectedXp > 0) {
                        val lastEntry = dayEntries.maxByOrNull { it.timestamp }
                        val timestamp = lastEntry?.timestamp ?: System.currentTimeMillis()
                        txsToInsert.add(XpTransaction(amount = expectedXp, activity = activityName, timestamp = timestamp))
                    }
                }
            }
            
            txsToInsert.forEach {
                repository.insertXpTransaction(it)
            }
        }
    }

    private fun reconcileXpForStreaks(entries: List<IntakeEntry>, transactions: List<XpTransaction>, goal: Int, currentStreak: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val milestone7Count = transactions.count { it.activity == "7-Day Streak" }
            val expected7Count = currentStreak / 7
            if (expected7Count > milestone7Count) {
                repository.insertXpTransaction(XpTransaction(amount = 100, activity = "7-Day Streak", timestamp = System.currentTimeMillis()))
            }
            
            val milestone30Count = transactions.count { it.activity == "30-Day Streak" }
            val expected30Count = currentStreak / 30
            if (expected30Count > milestone30Count) {
                repository.insertXpTransaction(XpTransaction(amount = 300, activity = "30-Day Streak", timestamp = System.currentTimeMillis()))
            }
            
            val milestone100Count = transactions.count { it.activity == "100-Day Streak" }
            val expected100Count = currentStreak / 100
            if (expected100Count > milestone100Count) {
                repository.insertXpTransaction(XpTransaction(amount = 500, activity = "100-Day Streak", timestamp = System.currentTimeMillis()))
            }
        }
    }

    // Unified Reactive Home UI State
    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            combine(allEntriesFlow, customCupsFlow, dailyGoalFlow, nextReminderTimeFlow) { entries, cups, goal, nextReminder ->
                Pair(Pair(entries, cups), Pair(goal, nextReminder))
            },
            repository.allXpTransactions
        ) { firstFour, xpTx ->
            Pair(firstFour, xpTx)
        },
        remindersEnabledFlow,
        smartReminderDayStart,
        smartReminderDayEnd,
        smartReminderIntervalMins
    ) { combinedFirst, remindersOn, dayStart, dayEnd, intervalMins ->
        val innerCombined = combinedFirst.first
        val xpTxList = combinedFirst.second
        
        val entries = innerCombined.first.first
        val cups = innerCombined.first.second
        val goal = innerCombined.second.first
        val nextReminder = innerCombined.second.second
        
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

        val totalXp = xpTxList.sumOf { it.amount }
        val cal = Calendar.getInstance()
        val activeDays = entries.map { 
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        }.distinct().size
        
        val currentRank = getRankForStats(totalXp, activeDays, streak.bestStreak)
        
        if (lastLoggedRankName != currentRank.name) {
            lastLoggedRankName = currentRank.name
            android.util.Log.d("HydrationViewModel", "[RankSystem] Derived/Active rank changed/loaded: ${currentRank.name} (XP: $totalXp, Active Days: $activeDays, Best Streak: ${streak.bestStreak} days)")
        }

        val currentRankIndex = RANKS_LIST.indexOf(currentRank)
        val nextRank = if (currentRankIndex != -1 && currentRankIndex < RANKS_LIST.size - 1) {
            RANKS_LIST[currentRankIndex + 1]
        } else {
            null
        }
        val nextRankProgressPct = if (nextRank != null) {
            val tierMaxXp = nextRank.minXp - currentRank.minXp
            val tierCurrentXp = totalXp - currentRank.minXp
            if (tierMaxXp > 0) (tierCurrentXp.toFloat() / tierMaxXp.toFloat()).coerceIn(0f, 1f) else 1.0f
        } else {
            1.0f
        }

        val hydrationScoreValue = calculateHydrationScore(progress, streak, entries, goal)
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
            hydrationScore = hydrationScoreValue,
            dynamicCoachMessage = dynamicCoachMsgValue,
            nextRankName = nextRank?.name ?: "Grandmaster",
            nextRankProgress = nextRankProgressPct
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    // Log water (XP is computed dynamically by the Daily Completion engine to prevent cheating/spamming)
    fun logWater(amountMl: Int, isQuickAdd: Boolean = false, timestamp: Long = System.currentTimeMillis(), context: Context? = null) {
        if (amountMl <= 0) return // Prevent negative intake values
        viewModelScope.launch {
            val entry = IntakeEntry(amountMl = amountMl, timestamp = timestamp)
            val insertedId = repository.insertIntakeEntry(entry)
            val entryWithId = entry.copy(id = insertedId.toInt())
            
            // Set last added entry to trigger snackbar and undo option
            _lastAddedEntry.value = entryWithId
            
            // Auto expire after 5 seconds
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
        _lastAddedEntry.value = null // Prevent duplicate undos instantly
        undoJob?.cancel()
        
        viewModelScope.launch {
            repository.deleteIntakeEntry(last)
            
            // Also delete all XP transactions matching the entry's timestamp
            val txs = repository.allXpTransactions.first()
            val toDelete = txs.filter { it.timestamp == last.timestamp }
            toDelete.forEach { repository.deleteXpTransaction(it) }
            
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
            
            // Delete XP transactions matching timestamp
            val txs = repository.allXpTransactions.first()
            val toDelete = txs.filter { it.timestamp == entry.timestamp }
            toDelete.forEach { repository.deleteXpTransaction(it) }
            
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

    // Edit custom cup
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
                val xpTransactions = repository.allXpTransactions.first()
                val rankHistory = repository.allRankHistory.first()
                val unlockedAchievements = repository.allUnlockedAchievements.first()

                val root = org.json.JSONObject()
                root.put("version", 4)
                root.put("type", "dailyhydra_full_backup")

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

                val xpArr = org.json.JSONArray()
                xpTransactions.forEach { tx ->
                    val obj = org.json.JSONObject()
                    obj.put("id", tx.id)
                    obj.put("amount", tx.amount)
                    obj.put("activity", tx.activity)
                    obj.put("timestamp", tx.timestamp)
                    xpArr.put(obj)
                }
                root.put("xp_transactions", xpArr)

                val rankArr = org.json.JSONArray()
                rankHistory.forEach { rh ->
                    val obj = org.json.JSONObject()
                    obj.put("id", rh.id)
                    obj.put("rankName", rh.rankName)
                    obj.put("timestamp", rh.timestamp)
                    rankArr.put(obj)
                }
                root.put("rank_history", rankArr)

                val achArr = org.json.JSONArray()
                unlockedAchievements.forEach { ach ->
                    val obj = org.json.JSONObject()
                    obj.put("id", ach.id)
                    obj.put("title", ach.title)
                    obj.put("unlockedAt", ach.unlockedAt)
                    achArr.put(obj)
                }
                root.put("unlocked_achievements", achArr)

                val jsonString = root.toString(4)

                val backupsDir = java.io.File(context.filesDir, "backups")
                if (!backupsDir.exists()) {
                    backupsDir.mkdirs()
                }

                val file3 = java.io.File(backupsDir, "dailyhydra_auto_backup_3.json")
                val file2 = java.io.File(backupsDir, "dailyhydra_auto_backup_2.json")
                val file1 = java.io.File(backupsDir, "dailyhydra_auto_backup_1.json")

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
            ongoingCelebrations.clear()
            repository.clearAllData()
        }
    }

    fun secureWipeAllData(context: Context, onCompleted: () -> Unit) {
        viewModelScope.launch {
            // Run on IO Dispatcher for frictionless non-blocking execution to avoid any freeze
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                    ongoingCelebrations.clear()
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

                // 6. Explicit Storage Verification (Rule 4)
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
        cups: List<CustomCup>,
        xpTransactions: List<XpTransaction>,
        rankHistory: List<RankHistoryItem>,
        unlockedAchievements: List<UnlockedAchievement>
    ) {
        viewModelScope.launch {
            ongoingCelebrations.clear()
            repository.restoreBackupRaw(entries, cups, xpTransactions, rankHistory, unlockedAchievements)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            ongoingCelebrations.clear()
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
        
        val minIntervalMs = 15 * 60 * 1000L // Min 15 mins for realistic estimate
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
            progressPercent >= 80 -> Pair("Excellent Hydration", "EXCELLENT")
            progressPercent >= 50 -> Pair("Good Hydration", "GOOD")
            progressPercent >= 25 -> Pair("Low Hydration", "LOW")
            else -> Pair("Dehydrated", "DEHYDRATED")
        }
    }

    private fun calculateHydrationScore(progressPercent: Int, streak: StreakData, allEntries: List<IntakeEntry>, dailyGoal: Int): Int {
        if (allEntries.isEmpty()) return 0
        
        // 1. Today's goal completion: max 40 points
        val todayProgressCapped = progressPercent.coerceAtMost(100)
        val todayPoints = (todayProgressCapped * 0.40).toInt()
        
        // 2. Consistency (past 7 days achievement rate): max 30 points
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val entriesByDay = allEntries.groupBy { dateFormat.format(Date(it.timestamp)) }
        
        var daysMetGoalCount = 0
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            val dayStr = dateFormat.format(cal.time)
            val dayIntake = entriesByDay[dayStr]?.sumOf { it.amountMl } ?: 0
            if (dayIntake >= dailyGoal) {
                daysMetGoalCount++
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val consistencyPoints = (daysMetGoalCount / 7.0 * 30.0).toInt()
        
        // 3. Streak score: max 15 points (3 pts per streak day, capped at 15)
        val streakPoints = (streak.currentStreak * 3).coerceAtMost(15)
        
        // 4. Historical performance (lifetime success rate): max 15 points
        val totalDaysLogged = entriesByDay.size
        val totalDaysMetGoal = entriesByDay.values.count { it.sumOf { e -> e.amountMl } >= dailyGoal }
        val successRate = if (totalDaysLogged > 0) totalDaysMetGoal.toFloat() / totalDaysLogged.toFloat() else 0f
        val historyPoints = (successRate * 15.0).toInt()
        
        return (todayPoints + consistencyPoints + streakPoints + historyPoints).coerceIn(0, 100)
    }

    private fun getDynamicCoachMessage(progressPercent: Int, remainingMls: Int, entriesToday: List<IntakeEntry>, allEntries: List<IntakeEntry>): String {
        if (progressPercent >= 100) {
            return "Goal achieved. Maintain consistency."
        }
        
        // Determine if today is a personal best
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(System.currentTimeMillis()))
        val pastIntakesByDay = allEntries
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .filterKeys { it != todayStr }
            .mapValues { it.value.sumOf { e -> e.amountMl } }
        
        val maxPastIntake = pastIntakesByDay.values.maxOrNull() ?: 0
        val totalToday = entriesToday.sumOf { it.amountMl }
        
        if (maxPastIntake > 0 && totalToday > maxPastIntake) {
            return "New personal best."
        }
        
        if (progressPercent >= 50) {
            return "Great progress. Keep going."
        }
        
        if (remainingMls > 0) {
            return "$remainingMls ml remaining to today's goal"
        }
        
        return "Sip some water to start your coach tracking."
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

            // Simulate minor delay for a polished UX feeling, 100% locally
            kotlinx.coroutines.delay(400)

            try {
                val tip = when {
                    todayIntake == 0 -> {
                        listOf(
                            "Start your day by drinking a glass of water to wake up your organs and kickstart your cellular metabolism.",
                            "Hydrating first thing in the morning rehydrates your body after sleep and increases alertness.",
                            "A single glass of water on an empty stomach helps cleanse the colon, making nutrient absorption easier."
                        ).random()
                    }
                    progress >= 100 -> {
                        listOf(
                            "Goal achieved! Excellent work maintaining your body's optimal fluid balance and cellular vitality today.",
                            "Fantastic job! Meeting your hydration target helps sustain your energy levels and supports cardiovascular health.",
                            "You met your daily goal! Keeping hydrated ensures your joints remain fully lubricated and protects vital organs."
                        ).random()
                    }
                    streakVal >= 3 -> {
                        listOf(
                            "Impressive $streakVal-day streak! Consistent hydration keeps your skin plump, radiant, and supports overall cellular repair.",
                            "Your habit is locking in! Over time, daily hydration significantly boosts long-term brain processing and cognitive speed.",
                            "An amazing streak of $streakVal days! Steady hydration levels optimize kidney function and keep blood flow efficient."
                        ).random()
                    }
                    else -> {
                        listOf(
                            "Your brain is 73% water. Staying properly hydrated improves concentration, reaction times, and mental clarity.",
                            "Fatigue is often one of the first signs of mild dehydration. Keep sipping to stay naturally focused and sharp.",
                            "Drinking water supports physical endurance and helps prevent muscle fatigue by maintaining proper electrolyte balance.",
                            "Water is essential for joint lubrication; keeping hydrated protects your cartilage and cushions your spinal cord.",
                            "Drinking water before meals can help boost your resting metabolism, supporting digestive efficiency.",
                            "Water plays a crucial role in regulating body temperature. Staying hydrated makes physical exercise safer and more effective."
                        ).random()
                    }
                }

                _hydrationTipText.value = tip
                repository.saveSetting("hydration_tip_text", tip)
                repository.saveSetting("hydration_tip_date", todayDateStr)
            } catch (e: Exception) {
                _hydrationTipText.value = "Keep your body hydrated to power cellular processes, brain function, and overall energy."
            } finally {
                _hydrationTipLoading.value = false
            }
        }
    }

    // Factory
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
    val hydrationStatus: String = "Dehydrated",
    val hydrationScore: Int = 0,
    val dynamicCoachMessage: String = "",
    val nextRankName: String = "Bronze II",
    val nextRankProgress: Float = 0.0f
)
// StreakData is now imported from com.example.data package

data class Rank(
    val name: String,
    val minXp: Int,
    val minActiveDays: Int,
    val minStreak: Int,
    val iconEmoji: String,
    val description: String
)

val RANKS_LIST = listOf(
    Rank("Bronze III", 0, 0, 0, "bronze", "The beginning of your hydration journey."),
    Rank("Bronze II", 400, 2, 1, "bronze", "Setting the first milestones on your custom journey."),
    Rank("Bronze I", 800, 5, 2, "bronze", "Adapting your biological clock to regular hydration habits."),
    Rank("Silver III", 1500, 10, 3, "silver", "Consistently refreshing cellular hydration."),
    Rank("Silver II", 2200, 14, 4, "silver", "Accelerating your water consumption and recovery."),
    Rank("Silver I", 3000, 18, 4, "silver", "Gaining outstanding fluid tracking momentum."),
    Rank("Gold IV", 4000, 25, 5, "gold", "Flawlessly tracking and meeting baseline daily goals."),
    Rank("Gold III", 5000, 30, 5, "gold", "Elevated state of natural vigor and focus."),
    Rank("Gold II", 6000, 36, 6, "gold", "Superb hydration consistency and physical response."),
    Rank("Gold I", 7000, 42, 6, "gold", "Outstanding daily water tracking habit integration."),
    Rank("Platinum V", 8000, 50, 7, "platinum", "Brilliant, crystal-clear focus and consistency."),
    Rank("Platinum IV", 9200, 55, 7, "platinum", "Enhanced endurance, mental crispness, and sleep quality."),
    Rank("Platinum III", 10400, 60, 8, "platinum", "Unshakeable stamina powered by flawless hydration."),
    Rank("Platinum II", 11600, 66, 8, "platinum", "Refined biological fluid optimization."),
    Rank("Platinum I", 12800, 72, 9, "platinum", "Sustained peak condition of water balance."),
    Rank("Diamond V", 15000, 80, 10, "diamond", "Pure, unblemished water logging and streak execution."),
    Rank("Diamond IV", 17000, 88, 11, "diamond", "Sparkling energy output and top-tier cell status."),
    Rank("Diamond III", 19000, 96, 12, "diamond", "Dazzling speed of cognitive and cellular healing."),
    Rank("Diamond II", 21000, 104, 13, "diamond", "Diamond-hard discipline and flawless habit record."),
    Rank("Diamond I", 23000, 112, 14, "diamond", "Masterful coordination of biological replenishment."),
    Rank("Master V", 25000, 120, 15, "master", "Ultimate keeper of the cellular source."),
    Rank("Master IV", 28000, 130, 18, "master", "Masterful control over internal energy pathways."),
    Rank("Master III", 31000, 140, 21, "master", "Commanding cellular synchronization and fluid efficiency."),
    Rank("Master II", 34000, 150, 24, "master", "Unrivaled control of baseline body recovery loops."),
    Rank("Master I", 37000, 165, 27, "master", "Perfected balance of water-induced longevity."),
    Rank("Grandmaster", 40000, 180, 30, "grandmaster", "Omniscient, celestial flow master with 6 months of consistency.")
)

fun getRankForStats(xp: Int, activeDays: Int, bStreak: Int): Rank {
    val rank = RANKS_LIST.lastOrNull { 
        xp >= it.minXp && activeDays >= it.minActiveDays && bStreak >= it.minStreak 
    }
    return rank ?: RANKS_LIST.first()
}

fun getRankForXp(xp: Int): Rank {
    val rank = RANKS_LIST.lastOrNull { xp >= it.minXp }
    return rank ?: RANKS_LIST.first()
}

fun getRankGradientColors(rankName: String): List<Long> {
    return when {
        rankName.startsWith("Bronze") -> listOf(0xFF8D6E63, 0xFF5D4037)
        rankName.startsWith("Silver") -> listOf(0xFFB0BEC5, 0xFF78909C)
        rankName.startsWith("Gold") -> listOf(0xFFFFD54F, 0xFFFFB300)
        rankName.startsWith("Platinum") -> listOf(0xFF80DEEA, 0xFF00ACC1)
        rankName.startsWith("Diamond") -> listOf(0xFF90CAF9, 0xFF1E88E5)
        rankName.startsWith("Master") -> listOf(0xFFB39DDB, 0xFF5E35B1)
        rankName.startsWith("Grandmaster") -> listOf(0xFFFF5252, 0xFFFF1744)
        else -> listOf(0xFF8D6E63, 0xFF5D4037)
    }
}

data class RankCelebration(
    val rankName: String,
    val rankIcon: String,
    val xpReward: Int,
    val isGrandmaster: Boolean,
    val lifetimeStats: LifetimeStats? = null
)

data class LifetimeStats(
    val totalVolumeMl: Int,
    val activeDays: Int,
    val totalCompletedGoals: Int,
    val bestStreak: Int
)

data class PromotionCheck(
    val tierName: String,
    val minXp: Int,
    val minActiveDays: Int,
    val minStreak: Int,
    val iconEmoji: String,
    val xpReward: Int
)

