package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        IntakeEntry::class,
        CustomCup::class,
        AppSettingsItem::class
    ],
    version = 5,
    exportSchema = false
)
abstract class HydrationDatabase : RoomDatabase() {
    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var INSTANCE: HydrationDatabase? = null

        fun getDatabase(context: Context): HydrationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydrationDatabase::class.java,
                    "dailyhydra_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default categories & metrics
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).hydrationDao()
                            // Standard Cup Sizes
                            dao.insertCustomCup(CustomCup(name = "Glass", amountMl = 250, iconName = "glass"))
                            dao.insertCustomCup(CustomCup(name = "Bottle", amountMl = 500, iconName = "bottle"))
                            dao.insertCustomCup(CustomCup(name = "Steel Bottle", amountMl = 1000, iconName = "steel"))
                            dao.insertCustomCup(CustomCup(name = "Tumbler", amountMl = 750, iconName = "tumbler"))

                            // Default Goal & Preference configuration
                            dao.insertSetting(AppSettingsItem("daily_goal", "2500"))
                            dao.insertSetting(AppSettingsItem("reminders_enabled", "true"))
                            dao.insertSetting(AppSettingsItem("next_reminder_time", "09:00"))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
