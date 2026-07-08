package com.lizz.neversleep.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lizz.neversleep.designsystem.AppTheme
import com.lizz.neversleep.designsystem.Theme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private data class OnboardingPage(
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        "Welcome",
        "Keep your notes and preferences ready across your devices.",
    ),
    OnboardingPage(
        "Stay organized",
        "Create, refresh, and manage notes from a clean shared workspace.",
    ),
    OnboardingPage(
        "Make it yours",
        "Choose your theme, sign in, and start with the workflow that fits you.",
    ),
)

/** Stateful wrapper: owns the ViewModel and collects one-off effects. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val viewModel = koinViewModel<OnboardingViewModel>()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.Done -> onFinished()
            }
        }
    }

    OnboardingContent(onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingContent(onEvent: (OnboardingEvent) -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { pages.size }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(page.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(Theme.spacing.md),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val color = if (index == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier
                        .padding(Theme.spacing.xs)
                        .size(Theme.spacing.sm)
                        .background(color, CircleShape),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onEvent(OnboardingEvent.Finish) }) { Text("Skip") }
            if (pagerState.currentPage == pages.lastIndex) {
                Button(onClick = { onEvent(OnboardingEvent.Finish) }) { Text("Get started") }
            } else {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Text("Next") }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingContentPreview() {
    AppTheme {
        OnboardingContent(onEvent = {})
    }
}
