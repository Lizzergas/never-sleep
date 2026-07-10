package com.lizz.neversleep

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Expandable disclaimer as a self-contained scrollable item.
 * Collapsed: just header (small fixed height)
 * Expanded: animates to known height (180.dp), content scrolls internally.
 * This way the main screen (button position) does not shift/"expand".
 */
@Composable
internal fun DisclaimerSection(
    modifier: Modifier = Modifier,
    privacyPolicyUrl: String,
) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }

    val collapsedHeight = 52.dp
    val expandedHeight = 180.dp // predetermined size - enough to read the text with scrolling

    val targetHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "disclaimerHeight",
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrowRotation",
    )

    Column(
        modifier = modifier
            .height(targetHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                expanded = !expanded
            }.background(Color(0xFF1A1A2E).copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Header always visible
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "About permissions & compliance",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "▼",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = arrowRotation
                },
            )
        }

        // Scrollable text - only when expanded, takes the remaining predetermined space
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
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
                            color = Color(0xFF9E8CFF),
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
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Start,
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
