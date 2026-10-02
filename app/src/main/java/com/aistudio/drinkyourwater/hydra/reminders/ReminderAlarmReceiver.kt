package com.aistudio.drinkyourwater.hydra.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.drinkyourwater.hydra.data.db.AppDatabase
import com.aistudio.drinkyourwater.hydra.data.repository.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0) return
        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val repository = WaterRepository(db)
                val reminder = if (isSnooze) {
                    db.reminderDao().getReminderById(reminderId)
                } else {
                    repository.startNewFiring(reminderId)
                }
                if (reminder != null && reminder.isActive) {
                    val profile = db.userProfileDao().getUserProfileDirect()
                    val onVacation = profile?.vacationModeEnd?.let { it > System.currentTimeMillis() } ?: false
                    if (!onVacation) {
                        NotificationHelper.showReminderNotification(appContext, reminder, profile)
                    }
                    // Re-schedule the following occurrence regardless of vacation mode, so
                    // reminders resume automatically once vacation mode ends.
                    ReminderScheduler.scheduleReminder(appContext, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
