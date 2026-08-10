package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RippleButton
import com.example.ui.theme.ConfirmGreen
import com.example.ui.theme.FreshBlue
import com.example.ui.theme.IceTeal
import com.example.ui.theme.MidnightWater

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (String) -> Unit,
    onNavigateToBuilder: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedMotivationalStyle by remember { mutableStateOf("waterFacts") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                (1..6).forEach { i ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == step) 28.dp else 8.dp)
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
                label = "onboarding_step"
            ) { currentStep ->
                when (currentStep) {
                    1 -> OnboardingStep1(
                        onStartWater = { step = 3 },
                        onShowHowItWorks = { step = 2 }
                    )
                    2 -> OnboardingStep2(onNext = { step = 3 })
                    3 -> OnboardingStep3(onNext = { step = 4 })
                    4 -> OnboardingStep4(onNext = { step = 5 })
                    5 -> OnboardingStep5(
                        selectedStyle = selectedMotivationalStyle,
                        onSelectStyle = { selectedMotivationalStyle = it },
                        onNext = { step = 6 }
                    )
                    6 -> OnboardingStep6(
                        onBuildCustom = {
                            onCompleteOnboarding(selectedMotivationalStyle)
                            onNavigateToBuilder()
                        },
                        onStartWaterOnly = {
                            onCompleteOnboarding(selectedMotivationalStyle)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep1(onStartWater: () -> Unit, onShowHowItWorks: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(IceTeal, MidnightWater)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Drink Your Water",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your body has been waiting.",
                style = MaterialTheme.typography.titleMedium,
                color = FreshBlue,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "True accountability through full-screen lock overlay reminders. Confirm hydration to unlock your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RippleButton(
                onClick = onStartWater,
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_water_free_btn"
            ) {
                Text("Start With Water (Free Forever)", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onShowHowItWorks,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Me How It Works", color = FreshBlue)
            }
        }
    }
}

@Composable
private fun OnboardingStep2(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(
            imageVector = Icons.Default.LockClock,
            contentDescription = null,
            tint = FreshBlue,
            modifier = Modifier.size(80.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Signature Lock Screen Overlay",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "When a reminder fires, your screen locks with a soothing water overlay. It only unlocks when you confirm you completed the habit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "onboarding_step2_next"
        ) {
            Text("Got It, Next", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingStep3(onNext: () -> Unit) {
    val sampleReminders = listOf(
        "08:00 AM" to "Morning Hydration (First Glass)",
        "10:30 AM" to "Post-Breakfast Water Refresh",
        "01:00 PM" to "Mid-Day Hydration Boost",
        "03:30 PM" to "Afternoon Energy Hydration",
        "06:30 PM" to "Pre-Dinner Glass of Water",
        "09:00 PM" to "Evening Wind-Down Hydration"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "6 Preloaded Hydrations",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Balanced throughout your day automatically",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleReminders) { (time, text) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = FreshBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "$time • $text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "activate_water_reminders_btn"
        ) {
            Text("Activate Water Reminders", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingStep4(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = FreshBlue,
            modifier = Modifier.size(80.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Enable Lock Overlay Alerts",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Allow notifications so your screen locks on schedule and keeps you accountable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RippleButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                testTag = "allow_notifications_btn"
            ) {
                Text("Allow Notifications", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Up Later")
            }
        }
    }
}

@Composable
private fun OnboardingStep5(
    selectedStyle: String,
    onSelectStyle: (String) -> Unit,
    onNext: () -> Unit
) {
    val styles = listOf(
        "waterFacts" to ("Water Facts" to "Scientific trivia & health benefits of proper hydration"),
        "affirmations" to ("Positive Affirmations" to "Mindful encouraging self-talk and habit focus"),
        "scripture" to ("Scripture & Faith" to "Inspirational spiritual quotes and verses")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Motivational Vibe",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Copy displayed on your lock screen overlay",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            styles.forEach { (key, pair) ->
                val (title, desc) = pair
                val isSelected = key == selectedStyle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectStyle(key) }
                        .testTag("style_option_$key"),
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
                            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = FreshBlue)
                        }
                    }
                }
            }
        }

        RippleButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            testTag = "confirm_vibe_btn"
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingStep6(
    onBuildCustom: () -> Unit,
    onStartWaterOnly: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Unlock Custom Habit Frameworks",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Water is free forever! You can also build lock overlays for medications, movement, study time, prayer, and vitamins.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RippleButton(
                onClick = onBuildCustom,
                modifier = Modifier.fillMaxWidth(),
                testTag = "build_custom_habit_btn"
            ) {
                Text("Build a Custom Habit Framework", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onStartWaterOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start With Water Only", color = FreshBlue)
            }
        }
    }
}
