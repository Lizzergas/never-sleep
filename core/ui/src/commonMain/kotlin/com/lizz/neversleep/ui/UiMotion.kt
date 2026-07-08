package com.lizz.neversleep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

const val UI_DEFERRED_INDICATOR_DELAY_MILLIS = 350
const val UI_REFRESH_MINIMUM_VISIBLE_MILLIS = 500
const val UI_STATUS_FADE_IN_MILLIS = 450
const val UI_STATUS_FADE_OUT_MILLIS = 250

@Composable
fun DelayedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = UI_DEFERRED_INDICATOR_DELAY_MILLIS,
    enter: EnterTransition = fadeIn(animationSpec = tween(UI_STATUS_FADE_IN_MILLIS)),
    exit: ExitTransition = fadeOut(animationSpec = tween(UI_STATUS_FADE_OUT_MILLIS)),
    content: @Composable () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(visible, delayMillis) {
        showContent = false
        if (visible) {
            delay(delayMillis.toLong().milliseconds)
            showContent = true
        }
    }
    AnimatedVisibility(
        visible = visible && showContent,
        modifier = modifier,
        enter = enter,
        exit = exit,
    ) {
        content()
    }
}
