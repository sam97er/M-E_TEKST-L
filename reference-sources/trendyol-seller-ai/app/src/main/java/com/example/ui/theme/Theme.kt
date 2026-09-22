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
  primary = TrendyolOrangeGradientStart,
  onPrimary = Color.White,
  primaryContainer = TrendyolOrangeDark,
  onPrimaryContainer = Color.White,
  secondary = TrendyolBlue,
  onSecondary = Color.White,
  tertiary = TrendyolSuccessGreen,
  background = Slate900,
  surface = Slate800,
  onBackground = Slate100,
  onSurface = Slate100,
  surfaceVariant = Slate700,
  onSurfaceVariant = Slate300
)

private val LightColorScheme = lightColorScheme(
  primary = TrendyolOrange,
  onPrimary = Color.White,
  primaryContainer = TrendyolOrangeLight,
  onPrimaryContainer = TrendyolOrangeDark,
  secondary = TrendyolNavy,
  onSecondary = Color.White,
  tertiary = TrendyolSuccessGreen,
  background = Slate50,
  surface = Color.White,
  onBackground = Slate900,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate700
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve iconic Trendyol branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

