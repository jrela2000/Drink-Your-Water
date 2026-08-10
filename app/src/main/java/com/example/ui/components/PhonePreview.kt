package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ConfirmGreen
import com.example.ui.theme.IceTeal
import com.example.ui.theme.MidnightWater
import com.example.ui.theme.SnoozeGray

@Composable
fun PhonePreview(
    frameworkName: String,
    theme: String,
    motivationalText: String,
    customMessage: String,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (theme) {
        "ice-teal" -> listOf(Color(0xFF0F4C5C), Color(0xFF48CAE4))
        "deep-ocean" -> listOf(Color(0xFF03045E), Color(0xFF0077B6))
        "cool-mint" -> listOf(Color(0xFF1B4332), Color(0xFF52B788))
        else -> listOf(MidnightWater, Color(0xFF1A6FA8))
    }

    Box(
        modifier = modifier
            .width(220.dp)
            .height(380.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(4.dp, Color(0xFF2C3E50), RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(gradientColors))
            .testTag("phone_preview"),
        contentAlignment = Alignment.Center
    ) {
        // Speaker Notch
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .width(60.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Time Clock
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "10:30",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "LOCK OVERLAY ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    ),
                    color = IceTeal
                )
            }

            // Central Water Droplet & Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = frameworkName.ifEmpty { "Habit Reminder" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (customMessage.isNotEmpty()) {
                    Text(
                        text = customMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = motivationalText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = IceTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConfirmGreen)
                ) {
                    Text(
                        text = "I Did It ✓",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SnoozeGray.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Snooze 15m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
