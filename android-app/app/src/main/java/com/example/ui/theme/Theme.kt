package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGoldLight,
    onPrimary = Midnight,
    primaryContainer = BrandGoldDark,
    onPrimaryContainer = Color.White,
    secondary = InfoSky,
    onSecondary = Midnight,
    tertiary = SuccessGreen,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B4C5)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2E5BF),
    onPrimaryContainer = Midnight,
    secondary = Midnight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E6EE),
    onSecondaryContainer = Midnight,
    tertiary = SuccessGreen,
    background = WarmBackground,
    onBackground = Midnight,
    surface = WarmSurface,
    onSurface = Midnight,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = Color(0xFF687386)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
