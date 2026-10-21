package com.aistudio.drinkyourwater.hydra.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun frameworkDao(): FrameworkDao
    abstract fun completionLogDao(): CompletionLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        /**
         * No schema change; fixes reminder data written by version 1:
         * - Default water reminders and framework-builder reminders were saved with a "1hr"
         *   interval, so each one repeated hourly until 10 PM (about 50 alerts a day). They're
         *   meant to fire once a day at their time. Reminders the user added by hand
         *   (frameworkId IS NULL) keep whatever frequency they typed.
         * - Snooze counts could get stuck at the 2-snooze cap; start everyone fresh.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE reminders SET frequency = 'daily' " +
                        "WHERE frequency = '1hr' AND frameworkId IS NOT NULL"
                )
                db.execSQL("UPDATE reminders SET snoozeCount = 0")
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
