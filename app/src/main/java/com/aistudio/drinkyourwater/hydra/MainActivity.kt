package com.aistudio.drinkyourwater.hydra

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.drinkyourwater.hydra.reminders.NotificationHelper
import com.aistudio.drinkyourwater.hydra.ui.navigation.MainAppNavGraph
import com.aistudio.drinkyourwater.hydra.ui.theme.DrinkYourWaterTheme
import com.aistudio.drinkyourwater.hydra.ui.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WaterViewModel by viewModels()

    private var pendingReminderId by mutableStateOf<Long?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.ensureChannel(this)
        requestRuntimePermissions()
        pendingReminderId = extractReminderId(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DrinkYourWaterTheme {
                MainAppNavGraph(
                    viewModel = viewModel,
                    uiState = uiState,
                    pendingReminderId = pendingReminderId,
                    onPendingReminderConsumed = { pendingReminderId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingReminderId = extractReminderId(intent)
    }

    private fun extractReminderId(intent: Intent?): Long? =
        intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)?.takeIf { it > 0 }

    private fun requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (e: Exception) {
                    // Some OEM builds omit this settings screen; reminders fall back to
                    // inexact alarms in that case (see ReminderScheduler).
                }
            }
        }
    }
}
