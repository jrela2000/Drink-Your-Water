package com.aistudio.drinkyourwater.hydra.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.drinkyourwater.hydra.data.db.AppDatabase
import com.aistudio.drinkyourwater.hydra.data.repository.WaterRepository
import com.aistudio.drinkyourwater.hydra.reminders.ReminderScheduler
import com.aistudio.drinkyourwater.hydra.ui.screens.normalizeClockTime
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderDataTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val migrationDbName = "migration-test.db"
    private var openDb: AppDatabase? = null

    @After
    fun tearDown() {
        openDb?.close()
        context.deleteDatabase(migrationDbName)
    }

    private fun inMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { openDb = it }

    /** Builds a database exactly as version 1 of the app wrote it, from the exported schema. */
    private fun createVersion1Database(rows: List<String>) {
        val schema = JSONObject(
            File("schemas/com.aistudio.drinkyourwater.hydra.data.db.AppDatabase/1.json").readText()
        ).getJSONObject("database")
        val file = context.getDatabasePath(migrationDbName).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        rows.forEach { db.execSQL(it) }
        db.version = 1
        db.close()
    }

    private fun reminderRow(id: Int, frequency: String, frameworkId: String, snoozeCount: Int) =
        "INSERT INTO reminders (id, text, frequency, customIntervalMinutes, startTime, endTime, " +
            "activeDays, isActive, snoozeCount, frameworkId, scheduledTime) VALUES ($id, 'R$id', " +
            "'$frequency', 60, '08:00', '22:00', 'true,true,true,true,true,true,true', 1, " +
            "$snoozeCount, $frameworkId, '09:00 AM')"

    @Test
    fun `migration 1 to 2 makes default and builder reminders daily and clears stuck snoozes`() {
        createVersion1Database(
            listOf(
                reminderRow(1, "1hr", "1", 2),     // seeded water reminder
                reminderRow(2, "1hr", "7", 0),     // framework-builder reminder
                reminderRow(3, "1hr", "NULL", 2),  // added by hand with the 1hr interval
                reminderRow(4, "30min", "1", 1)    // seeded reminder the user changed
            )
        )

        val db = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
            .also { openDb = it }
        val byId = runBlocking { db.reminderDao().getAllRemindersDirect() }.associateBy { it.id }

        assertEquals("daily", byId.getValue(1).frequency)
        assertEquals("daily", byId.getValue(2).frequency)
        assertEquals("1hr", byId.getValue(3).frequency)
        assertEquals("30min", byId.getValue(4).frequency)
        assertTrue(byId.values.all { it.snoozeCount == 0 })
    }

    @Test
    fun `fresh install gets six once-a-day water reminders`() {
        val db = inMemoryDb()
        runBlocking { WaterRepository(db).ensureInitialDataSeeded() }
        val reminders = runBlocking { db.reminderDao().getAllRemindersDirect() }

        assertEquals(6, reminders.size)
        assertTrue(reminders.all { it.frequency == ReminderScheduler.FREQUENCY_DAILY })

        // Count the alerts one full Monday produces: one per reminder.
        val dayStart = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L
        val alerts = reminders.sumOf { reminder ->
            var count = 0
            var from = dayStart - 1
            while (true) {
                val next = ReminderScheduler.nextTriggerAtMillis(reminder, from) ?: break
                if (next >= dayEnd) break
                count++
                from = next
            }
            count
        }
        assertEquals(6, alerts)
    }

    @Test
    fun `framework builder reminders fire once a day`() {
        val db = inMemoryDb()
        val repository = WaterRepository(db)
        val frameworkId = runBlocking {
            repository.createFrameworkWithReminders(
                frameworkName = "Vitamins",
                remindersList = listOf("09:00 AM" to "Morning vitamins"),
                overlayTheme = "midnight-water",
                motivationalContent = "affirmations",
                customMessage = ""
            )
        }
        val reminders = runBlocking { repository.getRemindersForFrameworkOnce(frameworkId) }
        assertEquals(listOf(ReminderScheduler.FREQUENCY_DAILY), reminders.map { it.frequency })
    }

    @Test
    fun `a new firing restores the full snooze allowance`() {
        val db = inMemoryDb()
        val repository = WaterRepository(db)
        runBlocking {
            repository.ensureInitialDataSeeded()
            repository.incrementSnoozeCount(1)
            repository.incrementSnoozeCount(1)
        }
        assertEquals(2, runBlocking { repository.getReminderById(1) }?.snoozeCount)

        val reset = runBlocking { repository.startNewFiring(1) }

        assertEquals(0, reset?.snoozeCount)
        assertEquals(0, runBlocking { repository.getReminderById(1) }?.snoozeCount)
    }

    @Test
    fun `reminder editor normalizes typed times and rejects bad ones`() {
        assertEquals("09:30 PM", normalizeClockTime("9:30 pm"))
        assertEquals("12:00 AM", normalizeClockTime("12:00 AM"))
        assertNull(normalizeClockTime("10:30"))
        assertNull(normalizeClockTime(""))
    }
}
