package com.example.gymrank.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GymRank Colors - Premium Dark Green Aesthetic
 * FIXED PALETTE - DO NOT CHANGE
 */
object GymRankColors {
    // Backgrounds
    val Background = Color(0xFF0B0F0E) // Dark green-black
    val Surface = Color(0xFF121917) // Cards, elevated surfaces
    val SurfaceAlt = Color(0xFF1A2421) // Inputs, secondary cards

    // Borders & Dividers
    val Outline = Color(0xFF1F2A26) // Borders, dividers

    // Text
    val TextPrimary = Color(0xFFFFFFFF) // White
    val TextSecondary = Color(0xFF9AA5A1) // Gray-green

    // Primary Accent (Soft Green - NOT neon)
    val PrimaryAccent = Color(0xFF2EF2A0) // Soft mint green
    val PrimaryAccentPressed = Color(0xFF1EBE7C) // Darker when pressed
    val PrimaryAccentText = Color(0xFF000000) // Black text on accent

    // Status
    val Error = Color(0xFFFF4D4D)
    val Success = Color(0xFF2EF2A0)

    // Gradients for overlays
    val OverlayTop = Color(0xCC0B0F0E) // 80% opacity
    val OverlayBottom = Color(0x000B0F0E) // Transparent

    // Blue premium palette additions for onboarding
    val BluePrimary = Color(0xFF0A84FF)
    val BluePrimaryPressed = Color(0xFF0066CC)
    val CyanGlow = Color(0xFF00D1FF)

    // iOS-like dark surfaces per spec
    val BlackBase = Color(0xFF000000)
    val SurfaceCard = Color(0xFF1C1C1E)
    val SurfaceInput = Color(0xFF2C2C2E)
    val DividerSubtle = Color(0xFF38383A)
}

/**
 * Design Tokens
 */
object DesignTokens {

    // Use GymRankColors
    object Colors {
        val BackgroundDark = GymRankColors.Background
        val BackgroundMedium = GymRankColors.Surface
        val BackgroundCard = GymRankColors.Surface

        val NeonGreen = GymRankColors.PrimaryAccent
        val NeonGreenLight = GymRankColors.PrimaryAccent
        val NeonGreenDark = GymRankColors.PrimaryAccentPressed

        val SurfaceDark = GymRankColors.Surface
        val SurfaceInput = GymRankColors.SurfaceAlt
        val SurfaceLight = GymRankColors.Surface
        val SurfaceOverlay = Color(0x80000000)

        val TextPrimary = GymRankColors.TextPrimary
        val TextSecondary = GymRankColors.TextSecondary
        val TextTertiary = GymRankColors.TextSecondary
        val TextHint = GymRankColors.TextSecondary

        val BorderSubtle = GymRankColors.Outline
        val BorderFocus = GymRankColors.PrimaryAccent
        val BorderInput = GymRankColors.Outline

        val LinkBlue = Color(0xFF0A84FF)

        val ErrorRed = GymRankColors.Error
        val SuccessGreen = GymRankColors.Success
        val WarningYellow = Color(0xFFFFD60A)

        val GradientStart = GymRankColors.Background
        val GradientMid = GymRankColors.Surface
        val GradientEnd = GymRankColors.Background
        val OverlayGradientTop = GymRankColors.OverlayTop
        val OverlayGradientBottom = GymRankColors.OverlayBottom

        // Map onboarding specific tokens
        val BackgroundBase = GymRankColors.BlackBase
        val SurfaceElevated = GymRankColors.SurfaceCard
        val SurfaceInputs = GymRankColors.SurfaceInput
        val DividerSubtle = GymRankColors.DividerSubtle
        val PrimaryBlue = GymRankColors.BluePrimary
        val PrimaryBluePressed = GymRankColors.BluePrimaryPressed
        val GlowCyan = GymRankColors.CyanGlow
    }

    // Spacing System (8dp base)
    object Spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
        val xxxl: Dp = 64.dp
    }

    // Corner Radii - iOS Style
    object CornerRadius {
        val small: Dp = 8.dp
        val medium: Dp = 14.dp
        val large: Dp = 16.dp
        val pill: Dp = 28.dp
        val sheetTop: Dp = 28.dp
        val round: Dp = 100.dp
        val card: Dp = 16.dp
        val input: Dp = 14.dp
        val pillLarge: Dp = 999.dp
    }

    // Elevation
    object Elevation {
        val none: Dp = 0.dp
        val low: Dp = 2.dp
        val medium: Dp = 4.dp
        val high: Dp = 8.dp
    }

    // Border widths
    object BorderWidth {
        val thin: Dp = 1.dp
        val medium: Dp = 1.5.dp
        val thick: Dp = 2.dp
    }
}
