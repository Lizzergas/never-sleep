package com.lizz.neversleep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment as ComposeAlignment
import android.util.Log
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.annotation.RequiresApi
import kotlin.math.cos
import kotlin.math.sin

private const val PREFS_NAME = "never_sleep_prefs"
private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
private const val KEY_ENABLED = "never_sleep_enabled"
private const val DEFAULT_NORMAL_TIMEOUT_MS = 30_000

// Use a high but practical value instead of Int.MAX_VALUE (some devices clamp very large values)
private const val NEVER_SLEEP_TIMEOUT_MS = 1_000_000_000  // ~11.5 days, "never" for inactivity

// Threshold for considering the timeout "never sleep" mode (even if system clamps)
private const val NEVER_SLEEP_THRESHOLD_MS = 3_600_000  // 1 hour

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Use class-level state so we can refresh it from onResume() when app is minimized/resumed
    private val isEnabledState = mutableStateOf(false)

    // Pending desired state if we had to prompt for permission
    private var pendingDesired: Boolean? = null

    // Ads visibility (persisted; default true so the stub ad shows unless user disables)
    private val showAdsState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)

        setContent {
            // Dark modern theme for a cool shader experience
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C4DFF),
                    onPrimary = Color.White,
                    background = Color(0xFF0D0D1A),
                    surface = Color(0xFF1A1A2E),
                )
            ) {
                val isEnabled by remember { isEnabledState }
                val showAds by remember { showAdsState }

                // Simple toggle callback that always reads the latest state at tap time.
                // This ensures reliable flip between never <-> normal even after resume/minimize or state changes.
                val onToggle: () -> Unit = {
                    val current = isEnabledState.value
                    val desired = ! current
                    if (Settings.System.canWrite(this)) {
                        // Attempt the change. We flip the UI immediately on click (when we have/can obtain the perm)
                        // to make the toggle feel direct and reliable, as requested. The actual system value
                        // may be clamped by the device but the button state follows the user's intent.
                        val attempted = if (desired) {
                            NeverSleepController.setNeverSleep(this)
                        } else {
                            NeverSleepController.setNormal(this)
                        }
                        isEnabledState.value = desired
                    } else {
                        // No perm yet: remember intent, prompt, auto-apply on resume (after user grants)
                        pendingDesired = desired
                        openWriteSettingsPermissionScreen()
                    }
                }

                NeverSleepScreen(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                    showAds = showAds,
                    onSetShowAds = { enabled ->
                        showAdsState.value = enabled
                        NeverSleepController.setAdsEnabled(this, enabled)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh state when app comes back from background/minimized - this fixes stale toggle state
        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)

        // If we had a pending toggle because we prompted for permission, apply it now if we have write access
        if (pendingDesired != null && Settings.System.canWrite(this)) {
            val desired = pendingDesired!!
            pendingDesired = null
            val success = if (desired) {
                NeverSleepController.setNeverSleep(this)
            } else {
                NeverSleepController.setNormal(this)
            }
            // Flip UI to desired on resume after grant, for direct toggle experience.
            isEnabledState.value = desired
        }
    }

    private fun openWriteSettingsPermissionScreen() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}

@Composable
private fun NeverSleepScreen(
    onToggle: () -> Unit,
    isEnabled: Boolean,
    showAds: Boolean,
    onSetShowAds: (Boolean) -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        // Let the shader draw full-bleed under the status bar; insets are applied to content below
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Pure native modern Android shader (AGSL on 33+, Canvas fallback) - full screen
            NativeShaderBackground(modifier = Modifier.fillMaxSize())

            // Settings gear (top-right) - respect status bar insets
            Box(
                modifier = Modifier
                    .align(ComposeAlignment.TopEnd)
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(12.dp)
            ) {
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Main content: title + button always centered.
            // Apply top inset so it doesn't sit under the status bar.
            // Keep bottom reserve for the disclaimer + optional ad (these live in the bottom column).
            Column(
                modifier = Modifier
                    .align(ComposeAlignment.Center)
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = if (showAds) 230.dp else 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = "never sleep",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(48.dp))

            // The big beautiful circular toggle button (modern & simple)
            val haptic = LocalHapticFeedback.current
            var isPressed by remember { mutableStateOf(false) }

            val targetColor = if (isEnabled) Color(0xFF7C4DFF) else Color(0xFF2A2A40)
            val buttonColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "buttonColor"
            )

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = tween(120),
                label = "pressScale"
            )

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(buttonColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEnabled) "🌙" else "☀",
                        color = Color.White,
                        fontSize = 42.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isEnabled) "NEVER" else "NORMAL",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Bottom area: disclaimer (expandable scrollable) + ad box
        // Respect system insets (navigation bar / gesture bar) so content isn't covered.
        Column(
            modifier = Modifier
                .align(ComposeAlignment.BottomCenter)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxWidth()
        ) {
            DisclaimerSection(
                modifier = Modifier.fillMaxWidth()
            )
            if (showAds) {
                AdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }  // close Box
}  // close Scaffold

    // Settings dialog for ads toggle (and future settings)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text("Settings", color = Color.White)
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSetShowAds(!showAds) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Disable ads",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = !showAds,
                            onCheckedChange = { disabled -> onSetShowAds(!disabled) }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Hides the small ad box at the bottom. The rest of the app works the same.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

/**
 * Expandable disclaimer as a self-contained scrollable item.
 * Collapsed: just header (small fixed height)
 * Expanded: animates to known height (180.dp), content scrolls internally.
 * This way the main screen (button position) does not shift/"expand".
 */
@Composable
private fun DisclaimerSection(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    val collapsedHeight = 52.dp
    val expandedHeight = 180.dp  // predetermined size - enough to read the text with scrolling

    val targetHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "disclaimerHeight"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrowRotation"
    )

    Column(
        modifier = modifier
            .height(targetHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                expanded = !expanded
            }
            .background(Color(0xFF1A1A2E).copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Header always visible
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "About permissions & compliance",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "▼",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = arrowRotation
                }
            )
        }

        // Scrollable text - only when expanded, takes the remaining predetermined space
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "This app uses the 'Modify system settings' permission to control screen timeout. " +
                           "This is required for the 'never sleep' feature. " +
                           "Enabling this may increase battery usage and device temperature. " +
                           "Some devices (e.g. Samsung, Xiaomi) or enterprise policies may override this setting. " +
                           "You can revoke access anytime in Android Settings > Apps > Special app access.\n\n" +
                           "Privacy: This app does not collect, store, or share any personal or device data beyond the system setting change you explicitly request.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/**
 * Small ad box at the bottom for potential advertisements.
 * 
 * This is a stub implementation. All logic for loading, displaying, and consuming ads
 * is centralized here so it can be easily replaced with a real ad SDK (AdMob, etc.)
 * in the future.
 *
 * Current stub:
 * - Simulates async ad loading
 * - Shows a placeholder "ad" UI
 * - Provides hooks for impression / click tracking (stubs)
 * - Easy to swap the inner content with real ad view (e.g. AndroidView + AdView)
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    var adState by remember { mutableStateOf<AdState>(AdState.Loading) }

    // Stub: simulate loading an ad when the composable enters composition
    LaunchedEffect(Unit) {
        adState = loadAd()
    }

    Box(
        modifier = modifier
            .background(Color(0xFFE8E8E8))
            .border(1.dp, Color(0xFFCCCCCC))
            .clickable {
                if (adState is AdState.Loaded) {
                    consumeAd(adState as AdState.Loaded)
                    // In real impl: this would be the ad click handler from the SDK
                }
            }
    ) {
        when (val state = adState) {
            is AdState.Loading -> {
                Text(
                    "Loading ad...",
                    modifier = Modifier.align(ComposeAlignment.Center),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            is AdState.Loaded -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stub ad content - replace this block with real ad view later
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sponsored • Tap to learn more",
                            fontSize = 10.sp,
                            color = Color(0xFF666666)
                        )
                        Text(
                            "Your product or service here",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }
                    // Fake "Ad" badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF888888))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Ad",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is AdState.Error -> {
                Text(
                    "Ad unavailable",
                    modifier = Modifier.align(ComposeAlignment.Center),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// === Ad logic stubs ===

sealed interface AdState {
    object Loading : AdState
    data class Loaded(val adUnitId: String) : AdState
    object Error : AdState
}

/**
 * Stub for loading an ad.
 * 
 * Real implementation example (AdMob):
 * - Initialize MobileAds in Application
 * - Create AdRequest
 * - Load into AdView
 * - Listen to onAdLoaded, onAdFailedToLoad, etc.
 * - Return Loaded when ready
 */
private suspend fun loadAd(): AdState {
    // Simulate network delay for ad fetch
    delay(1200)
    
    // In real code you would call the ad SDK here.
    // For now we always succeed with a stub ad.
    return AdState.Loaded("ca-app-pub-3940256099942544/6300978111") // Google test unit id
}

/**
 * Stub for consuming / tracking an ad (impression or click).
 * 
 * Call this when:
 * - Ad becomes visible (impression)
 * - User taps the ad (click)
 * 
 * Real implementation:
 * - AdMob SDK automatically tracks most events when you use their views.
 * - For custom: use AdListener, or manual tracking pixels / your backend.
 */
private fun consumeAd(ad: AdState.Loaded) {
    Log.d("AdStub", "Ad consumed: ${ad.adUnitId}")
    
    // Example real hook:
    // adView.adListener?.onAdClicked?.invoke() or similar
    // Or send analytics event
}

/**
 * Modern native Android shader background.
 * - API 33+: AGSL RuntimeShader (official, GPU accelerated, best practice in 2026 Compose)
 * - Fallback: Canvas with animated radial gradients + grain (works on minSdk 31)
 *
 * Based on official Android docs and modern practices:
 * - RuntimeShader + ShaderBrush in drawWithCache
 * - Time uniform for animation
 * - Adapted mesh-gradient style from GLSL examples (e.g. Paper Shaders inspired)
 */
@Composable
fun NativeShaderBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= 33) {
        AgslMeshShaderBackground(modifier)
    } else {
        CanvasMeshShaderBackground(modifier)
    }
}

@RequiresApi(33)
@Composable
private fun AgslMeshShaderBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "agslTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val shaderCode = """
        uniform float2 iResolution;
        uniform float iTime;
        layout(color) uniform half4 iColor1;
        layout(color) uniform half4 iColor2;
        layout(color) uniform half4 iColor3;
        layout(color) uniform half4 iColor4;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution.xy;
            float t = iTime * 0.05;

            // Mesh-like flowing blobs (inspired by modern AGSL + Paper mesh)
            float2 p1 = float2(0.5 + 0.4 * sin(t), 0.5 + 0.3 * cos(t * 1.1));
            float2 p2 = float2(0.5 + 0.35 * cos(t * 0.8), 0.5 + 0.4 * sin(t * 1.3));
            float2 p3 = float2(0.5 + 0.3 * sin(t * 1.2 + 2.0), 0.5 + 0.35 * cos(t * 0.9));
            float2 p4 = float2(0.5 + 0.38 * cos(t * 1.4), 0.5 + 0.32 * sin(t * 1.5));

            float d1 = length(uv - p1);
            float d2 = length(uv - p2);
            float d3 = length(uv - p3);
            float d4 = length(uv - p4);

            float w1 = 1.0 / (d1 + 0.1);
            float w2 = 1.0 / (d2 + 0.1);
            float w3 = 1.0 / (d3 + 0.1);
            float w4 = 1.0 / (d4 + 0.1);
            float total = w1 + w2 + w3 + w4;

            half3 col = (iColor1.rgb * w1 + iColor2.rgb * w2 + iColor3.rgb * w3 + iColor4.rgb * w4) / total;

            col *= 0.4; // further dim for subtle background

            // Subtle noise/grain
            float noise = fract(sin(dot(uv + t * 0.01, float2(12.9898, 78.233))) * 43758.5453);
            col = mix(col, col * (0.95 + noise * 0.1), 0.15);

            return half4(col, 1.0);
        }
    """.trimIndent()

    val shader = remember { RuntimeShader(shaderCode) }
    val brush = remember { ShaderBrush(shader) }

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)

        // Dimmed colors for background (not too bright)
        val c1 = android.graphics.Color.valueOf(0f, 0.25f, 0.35f, 1f)
        val c2 = android.graphics.Color.valueOf(0.18f, 0.10f, 0.32f, 1f)
        val c3 = android.graphics.Color.valueOf(0.32f, 0.05f, 0.18f, 1f)
        val c4 = android.graphics.Color.valueOf(0f, 0.25f, 0.22f, 1f)

        shader.setColorUniform("iColor1", c1)
        shader.setColorUniform("iColor2", c2)
        shader.setColorUniform("iColor3", c3)
        shader.setColorUniform("iColor4", c4)

        drawRect(brush = brush)
    }
}

@Composable
private fun CanvasMeshShaderBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "canvasMesh")
    val t1 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), "t1")
    val t2 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(9500, easing = LinearEasing), RepeatMode.Restart), "t2")
    val t3 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart), "t3")

    val colors = remember {
        listOf(
            Color(0xFF006B7A), // muted cyan
            Color(0xFF3D1F5C), // muted purple
            Color(0xFF7A1F4A), // muted pink
            Color(0xFF006B5A), // muted teal
            Color(0xFF2A3A7A), // muted indigo
        )
    }

    Canvas(modifier = modifier) {
        drawRect(color = Color(0xFF0A0A18))

        val blobs = listOf(
            // lowered alphas for dimmer background
            Triple(0, t1, 0.95f) to 0.32f,
            Triple(1, t2 * 1.1f, 0.75f) to 0.28f,
            Triple(2, t3 * 0.85f, 1.05f) to 0.25f,
            Triple(3, (t1 + 0.3f) % 1f, 0.65f) to 0.30f,
            Triple(4, (t2 * 0.7f + 0.6f) % 1f, 0.85f) to 0.22f,
        )

        blobs.forEachIndexed { idx, (triple, alpha) ->
            val (colorIdx, phase, radiusFactor) = triple
            val color = colors[colorIdx % colors.size]
            val cx = size.width * (0.5f + 0.45f * sin(phase * 6.28f + idx * 1.9f))
            val cy = size.height * (0.5f + 0.38f * cos(phase * 5.3f + idx * 2.6f))
            val radius = size.minDimension * radiusFactor * 0.48f

            for (layer in 0..4) {
                val la = alpha * (1f - layer * 0.18f)
                val lr = radius * (1f + layer * 0.18f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = la), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = lr
                    ),
                    center = Offset(cx, cy),
                    radius = lr
                )
            }
        }

        // Subtle grain
        val ga = 0.028f
        val step = 28f
        for (x in 0 until (size.width / step).toInt()) {
            for (y in 0 until (size.height / step).toInt()) {
                val gx = x * step + ((x * 31 + y * 17) % 7)
                val gy = y * step + ((y * 23 + x * 11) % 5)
                val g = (sin(gx * 0.9f + t1 * 4f) * cos(gy * 1.1f + t2 * 3f) + 1f) * 0.5f
                drawCircle(
                    center = Offset(gx, gy),
                    radius = 1.1f,
                    color = Color.White.copy(alpha = ga * g)
                )
            }
        }
    }
}
