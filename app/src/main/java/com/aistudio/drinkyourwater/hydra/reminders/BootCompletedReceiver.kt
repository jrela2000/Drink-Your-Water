package com.aistudio.drinkyourwater.hydra.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.drinkyourwater.hydra.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Exact alarms set with AlarmManager do not survive a device reboot, so every active
 * reminder must be re-scheduled once the system (and this app's alarms) come back up.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val activeReminders = db.reminderDao().getAllRemindersDirect().filter { it.isActive }
                ReminderScheduler.rescheduleAll(appContext, activeReminders)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
