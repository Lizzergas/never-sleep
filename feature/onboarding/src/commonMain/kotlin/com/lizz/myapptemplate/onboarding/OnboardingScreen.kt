package com.lizz.myapptemplate.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.designsystem.Theme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class OnboardingPage(
    val title: String,
    val description: String,
)

private val pages =
    listOf(
        OnboardingPage(
            "Welcome",
            "This template ships with networking, storage, theming and navigation already wired.",
        ),
        OnboardingPage(
            "Plug-in features",
            "Every feature is a module you can keep, replace, or delete in three lines.",
        ),
        OnboardingPage(
            "Make it yours",
            "Run ./rename.sh, delete the showcase, and start building.",
        ),
    )

/** First-launch intro. [onFinished] is called after the seen-flag is persisted. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val repository = koinInject<OnboardingRepository>()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { pages.size }

    fun finish() {
        scope.launch {
            repository.markSeen()
            onFinished()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(Theme.spacing.md),
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
                val color =
                    if (index == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                Box(
                    modifier =
                        Modifier
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
            TextButton(onClick = ::finish) { Text("Skip") }
            if (pagerState.currentPage == pages.lastIndex) {
                Button(onClick = ::finish) { Text("Get started") }
            } else {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Text("Next") }
            }
        }
    }
}
