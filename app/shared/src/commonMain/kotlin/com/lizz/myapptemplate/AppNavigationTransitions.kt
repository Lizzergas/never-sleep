package com.lizz.myapptemplate

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

internal const val APP_NAV_ENTER_MILLIS = 180
internal const val APP_NAV_EXIT_MILLIS = 90
internal const val APP_NAV_ENTER_DELAY_MILLIS = 90

internal fun <T : Any> appNavTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    { appNavFadeThroughTransform() }

internal fun <T : Any> appNavPopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    { appNavFadeThroughTransform() }

internal fun <T : Any> appNavPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    { _ -> appNavFadeThroughTransform() }

private fun appNavFadeThroughTransform(): ContentTransform =
    ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(
                durationMillis = APP_NAV_ENTER_MILLIS,
                delayMillis = APP_NAV_ENTER_DELAY_MILLIS,
            ),
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(
                durationMillis = APP_NAV_EXIT_MILLIS,
            ),
        ),
    )
