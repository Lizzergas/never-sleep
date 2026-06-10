package com.lizz.myapptemplate.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale used across all screens. Access via [AppSpacing.current] or `Theme.spacing`. */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Convenience accessor: `Theme.spacing.md` */
object Theme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}
