package com.example.ui.screens.builder

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PhonePreview
import com.example.ui.components.RippleButton
import com.example.ui.theme.ConfirmGreen
import com.example.ui.theme.FreshBlue
import com.example.ui.theme.IceTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameworkBuilderFlow(
    onCompleteBuilderActivate: (
        name: String,
        reminders: List<Pair<String, String>>,
        theme: String,
        motivationalContent: String,
        customMessage: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var frameworkName by remember { mutableStateOf("") }
    val remindersList = remember {
        mutableStateListOf(
            "09:00 AM" to "Morning Habit Lock Screen",
            "02:00 PM" to "Afternoon Practice Lock Screen"
        )
    }
    var overlayTheme by remember { mutableStateOf("midnight-water") }
    var motivationalContent by remember { mutableStateOf("affirmations") }
    var customMessage by remember { mutableStateOf("Take a deep breath and complete your habit.") }
    var selectedTier by remember { mutableStateOf("builder") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (step) {
                            1 -> "Framework Name"
                            2 -> "Habit Reminders"
                            3 -> "Overlay Preview"
                            4 -> "Review Framework"
                            else -> "Activate Subscription"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step-- else onNavigateBack()
                    }) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                (1..5).forEach { i ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (i == step) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == step) FreshBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "builder_step"
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1Name(
                        name = frameworkName,
                        onNameChange = { if (it.length <= 30) frameworkName = it },
                        onNext = { if (frameworkName.isNotBlank()) step = 2 }
                    )
                    2 -> Step2Reminders(
                        reminders = remindersList,
                        onAddReminder = {
                            if (remindersList.size < 5) {
                                remindersList.add("06:00 PM" to "Evening Habit Lock Screen")
                            }
                        },
                        onRemoveReminder = { idx ->
                            if (remindersList.size > 1) remindersList.removeAt(idx)
                        },
                        onUpdateReminder = { idx, time, text ->
                            remindersList[idx] = time to text
                        },
                        onNext = { step = 3 }
                    )
                    3 -> Step3Overlay(
                        frameworkName = frameworkName,
                        theme = overlayTheme,
                        onThemeSelect = { overlayTheme = it },
                        motivationalContent = motivationalContent,
                        onMotivationalSelect = { motivationalContent = it },
                        customMessage = customMessage,
                        onCustomMessageChange = { customMessage = it },
                        onNext = { step = 4 }
                    )
                    4 -> Step4Review(
                        name = frameworkName,
                        reminders = remindersList,
                        theme = overlayTheme,
                        motivationalContent = motivationalContent,
                        customMessage = customMessage,
                        onNext = { step = 5 }
                    )
                    5 -> Step5Activate(
                        selectedTier = selectedTier,
                        onSelectTier = { selectedTier = it },
                        onActivate = {
                            onCompleteBuilderActivate(
                                frameworkName,
                                remindersList.toList(),
                                overlayTheme,
                                motivationalContent,
                                customMessage
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1Name(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val suggestions = listOf("Medication Schedule", "Post-Study Movement", "Daily Prayer & Grace", "Vitamin & Supplement Routine", "Deep Focus Reset")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Name Your Custom Habit Framework",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Framework Name (30-char max)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("builder_name_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FreshBlue,
                    focusedLabelColor = FreshBlue
                )
            )

            Text(
                text = "Popular Framework Ideas:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNameChange(chip) }
                            .padding(12.dp)
                            .testTag("suggestion_chip_$chip")
                    ) {
                        Text(text = chip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
            testTag = "builder_step1_next"
        ) {
            Text("Next: Add Reminders", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step2Reminders(
    reminders: List<Pair<String, String>>,
    onAddReminder: () -> Unit,
    onRemoveReminder: (Int) -> Unit,
    onUpdateReminder: (Int, String, String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Set Daily Lock Overlay Schedule",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Up to 5 daily reminders (time & prompt message)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(reminders) { idx, pair ->
                    val (time, text) = pair
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = { onUpdateReminder(idx, it, text) },
                                    label = { Text("Time") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { onUpdateReminder(idx, time, it) },
                                    label = { Text("Reminder Message") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            if (reminders.size > 1) {
                                IconButton(onClick = { onRemoveReminder(idx) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (reminders.size < 5) {
                    item {
                        RippleButton(
                            onClick = onAddReminder,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "add_reminder_row_btn"
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Reminder Row (${reminders.size}/5)")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "builder_step2_next"
        ) {
            Text("Next: Customize Overlay", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step3Overlay(
    frameworkName: String,
    theme: String,
    onThemeSelect: (String) -> Unit,
    motivationalContent: String,
    onMotivationalSelect: (String) -> Unit,
    customMessage: String,
    onCustomMessageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Live Phone Overlay Preview",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhonePreview(
                frameworkName = frameworkName,
                theme = theme,
                motivationalText = motivationalContent,
                customMessage = customMessage,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customMessage,
                onValueChange = onCustomMessageChange,
                label = { Text("Custom Motivational Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_overlay_msg_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "builder_step3_next"
        ) {
            Text("Next: Review Framework", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step4Review(
    name: String,
    reminders: List<Pair<String, String>>,
    theme: String,
    motivationalContent: String,
    customMessage: String,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Review Framework Summary",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = FreshBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Theme: $theme • Style: $motivationalContent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Scheduled Lock Overlays:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    reminders.forEach { (time, text) ->
                        Text(text = "• $time: $text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "builder_step4_next"
        ) {
            Text("Proceed to Activation & Paywall", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step5Activate(
    selectedTier: String,
    onSelectTier: (String) -> Unit,
    onActivate: () -> Unit
) {
    val tiers = listOf(
        "starter" to ("Starter Habit Tier" to "$2.99 / month • 1 Custom Habit Lock Overlay"),
        "builder" to ("Builder Unlimited Tier" to "$4.99 / month • Unlimited Custom Lock Overlays & Sound Chimes"),
        "fullstack" to ("Full-Stack Lifetime Tier" to "$9.99 / year • All Features, Priority Sync & Lifetime Access")
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(FreshBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Activate Custom Framework",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Choose your subscription plan to unlock your custom lock screen overlay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiers.forEach { (key, pair) ->
                    val (title, price) = pair
                    val isSelected = key == selectedTier
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTier(key) }
                            .testTag("tier_card_$key"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) FreshBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text(text = price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FreshBlue)
                            }
                        }
                    }
                }
            }
        }

        RippleButton(
            onClick = onActivate,
            modifier = Modifier.fillMaxWidth(),
            testTag = "mock_activate_framework_button"
        ) {
            Text("Activate Framework (Mock Activation)", fontWeight = FontWeight.Bold)
        }
    }
}
