package com.aistudio.drinkyourwater.hydra.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Computes when each reminder should next fire and drives AlarmManager. A reminder is not
 * scheduled with a single repeating alarm; instead each firing (see [ReminderAlarmReceiver])
 * re-schedules the following one, since exact repeating alarms aren't reliable on modern
 * Android and this lets the interval/active-day/window rules be re-evaluated every time.
 */
object ReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        cancelReminder(context, reminder.id)

        if (!reminder.isActive) return
        val triggerAt = nextTriggerAtMillis(reminder) ?: return
        val pendingIntent = pendingIntentFor(context, reminder.id)

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntentFor(context, reminderId))
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { scheduleReminder(context, it) }
    }

    private fun pendingIntentFor(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Next timestamp after [fromMillis] at which [reminder] should fire, honoring its
     * scheduled time-of-day (or startTime as a fallback anchor), its repeat interval, its
     * active window (startTime..endTime) and its active weekdays. Returns null if the
     * reminder has no active weekday or a malformed window.
     */
    fun nextTriggerAtMillis(reminder: Reminder, fromMillis: Long = System.currentTimeMillis()): Long? {
        val intervalMinutes = parseIntervalMinutes(reminder).coerceAtLeast(1)
        val activeDays = parseActiveDays(reminder.activeDays)
        if (activeDays.none { it }) return null

        val (anchorHour, anchorMinute) = parseClockTime(reminder.scheduledTime)
            ?: parse24HourTime(reminder.startTime)
            ?: (8 to 0)
        val (endHour, endMinute) = parse24HourTime(reminder.endTime) ?: (22 to 0)

        val base = Calendar.getInstance()
        for (dayOffset in 0..7) {
            val day = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            // Calendar.DAY_OF_WEEK is SUNDAY=1..SATURDAY=7; convert to MONDAY=0..SUNDAY=6.
            val mondayIndexedDay = (day.get(Calendar.DAY_OF_WEEK) + 5) % 7
            if (!activeDays[mondayIndexedDay]) continue

            val dayStart = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, anchorHour)
                set(Calendar.MINUTE, anchorMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayEnd = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dayEnd.timeInMillis < dayStart.timeInMillis) continue

            var slot = dayStart.timeInMillis
            val stepMillis = intervalMinutes * 60_000L
            while (slot <= dayEnd.timeInMillis) {
                if (slot > fromMillis) return slot
                slot += stepMillis
            }
        }
        return null
    }

    private fun parseIntervalMinutes(reminder: Reminder): Int {
        val freq = reminder.frequency.trim().lowercase(Locale.US)
        return when (freq) {
            "30min" -> 30
            "1hr" -> 60
            "2hr" -> 120
            "3hr" -> 180
            "custom" -> reminder.customIntervalMinutes.takeIf { it > 0 } ?: 60
            else -> {
                val hourMatch = Regex("(\\d+)\\s*h").find(freq)
                val minuteMatch = Regex("(\\d+)\\s*m").find(freq)
                when {
                    hourMatch != null -> (hourMatch.groupValues[1].toIntOrNull() ?: 1) * 60
                    minuteMatch != null -> minuteMatch.groupValues[1].toIntOrNull() ?: 60
                    else -> reminder.customIntervalMinutes.takeIf { it > 0 } ?: 60
                }
            }
        }
    }

    private fun parseActiveDays(csv: String): BooleanArray {
        val parts = csv.split(",").map { it.trim().equals("true", ignoreCase = true) }
        return if (parts.size == 7) parts.toBooleanArray() else BooleanArray(7) { true }
    }

    private fun parseClockTime(value: String): Pair<Int, Int>? {
        return try {
            val format = SimpleDateFormat("h:mm a", Locale.US)
            format.isLenient = false
            val date = format.parse(value.trim()) ?: return null
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        } catch (e: ParseException) {
            null
        }
    }

    private fun parse24HourTime(value: String): Pair<Int, Int>? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
