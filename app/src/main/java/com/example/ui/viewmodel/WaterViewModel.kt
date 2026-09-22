package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CompletionLog
import com.example.data.model.Framework
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import com.example.data.repository.WaterRepository
import com.example.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WaterUiState(
    val userProfile: UserProfile = UserProfile(),
    val reminders: List<Reminder> = emptyList(),
    val frameworks: List<Framework> = emptyList(),
    val completionLogs: List<CompletionLog> = emptyList(),
    val isLoading: Boolean = false
)

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WaterRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WaterRepository(db)
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
            // Re-sync alarms with the active reminder set on every app open, since alarms
            // set with AlarmManager don't persist across reboots and edits made while the
            // app was closed (or before exact-alarm permission was granted) need to be honored.
            val active = repository.getAllRemindersOnce().filter { it.isActive }
            ReminderScheduler.rescheduleAll(application, active)
        }
    }

    val uiState: StateFlow<WaterUiState> = combine(
        repository.userProfile,
        repository.reminders,
        repository.frameworks,
        repository.completionLogs
    ) { profile, reminders, frameworks, logs ->
        WaterUiState(
            userProfile = profile ?: UserProfile(),
            reminders = reminders,
            frameworks = frameworks,
            completionLogs = logs,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterUiState(isLoading = true)
    )

    fun toggleReminder(reminderId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateReminderStatus(reminderId, isActive)
            if (isActive) {
                val reminder = repository.getReminderById(reminderId)
                if (reminder != null) ReminderScheduler.scheduleReminder(getApplication(), reminder)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), reminderId)
            }
        }
    }

    /**
     * Persists a new or edited reminder (unlike [toggleReminder], which only flips
     * isActive) and re-schedules its alarm to match the saved text/time/frequency.
     */
    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            val savedId = repository.insertOrUpdateReminder(reminder)
            val saved = reminder.copy(id = savedId)
            if (saved.isActive) {
                ReminderScheduler.scheduleReminder(getApplication(), saved)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), saved.id)
            }
        }
    }

    fun logCompletion(reminderId: Long, reminderText: String, snoozeCount: Int) {
        viewModelScope.launch {
            repository.logCompletionAndUnlock(reminderId, reminderText, snoozeCount)
        }
    }

    fun createFrameworkWithReminders(
        name: String,
        reminders: List<Pair<String, String>>,
        theme: String,
        motivationalContent: String,
        customMessage: String,
        isPremiumActivate: Boolean,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createFrameworkWithReminders(
                frameworkName = name,
                remindersList = reminders,
                overlayTheme = theme,
                motivationalContent = motivationalContent,
                customMessage = customMessage,
                isPremiumActivate = isPremiumActivate
            )
            repository.getRemindersForFrameworkOnce(id)
                .filter { it.isActive }
                .forEach { ReminderScheduler.scheduleReminder(getApplication(), it) }
            onComplete(id)
        }
    }

    fun updateNotificationSound(sound: String) {
        viewModelScope.launch {
            val current = uiState.value.userProfile
            repository.updateProfile(current.copy(notificationSound = sound))
        }
    }

    fun updateOverlayTheme(theme: String) {
        viewModelScope.launch {
            val current = uiState.value.userProfile
            repository.updateProfile(current.copy(overlayTheme = theme))
        }
    }

    fun updateMotivationalStyle(style: String) {
        viewModelScope.launch {
            val current = uiState.value.userProfile
            repository.updateProfile(current.copy(motivationalStyle = style))
        }
    }

    fun updateVacationMode(endDays: Int?) {
        viewModelScope.launch {
            val current = uiState.value.userProfile
            val endTime = if (endDays != null && endDays > 0) {
                System.currentTimeMillis() + (endDays * 24 * 60 * 60 * 1000L)
            } else null
            repository.updateProfile(current.copy(vacationModeEnd = endTime))
        }
    }

    fun completeOnboarding(style: String) {
        viewModelScope.launch {
            repository.completeOnboarding(style)
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            uiState.value.reminders.forEach { ReminderScheduler.cancelReminder(getApplication(), it.id) }
            repository.wipeAllUserDataAndReset()
            repository.getAllRemindersOnce()
                .filter { it.isActive }
                .forEach { ReminderScheduler.scheduleReminder(getApplication(), it) }
        }
    }
}
