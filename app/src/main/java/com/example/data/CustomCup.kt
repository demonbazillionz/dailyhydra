package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_cups")
data class CustomCup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amountMl: Int,
    val iconName: String
)
