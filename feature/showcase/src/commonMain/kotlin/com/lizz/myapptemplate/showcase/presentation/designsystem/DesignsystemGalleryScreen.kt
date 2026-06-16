package com.lizz.myapptemplate.showcase.presentation.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.Theme

/** Renders the design system tokens live: colors, typography, spacing. */
@Composable
fun DesignsystemGalleryScreen(onBack: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Text("Design system", style = MaterialTheme.typography.headlineMedium)

        Text("Colors", style = MaterialTheme.typography.titleMedium)
        ColorRow("primary", MaterialTheme.colorScheme.primary)
        ColorRow("primaryContainer", MaterialTheme.colorScheme.primaryContainer)
        ColorRow("secondary", MaterialTheme.colorScheme.secondary)
        ColorRow("surface", MaterialTheme.colorScheme.surface)

        Text("Typography", style = MaterialTheme.typography.titleMedium)
        Text("headlineMedium", style = MaterialTheme.typography.headlineMedium)
        Text("titleMedium", style = MaterialTheme.typography.titleMedium)
        Text("bodyMedium", style = MaterialTheme.typography.bodyMedium)
        Text("labelSmall", style = MaterialTheme.typography.labelSmall)

        Text("Spacing", style = MaterialTheme.typography.titleMedium)
        SpacingBar("xs", Theme.spacing.xs)
        SpacingBar("sm", Theme.spacing.sm)
        SpacingBar("md", Theme.spacing.md)
        SpacingBar("lg", Theme.spacing.lg)
        SpacingBar("xl", Theme.spacing.xl)

        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun ColorRow(
    name: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Theme.spacing.lg)
                    .background(color, RoundedCornerShape(4.dp)),
        )
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SpacingBar(
    name: String,
    width: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(width)
                    .height(Theme.spacing.sm)
                    .background(MaterialTheme.colorScheme.primary),
        )
        Text("$name ($width)", style = MaterialTheme.typography.labelSmall)
    }
}

@Preview
@Composable
private fun DesignsystemGalleryPreview() {
    AppTheme {
        DesignsystemGalleryScreen(onBack = {})
    }
}
