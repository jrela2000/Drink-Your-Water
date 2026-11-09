package com.aistudio.drinkyourwater.hydra.ui.screens

import androidx.compose.runtime.remember
import com.aistudio.drinkyourwater.hydra.data.stats.HabitStats
import com.aistudio.drinkyourwater.hydra.data.model.CompletionLog
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
import com.aistudio.drinkyourwater.hydra.billing.BillingState
import com.aistudio.drinkyourwater.hydra.data.model.Framework
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.data.model.UserProfile
import com.aistudio.drinkyourwater.hydra.ui.components.ProgressRing
import com.aistudio.drinkyourwater.hydra.ui.components.ReminderCard
import com.aistudio.drinkyourwater.hydra.ui.theme.FreshBlue
import com.aistudio.drinkyourwater.hydra.ui.theme.IceTeal

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    reminders: List<Reminder>,
    frameworks: List<Framework>,
    completionLogs: List<CompletionLog>,
    onToggleReminder: (Long, Boolean) -> Unit,
    onTriggerLockOverlay: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddNewReminder: () -> Unit,
    billing: BillingState = BillingState(),
    onNavigateToBuilder: () -> Unit = {}
) {
    val activeReminders = reminders.filter { it.isActive }
    val stats = remember(completionLogs, reminders) {
        HabitStats.snapshot(completionLogs, reminders, System.currentTimeMillis())
    }

    val activeFrameworks = frameworks.filter { it.status == "active" }

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
                        completedCount = stats.completedToday,
                        totalCount = stats.plannedToday,
                        streakCount = stats.currentStreak,
                        size = 200.dp
                    )
                }
            }

            // More reminder types: a "coming soon" teaser until billing is configured, then the
            // entry to the subscription-gated builder.
            item {
                val activeCustom = frameworks.count { !it.isWater && it.status == "active" }
                val badge = when {
                    !billing.isAvailable -> "Coming soon"
                    billing.tier == null -> "Subscribe"
                    activeCustom >= billing.tier.frameworkLimit -> "Upgrade"
                    else -> "Create +"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (billing.isAvailable) Modifier.clickable { onNavigateToBuilder() } else Modifier)
                        .testTag(if (billing.isAvailable) "build_custom_framework_banner" else "more_reminders_coming_soon"),
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
                                text = if (billing.isAvailable) "Build Custom Habit Framework" else "More Reminder Types",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (billing.isAvailable) {
                                    "Medications, movement, study, prayer & lock-screen accountability."
                                } else {
                                    "Medications, movement, study, prayer & more lock-screen accountability are on the way."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (billing.isAvailable) FreshBlue else FreshBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (billing.isAvailable) Color.White else FreshBlue
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

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
