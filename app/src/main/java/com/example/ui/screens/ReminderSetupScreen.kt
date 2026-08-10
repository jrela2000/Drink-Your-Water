package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Reminder
import com.example.ui.components.RippleButton
import com.example.ui.theme.FreshBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSetupScreen(
    existingReminder: Reminder?,
    onSaveReminder: (String, String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var reminderText by remember { mutableStateOf(existingReminder?.text ?: "Water Hydration") }
    var scheduledTime by remember { mutableStateOf(existingReminder?.scheduledTime ?: "10:00 AM") }
    var frequency by remember { mutableStateOf(existingReminder?.frequency ?: "1hr") }

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
                    label = { Text("Scheduled Time (e.g. 10:30 AM)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_time_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FreshBlue,
                        focusedLabelColor = FreshBlue
                    )
                )

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency Interval (e.g., 30min, 1hr, 2hr)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_frequency_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FreshBlue,
                        focusedLabelColor = FreshBlue
                    )
                )
            }

            RippleButton(
                onClick = {
                    if (reminderText.isNotBlank()) {
                        onSaveReminder(reminderText, scheduledTime, frequency)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_reminder_button"
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}
