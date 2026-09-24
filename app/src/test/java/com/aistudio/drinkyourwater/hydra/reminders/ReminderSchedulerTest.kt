package com.aistudio.drinkyourwater.hydra.reminders

import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {

    private fun calendarAt(hour: Int, minute: Int, dayOfMonth: Int = 1): Calendar =
        Calendar.getInstance().apply {
            // 2024-01-01 is a Monday; used as a fixed, known week for all cases below.
            set(2024, Calendar.JANUARY, dayOfMonth, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun reminder(
        scheduledTime: String = "09:00 AM",
        frequency: String = "1hr",
        customIntervalMinutes: Int = 60,
        startTime: String = "08:00",
        endTime: String = "22:00",
        activeDays: String = "true,true,true,true,true,true,true"
    ) = Reminder(
        text = "Test reminder",
        frequency = frequency,
        customIntervalMinutes = customIntervalMinutes,
        startTime = startTime,
        endTime = endTime,
        activeDays = activeDays,
        scheduledTime = scheduledTime
    )

    @Test
    fun `next slot within the same day steps by the interval from scheduledTime`() {
        val from = calendarAt(9, 30).timeInMillis // Monday 9:30am, after the 9:00 slot

        val trigger = ReminderScheduler.nextTriggerAtMillis(reminder(), from)

        assertEquals(calendarAt(10, 0).timeInMillis, trigger)
    }

    @Test
    fun `after the last slot of the day rolls to the next active day's first slot`() {
        val from = calendarAt(22, 30).timeInMillis // Monday 10:30pm, after the 10pm slot

        val trigger = ReminderScheduler.nextTriggerAtMillis(reminder(), from)

        assertEquals(calendarAt(9, 0, dayOfMonth = 2).timeInMillis, trigger) // Tuesday 9am
    }

    @Test
    fun `an inactive weekday is skipped in favor of the next active one`() {
        val mondayInactive = reminder(activeDays = "false,true,true,true,true,true,true")
        val from = calendarAt(9, 30).timeInMillis // Monday 9:30am

        val trigger = ReminderScheduler.nextTriggerAtMillis(mondayInactive, from)

        assertEquals(calendarAt(9, 0, dayOfMonth = 2).timeInMillis, trigger) // Tuesday 9am
    }

    @Test
    fun `no active weekdays at all returns null instead of looping forever`() {
        val neverActive = reminder(activeDays = "false,false,false,false,false,false,false")
        val from = calendarAt(9, 30).timeInMillis

        val trigger = ReminderScheduler.nextTriggerAtMillis(neverActive, from)

        assertNull(trigger)
    }

    @Test
    fun `custom frequency uses customIntervalMinutes`() {
        val everyFifteen = reminder(frequency = "custom", customIntervalMinutes = 15)
        val from = calendarAt(9, 30).timeInMillis

        val trigger = ReminderScheduler.nextTriggerAtMillis(everyFifteen, from)

        assertEquals(calendarAt(9, 45).timeInMillis, trigger)
    }

    @Test
    fun `freeform frequency text like 45min is parsed`() {
        val freeform = reminder(frequency = "45min")
        val from = calendarAt(9, 30).timeInMillis

        val trigger = ReminderScheduler.nextTriggerAtMillis(freeform, from)

        assertEquals(calendarAt(9, 45).timeInMillis, trigger)
    }
}
