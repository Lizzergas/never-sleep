package com.lizz.myapptemplate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError

/** Renders the standard chrome per state; provide only the success content. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { LoadingContent() },
    empty: @Composable () -> Unit = { EmptyContent() },
    error: @Composable (AppError) -> Unit = { ErrorContent(it, onRetry) },
    content: @Composable (T) -> Unit,
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    Box(
        modifier = modifier.animateContentSize(
            animationSpec = spatialSpec,
        ),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(UI_STATUS_FADE_IN_MILLIS)) togetherWith
                    fadeOut(animationSpec = tween(UI_STATUS_FADE_OUT_MILLIS))
            },
            label = "ui-state-content",
        ) { targetState ->
            when (targetState) {
                is UiState.Loading -> loading()
                is UiState.Empty -> empty()
                is UiState.Error -> error(targetState.error)
                is UiState.Success -> content(targetState.data)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Theme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        ContainedLoadingIndicator()
    }
}

@Composable
fun EmptyContent(
    message: String = "Nothing here yet",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Theme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ErrorContent(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    isRetrying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val scaleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spatialSpec)
                .padding(Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            error.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            val buttonScale by
                animateFloatAsState(
                    targetValue = if (isRetrying) 0.98f else 1f,
                    animationSpec = scaleSpec,
                    label = "retry-button-scale",
                )
            Button(
                onClick = onRetry,
                enabled = !isRetrying,
                modifier = Modifier.scale(buttonScale),
            ) {
                AnimatedContent(
                    targetState = isRetrying,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(UI_STATUS_FADE_IN_MILLIS)) togetherWith
                            fadeOut(animationSpec = tween(UI_STATUS_FADE_OUT_MILLIS))
                    },
                    label = "retry-button-content",
                ) { retrying ->
                    if (retrying) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LoadingIndicator(modifier = Modifier.size(18.dp))
                            Text("Retrying...")
                        }
                    } else {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
