package com.lizz.myapptemplate.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 window width breakpoints (600/840dp). Hand-rolled to stay
 * dependency-free; swap for material3-adaptive once it's stable on CMP.
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        private val MEDIUM_MIN = 600.dp
        private val EXPANDED_MIN = 840.dp

        fun fromWidth(width: Dp): WindowWidthClass =
            when {
                width < MEDIUM_MIN -> Compact
                width < EXPANDED_MIN -> Medium
                else -> Expanded
            }
    }
}
