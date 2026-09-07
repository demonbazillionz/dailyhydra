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
    primary = HydrationBlueDark,
    secondary = HydrationCyanDark,
    tertiary = ColorExcellent,
    background = HydrationBgDark,
    surface = HydrationSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = HydrationCardDark
)

private val LightColorScheme = lightColorScheme(
    primary = HydrationBlueLight,
    secondary = HydrationCyanLight,
    tertiary = ColorExcellent,
    background = HydrationBgLight,
    surface = HydrationSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1A1F2C),
    onSurface = Color(0xFF1A1F2C),
    surfaceVariant = HydrationCardLight
)

private val WaterLightColorScheme = lightColorScheme(
    primary = WaterPrimary,
    onPrimary = WaterTextPrimary,
    secondary = WaterSecondary,
    onSecondary = WaterTextPrimary,
    tertiary = WaterAccent,
    onTertiary = WaterTextPrimary,
    background = WaterBackground,
    onBackground = WaterTextPrimary,
    surface = WaterSurface,
    onSurface = WaterTextPrimary,
    surfaceVariant = WaterSurfaceVariant,
    onSurfaceVariant = WaterTextSecondary,
    outline = WaterTextSecondary.copy(alpha = 0.2f)
)

private val DarkWaterColorScheme = darkColorScheme(
    primary = DarkWaterPrimaryAccent,
    onPrimary = Color.Black,
    secondary = DarkWaterSecondaryAccent,
    onSecondary = Color.White,
    tertiary = DarkWaterHighlightAccent,
    onTertiary = Color.Black,
    background = DarkWaterBackground,
    onBackground = DarkWaterTextPrimary,
    surface = DarkWaterCardBackground,
    onSurface = DarkWaterTextPrimary,
    surfaceVariant = DarkWaterElevatedSurface,
    onSurfaceVariant = DarkWaterTextSecondary,
    outline = DarkWaterBorders,
    outlineVariant = DarkWaterSecondaryBackground
)

val LocalAnimationsEnabled = androidx.compose.runtime.compositionLocalOf { true }

@Composable
fun MyApplicationTheme(
    themeMode: String = "System",
    amoledDarkMode: Boolean = false,
    dynamicColor: Boolean = false,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val isDarkSelected = when (themeMode) {
        "Dark" -> true
        "Dark Water" -> true
        "Light" -> false
        "Water" -> false
        "Light Water" -> false
        else -> darkTheme
    }

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkSelected) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == "Water" || themeMode == "Light Water" -> WaterLightColorScheme
        themeMode == "Dark Water" -> DarkWaterColorScheme
        isDarkSelected -> DarkColorScheme
        else -> LightColorScheme
    }


    // Apply AMOLED extreme black background if configured
    if (amoledDarkMode && isDarkSelected) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0F1D), // Deep dark variant
            surfaceVariant = Color(0xFF121B30)
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAnimationsEnabled provides animationsEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
