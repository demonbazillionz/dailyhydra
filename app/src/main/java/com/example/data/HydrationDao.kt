package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {

    // Intake entries
    @Query("SELECT * FROM intake_entries ORDER BY timestamp DESC")
    fun getAllIntakeEntries(): Flow<List<IntakeEntry>>

    @Query("SELECT * FROM intake_entries WHERE timestamp >= :startOfMs AND timestamp <= :endOfMs ORDER BY timestamp DESC")
    fun getIntakeEntriesBetween(startOfMs: Long, endOfMs: Long): Flow<List<IntakeEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeEntry(entry: IntakeEntry): Long

    @Update
    suspend fun updateIntakeEntry(entry: IntakeEntry)

    @Delete
    suspend fun deleteIntakeEntry(entry: IntakeEntry)

    // Custom cups
    @Query("SELECT * FROM custom_cups ORDER BY id ASC")
    fun getAllCustomCups(): Flow<List<CustomCup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCup(cup: CustomCup)

    @Update
    suspend fun updateCustomCup(cup: CustomCup)

    @Delete
    suspend fun deleteCustomCup(cup: CustomCup)

    @Query("DELETE FROM custom_cups WHERE id = :id")
    suspend fun deleteCustomCupById(id: Int)

    @Query("DELETE FROM intake_entries")
    suspend fun clearAllIntakeEntries()

    @Query("DELETE FROM custom_cups")
    suspend fun clearAllCustomCups()

    @Query("DELETE FROM app_settings")
    suspend fun clearAllAppSettings()

    // App Settings
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<AppSettingsItem?>

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSettingsItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingsItem)
}
