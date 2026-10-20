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

    private const val SNOOZE_MINUTES = 10
    const val ESCALATION_MINUTES = 10
    const val MAX_ESCALATIONS = 3
    const val EXTRA_ESCALATION_LEVEL = "extra_escalation_level"
    const val EXTRA_FIRING_AT = "extra_firing_at"

    /** Fires once a day at the reminder's time. The default for every reminder. */
    const val FREQUENCY_DAILY = "daily"

    /** Frequencies offered in the reminder editor, as (stored value, label). */
    val frequencyOptions = listOf(
        FREQUENCY_DAILY to "Once a day",
        "30min" to "Every 30 min",
        "1hr" to "Every hour",
        "2hr" to "Every 2 hours",
        "3hr" to "Every 3 hours"
    )

    fun frequencyLabel(frequency: String): String =
        frequencyOptions.firstOrNull { it.first == frequency.trim().lowercase(Locale.US) }?.second
            ?: "Every $frequency"

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleReminder(context: Context, reminder: Reminder) {
        cancelAlarm(context, reminder.id, AlarmKind.FIRE)
        if (!reminder.isActive) return
        val triggerAt = nextTriggerAtMillis(reminder) ?: return
        setAlarm(context, reminder.id, triggerAt, AlarmKind.FIRE)
    }

    /**
     * Re-fires this reminder in [SNOOZE_MINUTES]. Snoozes use their own alarm, so the next
     * regular firing stays scheduled and re-syncing alarms on app open can't drop a snooze.
     */
    fun scheduleSnooze(context: Context, reminder: Reminder) {
        cancelAlarm(context, reminder.id, AlarmKind.ESCALATE)
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        setAlarm(context, reminder.id, triggerAt, AlarmKind.SNOOZE)
    }

    /**
     * Escalation: a check-in that is neither confirmed nor snoozed re-alerts every
     * [ESCALATION_MINUTES], up to [MAX_ESCALATIONS] times. [level] is the re-alert about to be
     * scheduled (1..MAX); [firingAt] is when the prompt being escalated went off, so the
     * receiver can tell whether it was confirmed since.
     */
    fun scheduleEscalation(
        context: Context,
        reminderId: Long,
        firingAt: Long,
        level: Int,
        now: Long = System.currentTimeMillis()
    ) {
        val triggerAt = nextEscalationAtMillis(level, now) ?: return
        setAlarm(context, reminderId, triggerAt, AlarmKind.ESCALATE) {
            putExtra(EXTRA_ESCALATION_LEVEL, level)
            putExtra(EXTRA_FIRING_AT, firingAt)
        }
    }

    /** When re-alert number [level] should go off, or null once escalation is exhausted. */
    fun nextEscalationAtMillis(level: Int, now: Long): Long? =
        if (level in 1..MAX_ESCALATIONS) now + ESCALATION_MINUTES * 60_000L else null

    /** Stops any pending snooze and re-alerts once a check-in is confirmed. */
    fun clearPendingCheckIn(context: Context, reminderId: Long) {
        cancelAlarm(context, reminderId, AlarmKind.SNOOZE)
        cancelAlarm(context, reminderId, AlarmKind.ESCALATE)
    }

    private fun setAlarm(
        context: Context,
        reminderId: Long,
        triggerAt: Long,
        kind: AlarmKind,
        extras: Intent.() -> Unit = {}
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntentFor(context, reminderId, kind, extras)
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

    private fun cancelAlarm(context: Context, reminderId: Long, kind: AlarmKind) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntentFor(context, reminderId, kind))
    }

    /** Cancels everything scheduled for a reminder (turned off, deleted, or data wiped). */
    fun cancelReminder(context: Context, reminderId: Long) {
        AlarmKind.values().forEach { cancelAlarm(context, reminderId, it) }
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { scheduleReminder(context, it) }
    }

    /**
     * The three alarms a reminder can have pending at once. Each has its own intent action,
     * which makes them distinct PendingIntents (extras don't count for matching). Alarms set
     * by older builds carry no action and are treated as [FIRE].
     */
    enum class AlarmKind(val action: String) {
        FIRE("com.aistudio.drinkyourwater.hydra.action.REMINDER_FIRE"),
        SNOOZE("com.aistudio.drinkyourwater.hydra.action.REMINDER_SNOOZE"),
        ESCALATE("com.aistudio.drinkyourwater.hydra.action.REMINDER_ESCALATE");

        companion object {
            fun fromAction(action: String?): AlarmKind = values().firstOrNull { it.action == action } ?: FIRE
        }
    }

    private fun pendingIntentFor(
        context: Context,
        reminderId: Long,
        kind: AlarmKind,
        extras: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = kind.action
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            extras()
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
     * scheduled time-of-day (or startTime as a fallback anchor), its active weekdays and,
     * for interval reminders, its repeat interval up to endTime. A [FREQUENCY_DAILY]
     * reminder fires only at its time. Returns null if the reminder has no active weekday
     * or a malformed window.
     */
    fun nextTriggerAtMillis(reminder: Reminder, fromMillis: Long = System.currentTimeMillis()): Long? {
        val isDaily = reminder.frequency.trim().equals(FREQUENCY_DAILY, ignoreCase = true)
        val intervalMinutes = parseIntervalMinutes(reminder).coerceAtLeast(1)
        val activeDays = parseActiveDays(reminder.activeDays)
        if (activeDays.none { it }) return null

        val (anchorHour, anchorMinute) = parseClockTime(reminder.scheduledTime)
            ?: parse24HourTime(reminder.startTime)
            ?: (8 to 0)
        val (endHour, endMinute) = parse24HourTime(reminder.endTime) ?: (22 to 0)

        val base = Calendar.getInstance().apply { timeInMillis = fromMillis }
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
            if (isDaily) {
                if (dayStart.timeInMillis > fromMillis) return dayStart.timeInMillis
                continue
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

    /** Parses "h:mm AM/PM" (e.g. "9:30 PM", "09:30 pm"); null if it isn't a valid time. */
    fun parseClockTime(value: String): Pair<Int, Int>? {
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
