package com.aistudio.drinkyourwater.hydra.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.billing.FrameworkAccess
import com.aistudio.drinkyourwater.hydra.billing.PurchaseResult
import com.aistudio.drinkyourwater.hydra.ui.screens.HomeScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.PaywallScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.LockOverlayScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.OnboardingScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.ProgressScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.ReminderSetupScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.SettingsScreen
import com.aistudio.drinkyourwater.hydra.ui.screens.builder.FrameworkBuilderFlow
import com.aistudio.drinkyourwater.hydra.ui.viewmodel.WaterUiState
import com.aistudio.drinkyourwater.hydra.ui.viewmodel.WaterViewModel

@Composable
fun MainAppNavGraph(
    viewModel: WaterViewModel,
    uiState: WaterUiState,
    navController: NavHostController = rememberNavController(),
    pendingReminderId: Long? = null,
    onPendingReminderConsumed: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val strictMode by viewModel.strictMode.collectAsState()
    val billing by viewModel.billingState.collectAsState()
    val context = LocalContext.current
    var paywallBusy by remember { mutableStateOf(false) }
    var paywallMessage by remember { mutableStateOf<String?>(null) }
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val isMainTab = currentRoute in listOf("home", "progress", "settings")

    // Opens the lock overlay directly when the app was launched (or resumed) from a
    // reminder notification tap.
    LaunchedEffect(pendingReminderId) {
        if (pendingReminderId != null) {
            navController.navigate("lock_overlay/$pendingReminderId")
            onPendingReminderConsumed()
        }
    }

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
                    moreTypesAvailable = billing.isAvailable
                )
            }

            // Home Tab
            composable("home") {
                HomeScreen(
                    userProfile = uiState.userProfile,
                    reminders = uiState.reminders,
                    frameworks = uiState.frameworks,
                    completionLogs = uiState.completionLogs,
                    onToggleReminder = { id, isActive -> viewModel.toggleReminder(id, isActive) },
                    onTriggerLockOverlay = { id -> navController.navigate("lock_overlay/$id") },
                    onEditReminder = { id -> navController.navigate("reminder_setup/$id") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onAddNewReminder = { navController.navigate("reminder_setup/-1") },
                    billing = billing,
                    onNavigateToBuilder = {
                        val active = uiState.frameworks.count { !it.isWater && it.status == "active" }
                        // Subscribers with room left go straight to the builder; everyone else sees plans.
                        navController.navigate(if (FrameworkAccess.canCreate(billing.tier, active)) "builder" else "paywall")
                    }
                )
            }

            // Progress Tab
            composable("progress") {
                ProgressScreen(
                    reminders = uiState.reminders,
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
                    },
                    strictMode = strictMode,
                    onUpdateStrictMode = { viewModel.setStrictMode(it) },
                    billing = billing,
                    onOpenPaywall = { navController.navigate("paywall") }
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
                    pinScreen = strictMode,
                    reminder = reminder,
                    userProfile = uiState.userProfile,
                    onConfirmHydration = { remId, text, snoozeCount ->
                        viewModel.logCompletion(remId, text, snoozeCount)
                        navController.popBackStack()
                    },
                    onSnooze = { remId ->
                        viewModel.snoozeReminder(remId)
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
                        viewModel.saveReminder(reminderToSave)
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Subscription plans. Only reachable when billing is configured.
            composable("paywall") {
                PaywallScreen(
                    billing = billing,
                    busy = paywallBusy,
                    message = paywallMessage,
                    onSubscribe = { plan ->
                        val activity = context as? Activity ?: return@PaywallScreen
                        paywallBusy = true
                        paywallMessage = null
                        viewModel.purchase(activity, plan) { result ->
                            paywallBusy = false
                            when (result) {
                                is PurchaseResult.Success -> navController.popBackStack()
                                is PurchaseResult.Failed -> paywallMessage = result.reason
                                PurchaseResult.Cancelled -> Unit
                            }
                        }
                    },
                    onRestore = {
                        paywallBusy = true
                        paywallMessage = null
                        viewModel.restorePurchases { result ->
                            paywallBusy = false
                            when (result) {
                                is PurchaseResult.Success ->
                                    if (result.tier != null) navController.popBackStack()
                                    else paywallMessage = "No previous purchase found for this Google account."
                                is PurchaseResult.Failed -> paywallMessage = result.reason
                                PurchaseResult.Cancelled -> Unit
                            }
                        }
                    },
                    onManageSubscription = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Framework Builder Route: subscription-gated; the ViewModel re-checks before saving.
            composable("builder") {
                FrameworkBuilderFlow(
                    onCompleteBuilderActivate = { name, remindersList, theme, motivationalContent, customMessage ->
                        viewModel.createFrameworkWithReminders(
                            name = name,
                            reminders = remindersList,
                            theme = theme,
                            motivationalContent = motivationalContent,
                            customMessage = customMessage,
                            onBlocked = { navController.navigate("paywall") { popUpTo("builder") { inclusive = true } } },
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
