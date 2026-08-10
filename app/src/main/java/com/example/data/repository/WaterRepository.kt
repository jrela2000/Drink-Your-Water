package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.CompletionLog
import com.example.data.model.Framework
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val db: AppDatabase) {

    val reminders: Flow<List<Reminder>> = db.reminderDao().getAllReminders()
    val frameworks: Flow<List<Framework>> = db.frameworkDao().getAllFrameworks()
    val completionLogs: Flow<List<CompletionLog>> = db.completionLogDao().getAllLogs()
    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()

    suspend fun ensureInitialDataSeeded() {
        var profile = db.userProfileDao().getUserProfileDirect()
        if (profile == null) {
            profile = UserProfile(
                id = 1,
                onboardingComplete = false,
                motivationalStyle = "waterFacts",
                overlayTheme = "midnight-water",
                notificationSound = "tone1",
                isPremium = false,
                streakCount = 3,
                bestStreak = 7,
                totalCompletions = 18
            )
            db.userProfileDao().insertOrUpdateProfile(profile)
        }

        // Seed default free Water Framework if none exists
        val currentFrameworks = db.frameworkDao().getAllFrameworks()
        // Check direct insert if table is empty
        val waterFramework = Framework(
            id = 1,
            name = "Water Hydration",
            overlayTheme = "midnight-water",
            motivationalContent = "waterFacts",
            customMessage = "Drink a glass of cold fresh water now!",
            status = "active",
            isWater = true
        )
        db.frameworkDao().insertFramework(waterFramework)

        // Seed initial 6 water reminders if reminders table is empty
        val existingReminder = db.reminderDao().getReminderById(1)
        if (existingReminder == null) {
            val defaultReminders = listOf(
                Reminder(
                    id = 1,
                    text = "Morning Hydration (First Glass)",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "08:00 AM"
                ),
                Reminder(
                    id = 2,
                    text = "Post-Breakfast Water Refresh",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "10:30 AM"
                ),
                Reminder(
                    id = 3,
                    text = "Mid-Day Hydration Boost",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "01:00 PM"
                ),
                Reminder(
                    id = 4,
                    text = "Afternoon Energy Hydration",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "03:30 PM"
                ),
                Reminder(
                    id = 5,
                    text = "Pre-Dinner Glass of Water",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "06:30 PM"
                ),
                Reminder(
                    id = 6,
                    text = "Evening Wind-Down Hydration",
                    frequency = "1hr",
                    startTime = "08:00",
                    endTime = "22:00",
                    isActive = true,
                    frameworkId = 1,
                    scheduledTime = "09:00 PM"
                )
            )
            db.reminderDao().insertReminders(defaultReminders)
        }
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return db.reminderDao().getReminderById(id)
    }

    suspend fun insertOrUpdateReminder(reminder: Reminder): Long {
        return db.reminderDao().insertReminder(reminder)
    }

    suspend fun updateReminderStatus(reminderId: Long, isActive: Boolean) {
        val reminder = db.reminderDao().getReminderById(reminderId)
        if (reminder != null) {
            db.reminderDao().updateReminder(reminder.copy(isActive = isActive))
        }
    }

    suspend fun createFrameworkWithReminders(
        frameworkName: String,
        remindersList: List<Pair<String, String>>, // time, text
        overlayTheme: String,
        motivationalContent: String,
        customMessage: String,
        isPremiumActivate: Boolean = true
    ): Long {
        val framework = Framework(
            name = frameworkName,
            overlayTheme = overlayTheme,
            motivationalContent = motivationalContent,
            customMessage = customMessage,
            status = if (isPremiumActivate) "active" else "draft",
            isWater = false
        )
        val frameworkId = db.frameworkDao().insertFramework(framework)

        val remindersToInsert = remindersList.map { (time, text) ->
            Reminder(
                text = text,
                scheduledTime = time,
                frameworkId = frameworkId,
                isActive = isPremiumActivate
            )
        }
        db.reminderDao().insertReminders(remindersToInsert)

        if (isPremiumActivate) {
            val profile = db.userProfileDao().getUserProfileDirect() ?: UserProfile()
            db.userProfileDao().insertOrUpdateProfile(profile.copy(isPremium = true))
        }

        return frameworkId
    }

    suspend fun logCompletionAndUnlock(reminderId: Long, reminderText: String, snoozeCount: Int) {
        val log = CompletionLog(
            reminderId = reminderId,
            reminderText = reminderText,
            wasScheduledAt = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis(),
            snoozeCount = snoozeCount
        )
        db.completionLogDao().insertLog(log)

        // Bump profile statistics
        val profile = db.userProfileDao().getUserProfileDirect() ?: UserProfile()
        val newStreak = profile.streakCount + 1
        val newBest = maxOf(profile.bestStreak, newStreak)
        val newTotal = profile.totalCompletions + 1

        db.userProfileDao().insertOrUpdateProfile(
            profile.copy(
                streakCount = newStreak,
                bestStreak = newBest,
                totalCompletions = newTotal
            )
        )
    }

    suspend fun updateProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdateProfile(profile)
    }

    suspend fun completeOnboarding(motivationalStyle: String) {
        val current = db.userProfileDao().getUserProfileDirect() ?: UserProfile()
        db.userProfileDao().insertOrUpdateProfile(
            current.copy(
                onboardingComplete = true,
                motivationalStyle = motivationalStyle
            )
        )
    }

    suspend fun wipeAllUserDataAndReset() {
        db.reminderDao().deleteAll()
        db.frameworkDao().deleteAll()
        db.completionLogDao().deleteAll()
        db.userProfileDao().deleteAll()
        ensureInitialDataSeeded()
    }
}
