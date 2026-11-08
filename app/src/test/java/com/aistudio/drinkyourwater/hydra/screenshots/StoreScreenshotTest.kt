package com.aistudio.drinkyourwater.hydra.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.drinkyourwater.hydra.data.model.CompletionLog
import com.aistudio.drinkyourwater.hydra.data.model.Framework
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.data.model.UserProfile
import com.aistudio.drinkyourwater.hydra.data.stats.HabitStats
import com.aistudio.drinkyourwater.hydra.ui.navigation.WaterBottomNav
import com.aistudio.drinkyourwater.hydra.ui.screens.HomeScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.LockOverlayScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.OnboardingScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.ProgressScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.SettingsScreen
import com.aistudio.drinkyourwater.hydra.ui.theme.DrinkYourWaterTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens with sample data and writes Google Play phone screenshots to
 * app/build/store-screenshots. Nothing is written in a normal test run; the Android
 * Screenshots workflow turns recording on with -Proborazzi.test.record=true.
 *
 * 360x720dp at xxhdpi is 1080x2160 px: 2:1, the tallest ratio Play accepts for phones.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h720dp-port-xxhdpi")
class StoreScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val today = HabitStats.startOfDay(now)

    private val profile = UserProfile(onboardingComplete = true)

    private val water = Framework(
        id = 1,
        name = "Water Hydration",
        motivationalContent = "waterFacts",
        customMessage = "Drink a glass of cold fresh water now!",
        status = "active",
        isWater = true
    )

    private val reminders = listOf(
        "Morning Hydration (First Glass)" to "08:00 AM",
        "Post-Breakfast Water Refresh" to "10:30 AM",
        "Mid-Day Hydration Boost" to "01:00 PM",
        "Afternoon Energy Hydration" to "03:30 PM",
        "Pre-Dinner Glass of Water" to "06:30 PM",
        "Evening Wind-Down Hydration" to "09:00 PM"
    ).mapIndexed { i, (text, time) ->
        Reminder(id = i + 1L, text = text, frequency = "daily", scheduledTime = time, frameworkId = 1)
    }

    /**
     * A 15-day streak with a few missed check-ins along the way, then a gap and an older
     * week. Today's check-ins are placed before "now" so history never shows the future.
     */
    private val logs: List<CompletionLog> = buildList {
        val perDay = listOf(4, 6, 5, 6, 4, 6, 5, 6, 6, 5, 4, 6, 5, 6, 5, 0, 5, 6, 4, 5, 6, 5)
        val hours = listOf(8 to 5, 10 to 40, 13 to 10, 15 to 45, 18 to 35, 21 to 10)
        perDay.forEachIndexed { back, count ->
            val day = HabitStats.addDays(today, -back)
            repeat(count) { i ->
                val at = if (back == 0) {
                    (now - (i + 1) * 20 * 60_000L).coerceAtLeast(day)
                } else {
                    day + hours[i].first * 3_600_000L + hours[i].second * 60_000L
                }
                add(
                    CompletionLog(
                        id = size + 1L,
                        reminderId = i + 1L,
                        reminderText = reminders[i].text,
                        wasScheduledAt = at,
                        completedAt = at
                    )
                )
            }
        }
    }

    @Test fun onboardingLight() = shoot("01-onboarding", dark = false) { OnboardingScreen({}) }

    @Test fun homeLight() = shoot("02-home", dark = false) { withNav("home") { home() } }

    @Test fun checkInLight() = shoot("03-check-in", dark = false) { checkIn() }

    @Test fun progressLight() = shoot("04-progress", dark = false) {
        withNav("progress") { ProgressScreen(reminders = reminders, completionLogs = logs) }
    }

    @Test fun settingsLight() = shoot("05-settings", dark = false) { withNav("settings") { settings() } }

    @Test fun homeDark() = shoot("06-home-dark", dark = true) { withNav("home") { home() } }

    @Test fun checkInDark() = shoot("07-check-in-dark", dark = true) { checkIn() }

    @Composable
    private fun home() = HomeScreen(
        userProfile = profile,
        reminders = reminders,
        frameworks = listOf(water),
        completionLogs = logs,
        onToggleReminder = { _, _ -> },
        onTriggerLockOverlay = {},
        onEditReminder = {},
        onNavigateToSettings = {},
        onAddNewReminder = {}
    )

    @Composable
    private fun checkIn() = LockOverlayScreen(
        reminder = reminders[2],
        userProfile = profile,
        onConfirmHydration = { _, _, _ -> },
        onSnooze = {}
    )

    @Composable
    private fun settings() = SettingsScreen(
        userProfile = profile,
        onUpdateNotificationSound = {},
        onUpdateOverlayTheme = {},
        onUpdateMotivationalStyle = {},
        onUpdateVacationMode = {},
        onWipeUserDataAndReset = {}
    )

    @Composable
    private fun withNav(route: String, content: @Composable () -> Unit) {
        Scaffold(bottomBar = { WaterBottomNav(currentRoute = route, onNavigateToTab = {}) }) { inner ->
            Box(Modifier.padding(inner)) { content() }
        }
    }

    private fun shoot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent { DrinkYourWaterTheme(darkTheme = dark) { content() } }
        composeRule.waitForIdle()
        // Let entrance animations settle before the capture.
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.onRoot().captureRoboImage("build/store-screenshots/$name.png")
    }
}
