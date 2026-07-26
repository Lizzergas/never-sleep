package com.lizz.neversleep

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Expandable disclaimer as a self-contained scrollable item.
 * The header uses a minimum rather than fixed height, so it can grow with font scale.
 * Expanded copy is bounded and scrollable to keep the primary control usable.
 */
@Composable
internal fun DisclaimerSection(
    modifier: Modifier = Modifier,
    privacyPolicyUrl: String,
) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrowRotation",
    )

    Surface(
        modifier = modifier,
        color = NeverSleepColors.Panel,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .semantics {
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }.clickable(role = Role.Button) {
                        expanded = !expanded
                    }.padding(
                        horizontal = NeverSleepSpacing.Md,
                        vertical = NeverSleepSpacing.Xs,
                    ),
            ) {
                Text(
                    text = "About permissions & compliance",
                    color = NeverSleepColors.ContentMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "▼",
                    color = NeverSleepColors.ContentSubtle,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = arrowRotation
                    },
                )
            }

            if (expanded) {
                Spacer(Modifier.height(NeverSleepSpacing.Xs))
                val disclaimerText = buildAnnotatedString {
                    append(
                        "This app uses the 'Modify system settings' permission to control screen timeout. " +
                            "This is required for the 'never sleep' feature. " +
                            "Enabling this may increase battery usage and device temperature. " +
                            "Some devices (e.g. Samsung, Xiaomi) or enterprise policies may override this setting. " +
                            "You can revoke access anytime in Android Settings > Apps > Special app access.\n\n" +
                            "Ads: A small banner ad may appear at the bottom. You can disable ads in Settings. " +
                            "Google AdMob may collect device identifiers for ad delivery — see our ",
                    )
                    pushStringAnnotation(tag = "privacy", annotation = privacyPolicyUrl)
                    withStyle(
                        SpanStyle(
                            color = NeverSleepColors.NeverAccent,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append("privacy policy")
                    }
                    pop()
                    append(".")
                }
                ClickableText(
                    text = disclaimerText,
                    style = TextStyle(
                        color = NeverSleepColors.ContentMuted,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        textAlign = TextAlign.Start,
                    ),
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = NeverSleepSpacing.Md,
                            end = NeverSleepSpacing.Md,
                            bottom = NeverSleepSpacing.Md,
                        ),
                    onClick = { offset ->
                        disclaimerText
                            .getStringAnnotations("privacy", offset, offset)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }
                    },
                )
            }
        }
    }
}
