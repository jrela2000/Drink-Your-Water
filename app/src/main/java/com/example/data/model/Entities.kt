package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val frequency: String = "1hr", // 30min | 1hr | 2hr | 3hr | custom
    val customIntervalMinutes: Int = 60,
    val startTime: String = "08:00",
    val endTime: String = "22:00",
    val activeDays: String = "true,true,true,true,true,true,true", // 7 bools as CSV
    val isActive: Boolean = true,
    val snoozeCount: Int = 0,
    val frameworkId: Long? = null,
    val scheduledTime: String = "09:00 AM"
)

@Entity(tableName = "frameworks")
data class Framework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val overlayTheme: String = "midnight-water", // midnight-water | ice-teal | deep-ocean | cool-mint
    val motivationalContent: String = "affirmations", // waterFacts | affirmations | scripture | frameworkTip | custom
    val customMessage: String = "",
    val status: String = "draft", // active | draft
    val isWater: Boolean = false
)

@Entity(tableName = "completion_logs")
data class CompletionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val reminderText: String,
    val wasScheduledAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val snoozeCount: Int = 0
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Long = 1,
    val onboardingComplete: Boolean = false,
    val motivationalStyle: String = "waterFacts", // waterFacts | affirmations | scripture
    val overlayTheme: String = "midnight-water",
    val notificationSound: String = "tone1", // tone1 | tone2 | tone3 | tone4
    val vacationModeEnd: Long? = null,
    val isPremium: Boolean = false,
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0
)
