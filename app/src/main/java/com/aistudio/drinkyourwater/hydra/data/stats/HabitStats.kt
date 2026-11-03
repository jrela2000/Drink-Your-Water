package com.aistudio.drinkyourwater.hydra.data.stats

import com.aistudio.drinkyourwater.hydra.data.model.CompletionLog
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.reminders.ReminderScheduler
import java.util.Calendar
import java.util.Locale

/** One day in the weekly chart. */
data class DaySummary(
    val dayStartMillis: Long,
    val label: String,
    val completed: Int,
    val planned: Int,
    val isToday: Boolean
)

/** Everything the Home and Progress screens show, derived from the check-in log. */
data class StatsSnapshot(
    val completedToday: Int,
    val plannedToday: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val week: List<DaySummary>
)

/**
 * Stats computed from the completion log, so they can never drift out of sync with it
 * (matches the iOS app's HabitStats). A day counts toward a streak when at least one
 * check-in was confirmed that day. Days use the device's time zone.
 */
object HabitStats {

    fun snapshot(logs: List<CompletionLog>, reminders: List<Reminder>, now: Long): StatsSnapshot {
        val today = startOfDay(now)
        return StatsSnapshot(
            completedToday = completionsOn(today, logs),
            plannedToday = plannedCount(reminders, today),
            currentStreak = currentStreak(logs, now),
            bestStreak = bestStreak(logs),
            totalCompletions = logs.size,
            week = week(logs, reminders, now)
        )
    }

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Start of the day [days] away from the day starting at [dayStart] (DST-safe). */
    fun addDays(dayStart: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayStart
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    fun completionsOn(dayStart: Long, logs: List<CompletionLog>): Int {
        val next = addDays(dayStart, 1)
        return logs.count { it.completedAt in dayStart until next }
    }

    /**
     * Consecutive days with a check-in, ending today. If nothing is logged yet today the
     * streak is still alive and counts back from yesterday.
     */
    fun currentStreak(logs: List<CompletionLog>, now: Long): Int {
        val days = logs.mapTo(HashSet()) { startOfDay(it.completedAt) }
        var day = startOfDay(now)
        if (day !in days) day = addDays(day, -1)
        var streak = 0
        while (day in days) {
            streak++
            day = addDays(day, -1)
        }
        return streak
    }

    fun bestStreak(logs: List<CompletionLog>): Int {
        val days = logs.map { startOfDay(it.completedAt) }.distinct().sorted()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in days) {
            run = if (previous != null && addDays(previous, 1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    /** How many check-ins the active reminders schedule on the day starting at [dayStart]. */
    fun plannedCount(reminders: List<Reminder>, dayStart: Long): Int {
        val next = addDays(dayStart, 1)
        return reminders.filter { it.isActive }.sumOf { reminder ->
            var count = 0
            var from = dayStart - 1
            while (true) {
                val at = ReminderScheduler.nextTriggerAtMillis(reminder, from) ?: break
                if (at >= next) break
                count++
                from = at
            }
            count
        }
    }

    /** The last seven days ending today, oldest first. */
    fun week(logs: List<CompletionLog>, reminders: List<Reminder>, now: Long): List<DaySummary> {
        val today = startOfDay(now)
        return (6 downTo 0).map { back ->
            val day = addDays(today, -back)
            val label = Calendar.getInstance().apply { timeInMillis = day }
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
            DaySummary(
                dayStartMillis = day,
                label = label,
                completed = completionsOn(day, logs),
                planned = plannedCount(reminders, day),
                isToday = back == 0
            )
        }
    }
}
