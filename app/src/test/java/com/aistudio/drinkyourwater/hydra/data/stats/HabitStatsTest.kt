package com.aistudio.drinkyourwater.hydra.data.stats

import com.aistudio.drinkyourwater.hydra.data.model.CompletionLog
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {

    // 2024-01-03 is a Wednesday. Times are in the JVM's default time zone, like the app.
    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        set(2024, Calendar.JANUARY, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun log(day: Int, hour: Int) = CompletionLog(
        reminderId = 1, reminderText = "Water", wasScheduledAt = at(day, hour), completedAt = at(day, hour)
    )

    private val now = at(10, 15)

    @Test
    fun `no check-ins means zero everywhere`() {
        val stats = HabitStats.snapshot(emptyList(), emptyList(), now)
        assertEquals(0, stats.completedToday)
        assertEquals(0, stats.plannedToday)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertEquals(0, stats.totalCompletions)
        assertEquals(7, stats.week.size)
        assertTrue(stats.week.all { it.completed == 0 && it.planned == 0 })
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val logs = listOf(log(8, 9), log(9, 9), log(9, 18), log(10, 8))
        assertEquals(3, HabitStats.currentStreak(logs, now))
    }

    @Test
    fun `streak survives until the end of today when today has no check-in yet`() {
        val logs = listOf(log(8, 9), log(9, 9))
        assertEquals(2, HabitStats.currentStreak(logs, now))
    }

    @Test
    fun `a missed day breaks the streak`() {
        val logs = listOf(log(7, 9), log(8, 9))
        assertEquals(0, HabitStats.currentStreak(logs, now))
    }

    @Test
    fun `best streak is the longest run of days`() {
        val logs = listOf(log(1, 9), log(2, 9), log(3, 9), log(3, 20), log(5, 9), log(6, 9))
        assertEquals(3, HabitStats.bestStreak(logs))
    }

    @Test
    fun `today counts only today's check-ins and total counts all`() {
        val logs = listOf(log(9, 9), log(10, 8), log(10, 12))
        val stats = HabitStats.snapshot(logs, emptyList(), now)
        assertEquals(2, stats.completedToday)
        assertEquals(3, stats.totalCompletions)
    }

    @Test
    fun `planned counts daily and interval firings of active reminders`() {
        val daily = Reminder(id = 1, text = "a", frequency = "daily", scheduledTime = "08:00 AM")
        // Every 2 hours from 9:00 to 17:00 -> 9, 11, 13, 15, 17 = 5 firings.
        val interval = Reminder(
            id = 2, text = "b", frequency = "2hr", scheduledTime = "09:00 AM", endTime = "17:00"
        )
        val paused = Reminder(id = 3, text = "c", frequency = "daily", isActive = false)
        val dayStart = HabitStats.startOfDay(now)
        assertEquals(6, HabitStats.plannedCount(listOf(daily, interval, paused), dayStart))
    }

    @Test
    fun `planned skips inactive weekdays`() {
        // Monday-indexed: only Monday is active. 2024-01-10 is a Wednesday.
        val mondayOnly = Reminder(
            id = 1, text = "a", frequency = "daily", activeDays = "true,false,false,false,false,false,false"
        )
        assertEquals(0, HabitStats.plannedCount(listOf(mondayOnly), HabitStats.startOfDay(now)))
        assertEquals(1, HabitStats.plannedCount(listOf(mondayOnly), HabitStats.startOfDay(at(8, 12))))
    }

    @Test
    fun `week is the last seven days, oldest first, ending today`() {
        val logs = listOf(log(4, 9), log(10, 9), log(10, 10))
        val week = HabitStats.week(logs, emptyList(), now)
        assertEquals(HabitStats.startOfDay(at(4, 0)), week.first().dayStartMillis)
        assertEquals(1, week.first().completed)
        assertTrue(week.last().isToday)
        assertEquals(2, week.last().completed)
        assertEquals(1, week.count { it.isToday })
    }
}
