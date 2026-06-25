package com.example.data

import kotlinx.coroutines.flow.Flow

class HydrationRepository(private val dao: HydrationDao) {

    val allIntakeEntries: Flow<List<IntakeEntry>> = dao.getAllIntakeEntries()

    val customCups: Flow<List<CustomCup>> = dao.getAllCustomCups()
    
    // Ranks and XP
    val allXpTransactions: Flow<List<XpTransaction>> = dao.getAllXpTransactions()
    val allRankHistory: Flow<List<RankHistoryItem>> = dao.getAllRankHistory()
    val allUnlockedAchievements: Flow<List<UnlockedAchievement>> = dao.getAllUnlockedAchievements()

    suspend fun insertXpTransaction(transaction: XpTransaction) {
        dao.insertXpTransaction(transaction)
    }

    suspend fun deleteXpTransaction(transaction: XpTransaction) {
        dao.deleteXpTransaction(transaction)
    }

    suspend fun insertRankHistory(item: RankHistoryItem) {
        dao.insertRankHistory(item)
    }

    suspend fun insertUnlockedAchievement(achievement: UnlockedAchievement) {
        dao.insertUnlockedAchievement(achievement)
    }

    fun getIntakeEntriesBetween(startMs: Long, endMs: Long): Flow<List<IntakeEntry>> {
        return dao.getIntakeEntriesBetween(startMs, endMs)
    }

    suspend fun insertIntakeEntry(entry: IntakeEntry): Long {
        return dao.insertIntakeEntry(entry)
    }

    suspend fun updateIntakeEntry(entry: IntakeEntry) {
        dao.updateIntakeEntry(entry)
    }

    suspend fun deleteIntakeEntry(entry: IntakeEntry) {
        dao.deleteIntakeEntry(entry)
    }

    suspend fun insertCustomCup(cup: CustomCup) {
        dao.insertCustomCup(cup)
    }

    suspend fun updateCustomCup(cup: CustomCup) {
        dao.updateCustomCup(cup)
    }

    suspend fun deleteCustomCup(cup: CustomCup) {
        dao.deleteCustomCup(cup)
    }

    suspend fun deleteCustomCupById(id: Int) {
        dao.deleteCustomCupById(id)
    }

    fun getSettingFlow(key: String): Flow<AppSettingsItem?> {
        return dao.getSettingFlow(key)
    }

    suspend fun getSettingValue(key: String): String? {
        return dao.getSetting(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(AppSettingsItem(key, value))
    }

    suspend fun clearIntakeHistoryOnly() {
        dao.clearAllIntakeEntries()
    }

    suspend fun clearAllData() {
        dao.clearAllIntakeEntries()
        dao.clearAllCustomCups()
        dao.clearAllAppSettings()
        dao.clearAllXpTransactions()
        dao.clearAllRankHistory()
        dao.clearAllUnlockedAchievements()
        
        // Pre-populate default cups
        dao.insertCustomCup(CustomCup(name = "Glass", amountMl = 250, iconName = "glass"))
        dao.insertCustomCup(CustomCup(name = "Bottle", amountMl = 500, iconName = "bottle"))
        dao.insertCustomCup(CustomCup(name = "Steel Bottle", amountMl = 1000, iconName = "steel"))
        dao.insertCustomCup(CustomCup(name = "Tumbler", amountMl = 750, iconName = "tumbler"))

        // Re-populate basic defaults
        dao.insertSetting(AppSettingsItem("daily_goal", "2500"))
        dao.insertSetting(AppSettingsItem("reminders_enabled", "true"))
        dao.insertSetting(AppSettingsItem("next_reminder_time", "09:00 AM"))
    }

    suspend fun restoreBackupRaw(
        entries: List<IntakeEntry>,
        cups: List<CustomCup>,
        xpTransactions: List<XpTransaction>,
        rankHistory: List<RankHistoryItem>,
        unlockedAchievements: List<UnlockedAchievement>
    ) {
        dao.clearAllIntakeEntries()
        dao.clearAllCustomCups()
        dao.clearAllAppSettings()
        dao.clearAllXpTransactions()
        dao.clearAllRankHistory()
        dao.clearAllUnlockedAchievements()

        if (cups.isNotEmpty()) {
            cups.forEach { dao.insertCustomCup(it) }
        } else {
            dao.insertCustomCup(CustomCup(name = "Glass", amountMl = 250, iconName = "glass"))
            dao.insertCustomCup(CustomCup(name = "Bottle", amountMl = 500, iconName = "bottle"))
            dao.insertCustomCup(CustomCup(name = "Steel Bottle", amountMl = 1000, iconName = "steel"))
            dao.insertCustomCup(CustomCup(name = "Tumbler", amountMl = 750, iconName = "tumbler"))
        }

        // Default setting configuration
        dao.insertSetting(AppSettingsItem("daily_goal", "2500"))
        dao.insertSetting(AppSettingsItem("reminders_enabled", "true"))
        dao.insertSetting(AppSettingsItem("next_reminder_time", "09:00 AM"))
        dao.insertSetting(AppSettingsItem("onboarded", "true"))

        entries.forEach { dao.insertIntakeEntry(it) }
        xpTransactions.forEach { dao.insertXpTransaction(it) }
        rankHistory.forEach { dao.insertRankHistory(it) }
        unlockedAchievements.forEach { dao.insertUnlockedAchievement(it) }
    }

    suspend fun resetSettingsToDefault() {
        dao.clearAllAppSettings()
        
        // Setup initial default set
        dao.insertSetting(AppSettingsItem("daily_goal", "2500"))
        dao.insertSetting(AppSettingsItem("reminders_enabled", "true"))
        dao.insertSetting(AppSettingsItem("next_reminder_time", "09:00 AM"))
        dao.insertSetting(AppSettingsItem("app_theme", "System"))
        dao.insertSetting(AppSettingsItem("dynamic_colors_enabled", "false"))
        dao.insertSetting(AppSettingsItem("amoled_dark_mode", "false"))
        dao.insertSetting(AppSettingsItem("goal_unit", "ml"))
        dao.insertSetting(AppSettingsItem("haptic_feedback_enabled", "true"))
        dao.insertSetting(AppSettingsItem("animations_enabled", "true"))
        dao.insertSetting(AppSettingsItem("quiet_hours_enabled", "false"))
        dao.insertSetting(AppSettingsItem("quiet_hours_start", "22:00"))
        dao.insertSetting(AppSettingsItem("quiet_hours_end", "08:00"))
        dao.insertSetting(AppSettingsItem("default_quick_add_mls", "250"))
    }
}
