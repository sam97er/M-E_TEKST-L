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
    primary = Color(0xFF72B6FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF004689),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF4DD8D0),
    onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFF71F5EC),
    tertiary = TrendyolOrangeLight,
    onTertiary = Color(0xFF4C2100),
    tertiaryContainer = Color(0xFF6E3200),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = ErpNavyDarkBg,
    onBackground = Color(0xFFE2E8F0),
    surface = ErpNavyDarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = ErpNavyDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = ErpNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FD),
    onPrimaryContainer = Color(0xFF001B3B),
    secondary = ErpTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCEEE9),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = TrendyolOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEC9),
    onTertiaryContainer = Color(0xFF321300),
    background = ErpBgLight,
    onBackground = Color(0xFF0F172A),
    surface = ErpSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = ErpSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun SmartErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our tailored ERP palette by default
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
