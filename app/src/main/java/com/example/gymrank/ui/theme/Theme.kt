package com.example.gymrank.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GymRankDarkColorScheme = darkColorScheme(
    primary = GymRankColors.PrimaryAccent,
    onPrimary = GymRankColors.PrimaryAccentText,
    primaryContainer = GymRankColors.PrimaryAccentPressed,
    onPrimaryContainer = GymRankColors.PrimaryAccentText,

    secondary = GymRankColors.Surface,
    onSecondary = GymRankColors.TextPrimary,
    secondaryContainer = GymRankColors.SurfaceAlt,
    onSecondaryContainer = GymRankColors.TextSecondary,

    tertiary = GymRankColors.PrimaryAccent,
    onTertiary = GymRankColors.PrimaryAccentText,

    background = GymRankColors.Background,
    onBackground = GymRankColors.TextPrimary,

    surface = GymRankColors.Surface,
    onSurface = GymRankColors.TextPrimary,
    surfaceVariant = GymRankColors.SurfaceAlt,
    onSurfaceVariant = GymRankColors.TextSecondary,

    error = GymRankColors.Error,
    onError = GymRankColors.TextPrimary,

    outline = GymRankColors.Outline,
    outlineVariant = GymRankColors.Outline,

    scrim = Color.Black.copy(alpha = 0.6f)
)

private val GymRankShapes = Shapes(
    small = RoundedCornerShape(DesignTokens.CornerRadius.small),
    medium = RoundedCornerShape(DesignTokens.CornerRadius.medium),
    large = RoundedCornerShape(DesignTokens.CornerRadius.large)
)

@Composable
fun GymRankTheme(
    darkTheme: Boolean = true, // Always use dark theme for premium look
    dynamicColor: Boolean = false, // Disable dynamic color to maintain brand
    content: @Composable () -> Unit
) {
    val colorScheme = GymRankDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GymRankColors.Background.toArgb()
            window.navigationBarColor = GymRankColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GymRankShapes,
        content = content
    )
}
