package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsItem(
    @PrimaryKey val key: String,
    val value: String
)
