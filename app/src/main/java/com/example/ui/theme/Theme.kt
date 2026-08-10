package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FreshBlue,
    onPrimary = Color.White,
    primaryContainer = GlacierBlue,
    onPrimaryContainer = MidnightWater,
    secondary = IceTeal,
    onSecondary = MidnightWater,
    secondaryContainer = ClearWater,
    onSecondaryContainer = MidnightWater,
    tertiary = CoolMint,
    onTertiary = MidnightWater,
    background = IceWhite,
    onBackground = MidnightWater,
    surface = Color.White,
    onSurface = DeepWater,
    surfaceVariant = GlacierBlue,
    onSurfaceVariant = DeepWater,
    outline = ClearWater,
    outlineVariant = ColdMist,
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FreshBlue,
    onPrimary = MidnightWater,
    primaryContainer = DarkCard,
    onPrimaryContainer = ColdMist,
    secondary = IceTeal,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = ColdMist,
    tertiary = CoolMint,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = IceWhite,
    surface = DarkSurface,
    onSurface = IceWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = ColdMist,
    outline = DarkBorder,
    outlineVariant = DarkCard,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun DrinkYourWaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
