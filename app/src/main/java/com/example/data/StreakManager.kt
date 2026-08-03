package com.example.data

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakData(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalHydratedDays: Int
)

object StreakManager {
    private const val TAG = "StreakManager"

    fun calculateStreaks(entries: List<IntakeEntry>, dailyGoal: Int): StreakData {
        if (entries.isEmpty()) {
            Log.d(TAG, "[StreakManager] Calculation triggered, but history is empty.")
            Log.d(TAG, "[StreakManager] Streak reset reason: No hydration history entries found.")
            return StreakData(0, 0, 0)
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        // Group entries by local date "yyyyMMdd"
        val entriesByDay = entries.groupBy { dateFormat.format(Date(it.timestamp)) }
        
        // Rule 6: Future dates must be ignored
        val validEntriesByDay = entriesByDay.filter { (dayStr, _) ->
            if (dayStr > todayStr) {
                Log.d(TAG, "[StreakManager] Ignoring future hydration entry on date: $dayStr")
                false
            } else {
                true
            }
        }

        // Calculate total amount per day (Rule 6: Duplicate entries must not inflate streak, we sum them)
        val daysWithIntakeTotal = validEntriesByDay.mapValues { (_, dayEntries) ->
            dayEntries.sumOf { it.amountMl }
        }
        
        // Find days where the intake >= dailyGoal (Rule 1: Reaching the daily goal)
        val metGoalDaysSet = daysWithIntakeTotal.filter { it.value >= dailyGoal }.keys
        val totalHydratedDays = metGoalDaysSet.size

        // Parse and sort the dates when the goal was met
        val activeDaysSorted = metGoalDaysSet.map { dateFormat.parse(it)!! }.sorted()

        // Extensive logging (Rule 7: Required diagnostic logging)
        metGoalDaysSet.sorted().forEach { dateStr ->
            Log.d(TAG, "[StreakManager] Goal reached date: $dateStr")
        }
        Log.d(TAG, "[StreakManager] Consecutive completed days found count: $totalHydratedDays. Days met goal: ${metGoalDaysSet.sorted()}")

        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        if (activeDaysSorted.isNotEmpty()) {
            tempStreak = 1
            bestStreak = 1
            for (i in 1 until activeDaysSorted.size) {
                // Rule 5: Calculate streak from historical data using calendar increment.
                // Avoid millisecond division which can fail under DST shifts (23/25 hour days).
                val prevCal = Calendar.getInstance().apply {
                    time = activeDaysSorted[i - 1]
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val prevPlusOne = dateFormat.format(prevCal.time)
                val currStr = dateFormat.format(activeDaysSorted[i])
                
                if (currStr == prevPlusOne) {
                    tempStreak++
                } else {
                    if (tempStreak > bestStreak) {
                        bestStreak = tempStreak
                    }
                    tempStreak = 1
                }
            }
            if (tempStreak > bestStreak) {
                bestStreak = tempStreak
            }
        }

        // Calculate CURRENT streak looking backwards from today/yesterday (Rule 5 & 6)
        val calendar = Calendar.getInstance()
        val todayFormatted = dateFormat.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayFormatted = dateFormat.format(calendar.time)
        
        val containsToday = metGoalDaysSet.contains(todayFormatted)
        val containsYesterday = metGoalDaysSet.contains(yesterdayFormatted)

        if (containsToday || containsYesterday) {
            val testCal = Calendar.getInstance()
            if (!containsToday && containsYesterday) {
                // Yesterday was completed but today is not completed yet. Start scanning backwards from yesterday!
                testCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            while (true) {
                val dayStr = dateFormat.format(testCal.time)
                if (metGoalDaysSet.contains(dayStr)) {
                    currentStreak++
                    testCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            Log.d(TAG, "[StreakManager] Scanned backward: currentStreak = $currentStreak")
        } else {
            currentStreak = 0
            val reason = if (metGoalDaysSet.isEmpty()) {
                "No goals completed yet in history."
            } else {
                "User missed completing goal yesterday ($yesterdayFormatted) and today is not completed yet ($todayFormatted)."
            }
            Log.d(TAG, "[StreakManager] Streak reset reason: $reason")
        }

        val finalBestStreak = maxOf(bestStreak, currentStreak)
        Log.d(TAG, "[StreakManager] Current streak value: $currentStreak, Best streak value: $finalBestStreak")

        return StreakData(currentStreak, finalBestStreak, totalHydratedDays)
    }
}
