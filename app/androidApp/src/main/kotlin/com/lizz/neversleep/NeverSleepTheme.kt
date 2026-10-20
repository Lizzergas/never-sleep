package com.lizz.neversleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared product roles used by both app states.
 *
 * ON is warm: sun, amber, and a screen that stays awake.
 * OFF is cool: moon, violet, and the device's usual timeout.
 */
internal object NeverSleepColors {
    val Canvas = Color(0xFF0D0D1A)
    val Panel = Color(0xFF18182A)
    val PanelStrong = Color(0xFF22223A)
    val Content = Color(0xFFF1EFF8)
    val ContentMuted = Color(0xFFC3BECE)
    val ContentSubtle = Color(0xFFA9A3B7)
    val Divider = Color(0xFF3D3850)
    val NormalAccent = Color(0xFFFFB74D)
    val DayAmbient = Color(0xFFFFB74D)
    val NeverAccent = Color(0xFF9E8CFF)
    val NightAmbient = Color(0xFF7C4DFF)
}

internal object NeverSleepSpacing {
    val Xs: Dp = 8.dp
    val Sm: Dp = 12.dp
    val Md: Dp = 16.dp
    val Lg: Dp = 24.dp
    val Xl: Dp = 32.dp
}

internal object NeverSleepDimensions {
    val AdHeight: Dp = 50.dp
    val ToggleMax: Dp = 210.dp
    val ToggleLandscapeMax: Dp = 180.dp
    val ToggleMinimum: Dp = 136.dp
    val TouchTarget: Dp = 48.dp
}

private val NeverSleepTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 4.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

/** Dark, shader-friendly Material theme for the Never Sleep product surfaces. */
@Composable
internal fun NeverSleepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeverSleepColors.NeverAccent,
            onPrimary = NeverSleepColors.Canvas,
            primaryContainer = NeverSleepColors.PanelStrong,
            onPrimaryContainer = NeverSleepColors.Content,
            background = NeverSleepColors.Canvas,
            onBackground = NeverSleepColors.Content,
            surface = NeverSleepColors.Panel,
            onSurface = NeverSleepColors.Content,
            surfaceVariant = NeverSleepColors.PanelStrong,
            onSurfaceVariant = NeverSleepColors.ContentMuted,
            outline = NeverSleepColors.Divider,
        ),
        typography = NeverSleepTypography,
        content = content,
    )
}
