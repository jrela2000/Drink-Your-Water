package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.Reminder
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockOverlayScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ReminderSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.builder.FrameworkBuilderFlow
import com.example.ui.viewmodel.WaterUiState
import com.example.ui.viewmodel.WaterViewModel

@Composable
fun MainAppNavGraph(
    viewModel: WaterViewModel,
    uiState: WaterUiState,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val isMainTab = currentRoute in listOf("home", "progress", "settings")

    Scaffold(
        bottomBar = {
            if (isMainTab && uiState.userProfile.onboardingComplete) {
                WaterBottomNav(
                    currentRoute = currentRoute,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.userProfile.onboardingComplete) "home" else "onboarding",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Onboarding Route
            composable("onboarding") {
                OnboardingScreen(
                    onCompleteOnboarding = { style ->
                        viewModel.completeOnboarding(style)
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    onNavigateToBuilder = {
                        navController.navigate("builder")
                    }
                )
            }

            // Home Tab
            composable("home") {
                HomeScreen(
                    userProfile = uiState.userProfile,
                    reminders = uiState.reminders,
                    frameworks = uiState.frameworks,
                    onToggleReminder = { id, isActive -> viewModel.toggleReminder(id, isActive) },
                    onTriggerLockOverlay = { id -> navController.navigate("lock_overlay/$id") },
                    onEditReminder = { id -> navController.navigate("reminder_setup/$id") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToBuilder = { navController.navigate("builder") },
                    onNavigateToActivate = { navController.navigate("builder") },
                    onAddNewReminder = { navController.navigate("reminder_setup/-1") }
                )
            }

            // Progress Tab
            composable("progress") {
                ProgressScreen(
                    userProfile = uiState.userProfile,
                    completionLogs = uiState.completionLogs
                )
            }

            // Settings Tab
            composable("settings") {
                SettingsScreen(
                    userProfile = uiState.userProfile,
                    onUpdateNotificationSound = { viewModel.updateNotificationSound(it) },
                    onUpdateOverlayTheme = { viewModel.updateOverlayTheme(it) },
                    onUpdateMotivationalStyle = { viewModel.updateMotivationalStyle(it) },
                    onUpdateVacationMode = { viewModel.updateVacationMode(it) },
                    onWipeUserDataAndReset = {
                        viewModel.wipeAllData()
                        navController.navigate("onboarding") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // Lock Screen Overlay Route
            composable(
                route = "lock_overlay/{reminderId}",
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: 1L
                val reminder = uiState.reminders.find { it.id == reminderId } ?: Reminder(
                    id = 1,
                    text = "Water Hydration Glass"
                )

                LockOverlayScreen(
                    reminder = reminder,
                    userProfile = uiState.userProfile,
                    onConfirmHydration = { remId, text, snoozeCount ->
                        viewModel.logCompletion(remId, text, snoozeCount)
                        navController.popBackStack()
                    },
                    onSnooze = {
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }

            // Reminder Setup Route
            composable(
                route = "reminder_setup/{reminderId}",
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
                val existing = uiState.reminders.find { it.id == reminderId }

                ReminderSetupScreen(
                    existingReminder = existing,
                    onSaveReminder = { text, time, freq ->
                        val reminderToSave = existing?.copy(
                            text = text,
                            scheduledTime = time,
                            frequency = freq
                        ) ?: Reminder(
                            text = text,
                            scheduledTime = time,
                            frequency = freq
                        )
                        viewModel.toggleReminder(reminderToSave.id, reminderToSave.isActive)
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Framework Builder Route
            composable("builder") {
                FrameworkBuilderFlow(
                    onCompleteBuilderActivate = { name, remindersList, theme, motivationalContent, customMessage ->
                        viewModel.createFrameworkWithReminders(
                            name = name,
                            reminders = remindersList,
                            theme = theme,
                            motivationalContent = motivationalContent,
                            customMessage = customMessage,
                            isPremiumActivate = true,
                            onComplete = {
                                navController.navigate("home") {
                                    popUpTo("builder") { inclusive = true }
                                }
                            }
                        )
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
