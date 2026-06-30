package com.lizz.myapptemplate

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

internal const val APP_NAV_ENTER_MILLIS = 260
internal const val APP_NAV_EXIT_MILLIS = 180
internal const val APP_NAV_ENTER_DELAY_MILLIS = 0
internal const val APP_NAV_SWITCH_ENTER_MILLIS = 180
internal const val APP_NAV_SWITCH_EXIT_MILLIS = 90
internal const val APP_NAV_SWITCH_ENTER_DELAY_MILLIS = 90

internal fun <T : Any> appNavTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    {
        if (isPushWithinCurrentStack()) {
            appNavForwardTransform()
        } else {
            appNavSwitchTransform()
        }
    }

internal fun <T : Any> appNavPopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    {
        if (isPopWithinCurrentStack()) {
            appNavPopTransform()
        } else {
            appNavSwitchTransform()
        }
    }

internal fun <T : Any> appNavPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    { _ ->
        if (isPopWithinCurrentStack()) {
            appNavPopTransform()
        } else {
            appNavSwitchTransform()
        }
    }

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.isPushWithinCurrentStack(): Boolean {
    val initialEntryKey = initialState.entries.lastOrNull()?.contentKey
    val targetPreviousEntryKey = targetState.previousEntries.lastOrNull()?.contentKey
    return initialEntryKey != null && initialEntryKey == targetPreviousEntryKey
}

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.isPopWithinCurrentStack(): Boolean {
    val targetEntryKey = targetState.entries.lastOrNull()?.contentKey
    val initialPreviousEntryKey = initialState.previousEntries.lastOrNull()?.contentKey
    return targetEntryKey != null && targetEntryKey == initialPreviousEntryKey
}

private fun appNavSwitchTransform(): ContentTransform =
    ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(
                durationMillis = APP_NAV_SWITCH_ENTER_MILLIS,
                delayMillis = APP_NAV_SWITCH_ENTER_DELAY_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(
                durationMillis = APP_NAV_SWITCH_EXIT_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ),
    )

private fun appNavForwardTransform(): ContentTransform =
    (
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = APP_NAV_ENTER_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetX = { fullWidth -> fullWidth },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = APP_NAV_ENTER_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
    ).togetherWith(
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = APP_NAV_EXIT_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            targetOffsetX = { fullWidth -> -fullWidth },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = APP_NAV_EXIT_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ),
    )

private fun appNavPopTransform(): ContentTransform =
    (
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = APP_NAV_ENTER_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetX = { fullWidth -> -fullWidth },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = APP_NAV_ENTER_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
    ).togetherWith(
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = APP_NAV_EXIT_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            targetOffsetX = { fullWidth -> fullWidth },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = APP_NAV_EXIT_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ),
    )
