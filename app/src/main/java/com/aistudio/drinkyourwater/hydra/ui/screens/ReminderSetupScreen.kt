package com.aistudio.drinkyourwater.hydra.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.reminders.ReminderScheduler
import com.aistudio.drinkyourwater.hydra.ui.components.RippleButton
import com.aistudio.drinkyourwater.hydra.ui.theme.FreshBlue
import java.util.Locale

/** "9:30 pm" -> "09:30 PM", matching how seeded reminders store their time; null if invalid. */
internal fun normalizeClockTime(input: String): String? {
    val (hour, minute) = ReminderScheduler.parseClockTime(input) ?: return null
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.US, "%02d:%02d %s", h12, minute, if (hour < 12) "AM" else "PM")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSetupScreen(
    existingReminder: Reminder?,
    onSaveReminder: (String, String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var reminderText by remember { mutableStateOf(existingReminder?.text ?: "Water Hydration") }
    var scheduledTime by remember { mutableStateOf(existingReminder?.scheduledTime ?: "10:00 AM") }
    var frequency by remember {
        mutableStateOf(existingReminder?.frequency ?: ReminderScheduler.FREQUENCY_DAILY)
    }

    val normalizedTime = normalizeClockTime(scheduledTime)
    val canSave = reminderText.isNotBlank() && normalizedTime != null

    // Keep an unusual stored value (e.g. "45min" from an older build) selectable as-is.
    val frequencyOptions = ReminderScheduler.frequencyOptions.let { options ->
        if (options.any { it.first == frequency }) options
        else options + (frequency to ReminderScheduler.frequencyLabel(frequency))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingReminder != null) "Edit Reminder" else "New Hydration Reminder",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = reminderText,
                    onValueChange = { reminderText = it },
                    label = { Text("Reminder Title / Habit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_text_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FreshBlue,
                        focusedLabelColor = FreshBlue
                    )
                )

                OutlinedTextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = { Text("Time (e.g. 10:30 AM)") },
                    isError = normalizedTime == null,
                    supportingText = {
                        if (normalizedTime == null) Text("Enter a time like 9:00 AM or 2:30 PM")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_time_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FreshBlue,
                        focusedLabelColor = FreshBlue
                    )
                )

                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("reminder_frequency_options"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frequencyOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("frequency_option_$value")
                        )
                    }
                }
                if (frequency != ReminderScheduler.FREQUENCY_DAILY) {
                    Text(
                        text = "Repeats from this time until 10:00 PM on active days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RippleButton(
                onClick = {
                    if (canSave && normalizedTime != null) {
                        onSaveReminder(reminderText.trim(), normalizedTime, frequency)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                testTag = "save_reminder_button"
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}
