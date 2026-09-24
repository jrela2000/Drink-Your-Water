package com.aistudio.drinkyourwater.hydra.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.drinkyourwater.hydra.data.model.Reminder
import com.aistudio.drinkyourwater.hydra.data.model.UserProfile
import com.aistudio.drinkyourwater.hydra.ui.theme.ConfirmGreen
import com.aistudio.drinkyourwater.hydra.ui.theme.IceTeal
import com.aistudio.drinkyourwater.hydra.ui.theme.MidnightWater
import com.aistudio.drinkyourwater.hydra.ui.theme.SnoozeGray
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun LockOverlayScreen(
    reminder: Reminder?,
    userProfile: UserProfile,
    onConfirmHydration: (Long, String, Int) -> Unit,
    onSnooze: (Long) -> Unit
) {
    var snoozeCount by remember { mutableIntStateOf(reminder?.snoozeCount ?: 0) }
    var isConfirmed by remember { mutableStateOf(false) }

    // Pin the app so Home/Recents/system Back can't be used to skip the reminder. Requires
    // "Screen pinning" to be enabled on the device (Settings > Security); if it's off,
    // startLockTask() is a no-op here rather than throwing, so the overlay still shows,
    // just without OS-level pinning.
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        try {
            activity?.startLockTask()
        } catch (e: Exception) {
        }
        onDispose {
            try {
                activity?.stopLockTask()
            } catch (e: Exception) {
            }
        }
    }

    // Only Confirm or Snooze may leave this screen — swallow the system Back button.
    BackHandler { }

    // Wave Pulse Animation Transition
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val currentTimeStr = timeFormat.format(Date())

    val motivationalCopy = when (userProfile.motivationalStyle) {
        "affirmations" -> "I nourish my body with pure water and stay focused on my daily vitality."
        "scripture" -> "Let anyone who is thirsty come to me and drink. (John 7:37)"
        else -> "Drinking water boosts brain energy, clears skin, and fuels physical endurance!"
    }

    val gradientColors = when (userProfile.overlayTheme) {
        "ice-teal" -> listOf(Color(0xFF0F4C5C), Color(0xFF48CAE4))
        "deep-ocean" -> listOf(Color(0xFF03045E), Color(0xFF0077B6))
        "cool-mint" -> listOf(Color(0xFF1B4332), Color(0xFF52B788))
        else -> listOf(MidnightWater, Color(0xFF1A6FA8))
    }

    val reminderTitle = reminder?.text ?: "Hydration Reminder"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .padding(24.dp)
            .testTag("lock_overlay_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Lock Banner & Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = IceTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOCK SCREEN OVERLAY ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTimeStr,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    ),
                    color = Color.White
                )
            }

            // Central Animated Water Droplet + Motivational Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .scale(waveScale)
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(IceTeal, Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = reminderTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = motivationalCopy,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceTeal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Success Burst or Main Action Buttons
            if (isConfirmed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ConfirmGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hydration Confirmed! Unlocking...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isConfirmed = true
                            onConfirmHydration(reminder?.id ?: 1L, reminderTitle, snoozeCount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("confirm_hydration_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConfirmGreen)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Confirm & Unlock Screen ✓",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    val snoozeLimitReached = snoozeCount >= 2
                    Button(
                        onClick = {
                            snoozeCount++
                            onSnooze(reminder?.id ?: 1L)
                        },
                        enabled = !snoozeLimitReached,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("snooze_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnoozeGray.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (snoozeLimitReached) {
                                    "No snoozes left — confirm to continue"
                                } else {
                                    "Snooze 10 min ($snoozeCount/2 used)"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
