package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "xp_transactions",
    indices = [androidx.room.Index(value = ["timestamp"])]
)
data class XpTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Int,
    val activity: String, // "Manual Log", "Quick-Add Shortcut", "Daily Goal Completed", "7-Day Streak", "30-Day Streak", "100-Day Streak"
    val timestamp: Long = System.currentTimeMillis()
)
