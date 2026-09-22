package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = NavySecondary,
    onPrimaryContainer = PrimaryOrangeLight,
    secondary = PrimaryOrange,
    onSecondary = Color.White,
    background = NavyDark,
    surface = NavyPrimary,
    surfaceVariant = NavySecondary,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NavySecondary,
    error = LossRed,
    errorContainer = ErrorContainer,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = PrimaryOrangeLight,
    onPrimaryContainer = PrimaryOrangeDark,
    secondary = NavyPrimary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = NavyPrimary,
    tertiary = ProfitGreen,
    background = SurfaceLight,
    surface = SurfaceCard,
    surfaceVariant = SurfaceMuted,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = BorderColor.copy(alpha = 0.6f),
    error = LossRed,
    errorContainer = ErrorContainer,
    onError = Color.White,
    onErrorContainer = LossRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent ERP brand styling across all devices
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

