package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Framework
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import com.example.ui.components.DraftFrameworkCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.ReminderCard
import com.example.ui.theme.FreshBlue
import com.example.ui.theme.IceTeal

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    reminders: List<Reminder>,
    frameworks: List<Framework>,
    onToggleReminder: (Long, Boolean) -> Unit,
    onTriggerLockOverlay: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBuilder: () -> Unit,
    onNavigateToActivate: () -> Unit,
    onAddNewReminder: () -> Unit
) {
    val activeReminders = reminders.filter { it.isActive }
    val totalCount = reminders.size
    val activeCount = activeReminders.size

    val activeFrameworks = frameworks.filter { it.status == "active" }
    val draftFrameworks = frameworks.filter { it.status == "draft" }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FreshBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = FreshBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Drink Your Water",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewReminder,
                containerColor = FreshBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Reminder"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Progress Ring
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(
                        completedCount = activeCount,
                        totalCount = totalCount,
                        streakCount = userProfile.streakCount,
                        size = 200.dp
                    )
                }
            }

            // Quick Banner for Custom Framework Builder
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToBuilder() }
                        .testTag("build_custom_framework_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = FreshBlue.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Build Custom Habit Framework",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Medications, movement, study, prayer & lock-screen accountability.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FreshBlue)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Create +",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Active Reminders Header
            item {
                Text(
                    text = "Today's Active Reminders (${reminders.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Reminders List
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleActive = { isActive -> onToggleReminder(reminder.id, isActive) },
                    onTriggerLockOverlay = { onTriggerLockOverlay(reminder.id) },
                    onEditReminder = { onEditReminder(reminder.id) }
                )
            }

            // Drafts / Locked Custom Frameworks Section
            if (draftFrameworks.isNotEmpty() || !userProfile.isPremium) {
                item {
                    Text(
                        text = "Explore Custom Habit Lock Overlays",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                if (draftFrameworks.isNotEmpty()) {
                    items(draftFrameworks, key = { it.id }) { draft ->
                        DraftFrameworkCard(
                            framework = draft,
                            onActivateTap = onNavigateToActivate
                        )
                    }
                } else {
                    // Default draft presets
                    item {
                        DraftFrameworkCard(
                            framework = Framework(
                                id = 991,
                                name = "Medication & Supplement Schedule",
                                status = "draft"
                            ),
                            onActivateTap = onNavigateToActivate
                        )
                    }
                    item {
                        DraftFrameworkCard(
                            framework = Framework(
                                id = 992,
                                name = "Post-Study Movement & Stretch",
                                status = "draft"
                            ),
                            onActivateTap = onNavigateToActivate
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
