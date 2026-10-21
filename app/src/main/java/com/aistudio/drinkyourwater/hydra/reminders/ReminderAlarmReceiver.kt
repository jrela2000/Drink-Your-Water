package com.aistudio.drinkyourwater.hydra.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.drinkyourwater.hydra.data.db.AppDatabase
import com.aistudio.drinkyourwater.hydra.data.repository.WaterRepository
import com.aistudio.drinkyourwater.hydra.reminders.ReminderScheduler.AlarmKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0) return
        val kind = AlarmKind.fromAction(intent.action)
        val escalationLevel = intent.getIntExtra(ReminderScheduler.EXTRA_ESCALATION_LEVEL, 0)
        val firingAt = intent.getLongExtra(ReminderScheduler.EXTRA_FIRING_AT, 0L)

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val repository = WaterRepository(db)
                val now = System.currentTimeMillis()
                val reminder = when (kind) {
                    // A regular firing is a new check-in with a fresh snooze allowance.
                    AlarmKind.FIRE -> repository.startNewFiring(reminderId)
                    AlarmKind.SNOOZE, AlarmKind.ESCALATE -> repository.getReminderById(reminderId)
                }
                if (reminder == null || !reminder.isActive) return@launch

                val profile = db.userProfileDao().getUserProfileDirect()
                val onVacation = profile?.vacationModeEnd?.let { it > now } ?: false

                when (kind) {
                    AlarmKind.ESCALATE -> {
                        // Stop as soon as the check-in was confirmed (or the user went on vacation).
                        if (!onVacation && !repository.isCompletedSince(reminderId, firingAt)) {
                            NotificationHelper.showReminderNotification(
                                appContext, reminder, profile, escalationLevel
                            )
                            ReminderScheduler.scheduleEscalation(
                                appContext, reminderId, firingAt, escalationLevel + 1, now
                            )
                        }
                    }
                    AlarmKind.FIRE, AlarmKind.SNOOZE -> {
                        if (!onVacation) {
                            NotificationHelper.showReminderNotification(appContext, reminder, profile)
                            ReminderScheduler.scheduleEscalation(appContext, reminderId, now, level = 1, now = now)
                        }
                        // A regular firing schedules the next one, regardless of vacation mode,
                        // so reminders resume on their own once vacation ends. A snooze leaves
                        // the regular schedule alone; it has its own alarm.
                        if (kind == AlarmKind.FIRE) ReminderScheduler.scheduleReminder(appContext, reminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
