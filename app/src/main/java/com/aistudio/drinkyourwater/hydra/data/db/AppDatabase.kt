package com.aistudio.drinkyourwater.hydra.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.drinkyourwater.hydra.data.model.CompletionLog
import com.aistudio.drinkyourwater.hydra.data.model.Framework
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.data.model.UserProfile

@Database(
    entities = [
        Reminder::class,
        Framework::class,
        CompletionLog::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun frameworkDao(): FrameworkDao
    abstract fun completionLogDao(): CompletionLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drink_your_water_database"
                )
                    // Deliberately no fallbackToDestructiveMigration(): a future version bump
                    // with no matching Migration should crash loudly during development so the
                    // missing migration gets written, instead of silently wiping every user's
                    // reminders/streaks/history in production. Downgrades (installing an older
                    // debug build over a newer one) are dev-only and safe to reset.
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
