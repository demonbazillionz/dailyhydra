package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievement(
    @PrimaryKey val id: String,
    val title: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
