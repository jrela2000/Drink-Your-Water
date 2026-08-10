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
            repository.wipeAllUserDataAndReset()
        }
    }
}
