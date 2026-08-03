package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rank_history")
data class RankHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rankName: String,
    val timestamp: Long = System.currentTimeMillis()
)
