package com.itantra.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TacticalCyan,
    secondary = TacticalGreen,
    tertiary = TacticalAmber,
    background = TacticalDarkBg,
    surface = TacticalSurfaceDark,
    surfaceVariant = TacticalCardDark,
    onPrimary = TacticalDarkBg,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = TacticalRed
)

private val LightColorScheme = lightColorScheme(
    primary = TacticalCyan,
    secondary = TacticalGreen,
    tertiary = TacticalAmber,
    background = TacticalLightBg,
    surface = TacticalSurfaceLight,
    surfaceVariant = TacticalCardLight,
    onPrimary = TacticalSurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = TacticalRed
)

@Composable
fun ITantraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
