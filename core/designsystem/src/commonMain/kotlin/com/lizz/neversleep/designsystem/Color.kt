package com.lizz.neversleep.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Replace these seed colors with your brand palette — everything else follows.
private val Primary = Color(0xFF4355B9)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFDEE0FF)
private val OnPrimaryContainer = Color(0xFF00105C)
private val Secondary = Color(0xFF5B5D72)
private val OnSecondary = Color(0xFFFFFFFF)
private val Surface = Color(0xFFFBF8FF)
private val OnSurface = Color(0xFF1B1B21)
private val SurfaceDark = Color(0xFF121318)
private val OnSurfaceDark = Color(0xFFE4E1E9)

val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    surface = Surface,
    onSurface = OnSurface,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)
