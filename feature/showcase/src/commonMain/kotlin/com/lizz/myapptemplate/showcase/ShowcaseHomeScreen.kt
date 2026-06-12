package com.lizz.myapptemplate.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.navigation.FeatureCatalog
import org.koin.compose.koinInject

/**
 * The template's start destination: lists every installed feature from the
 * FeatureCatalog (derived from the app shell's registration list). Replace
 * with your real start screen when building an app from the template.
 */
@Composable
fun ShowcaseHomeScreen(onOpenFeature: (NavKey) -> Unit) {
    val catalog = koinInject<FeatureCatalog>()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("MyAppTemplate", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Installed features",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Theme.spacing.sm),
        )
        catalog.features.forEach { feature ->
            Card(
                onClick = { onOpenFeature(feature.startRoute) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Theme.spacing.md)) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium)
                    Text(feature.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
