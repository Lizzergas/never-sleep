package com.lizz.neversleep.showcase.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.lizz.neversleep.designsystem.AppTheme
import com.lizz.neversleep.designsystem.Theme
import com.lizz.neversleep.navigation.FeatureCatalog
import com.lizz.neversleep.navigation.FeatureDescriptor
import com.lizz.neversleep.showcase.DesignsystemGalleryRoute
import org.koin.compose.koinInject

/**
 * The template's start destination: lists every installed feature from the
 * FeatureCatalog (derived from the app shell's registration list). Replace
 * with your real start screen when building an app from the template.
 */
@Composable
fun ShowcaseHomeScreen(onOpenFeature: (NavKey) -> Unit) {
    val catalog = koinInject<FeatureCatalog>()
    ShowcaseHomeContent(catalog = catalog, onOpenFeature = onOpenFeature)
}

@Composable
fun ShowcaseHomeContent(
    catalog: FeatureCatalog,
    onOpenFeature: (NavKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            "Installed features",
            style = MaterialTheme.typography.titleMedium,
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

@Preview
@Composable
private fun ShowcaseHomeContentPreview() {
    AppTheme {
        ShowcaseHomeContent(
            catalog = FeatureCatalog(
                listOf(
                    FeatureDescriptor(
                        id = "preview",
                        title = "Design system gallery",
                        description = "Colors, typography and spacing tokens rendered live",
                        startRoute = DesignsystemGalleryRoute,
                    ),
                ),
            ),
            onOpenFeature = {},
        )
    }
}
