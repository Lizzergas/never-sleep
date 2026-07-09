package com.lizz.neversleep

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TOGGLE_FACE_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iGlow;
uniform float iNeverMode;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv - 0.5;
    float dist = length(p) * 2.0;

    half3 deep = half3(0.04, 0.04, 0.10);
    half3 purple = half3(0.14, 0.08, 0.26);
    half3 warm = half3(0.16, 0.12, 0.10);
    half3 base = mix(warm, mix(deep, purple, smoothstep(0.0, 0.7, 1.0 - dist)), iNeverMode);

    float t = iTime * 0.001;
    float nebula = 0.06 * sin(p.x * 9.0 + t) * sin(p.y * 7.0 - t * 1.3);
    base += half3(nebula * iNeverMode);

    float rim = smoothstep(0.78, 0.92, dist) * smoothstep(1.02, 0.86, dist);
    float pulse = 0.82 + 0.18 * sin(t * 4.5);
    half3 cyan = half3(0.2, 0.85, 0.95);
    half3 violet = half3(0.55, 0.35, 1.0);
    half3 rimColor = mix(half3(0.45, 0.55, 0.75), mix(cyan, violet, 0.5 + 0.5 * sin(t * 2.0)), iNeverMode);
    base += rimColor * rim * pulse * iGlow;

    float vignette = smoothstep(1.1, 0.35, dist);
    base *= vignette;

    return half4(base, 1.0);
}
"""

/**
 * Main toggle — matches launcher icon branding:
 * - API 33+: AGSL [RuntimeShader] button face + animated neon rim (ShaderBrush in Canvas;
 *   see Android docs "Using RuntimeShader with Jetpack Compose")
 * - API 31–32: Canvas radial-gradient fallback
 * - Vector crescent moon / sun (no emoji)
 */
@Composable
fun NeverSleepToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val glowStrength by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.45f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "glowStrength",
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )

    val infinite = rememberInfiniteTransition(label = "toggleShader")
    val shaderTime by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shaderTime",
    )

    val faceShader = if (Build.VERSION.SDK_INT >= 33) {
        remember { RuntimeShader(TOGGLE_FACE_SHADER) }
    } else {
        null
    }

    Box(
        modifier = modifier
            .size(210.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val bloomColor = if (isEnabled) Color(0xFF7C4DFF) else Color(0xFF4A6080)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloomColor.copy(alpha = 0.35f * glowStrength),
                        bloomColor.copy(alpha = 0.08f * glowStrength),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.52f,
                ),
                center = center,
                radius = size.minDimension * 0.52f,
            )
        }

        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .pointerInput(onToggle) {
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
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                if (faceShader != null) {
                    faceShader.setFloatUniform("iResolution", size.width, size.height)
                    faceShader.setFloatUniform("iTime", shaderTime)
                    faceShader.setFloatUniform("iGlow", glowStrength)
                    faceShader.setFloatUniform("iNeverMode", if (isEnabled) 1f else 0f)
                    drawCircle(
                        brush = ShaderBrush(faceShader),
                        radius = radius,
                        center = center,
                    )
                } else {
                    drawToggleFaceFallback(
                        isNeverMode = isEnabled,
                        glowStrength = glowStrength,
                        time = shaderTime,
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isEnabled) {
                    drawBrandMoon(center = Offset(size.width / 2f, size.height * 0.38f))
                } else {
                    drawBrandSun(center = Offset(size.width / 2f, size.height * 0.38f))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Spacer(Modifier.height(118.dp))
                Text(
                    text = if (isEnabled) "NEVER" else "NORMAL",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

private fun DrawScope.drawToggleFaceFallback(
    isNeverMode: Boolean,
    glowStrength: Float,
    time: Float,
) {
    val radius = size.minDimension / 2f
    val center = this.center

    val coreColors = if (isNeverMode) {
        listOf(Color(0xFF2A1848), Color(0xFF12122A), Color(0xFF08081A))
    } else {
        listOf(Color(0xFF3A3548), Color(0xFF1E1E2E), Color(0xFF101018))
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = coreColors,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )

    val rimColor = if (isNeverMode) Color(0xFF7C4DFF) else Color(0xFF6A8AAA)
    val pulse = 0.85f + 0.15f * sin(time * 0.004f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                rimColor.copy(alpha = 0.55f * glowStrength * pulse),
                Color.Transparent,
            ),
            center = center,
            radius = radius * 1.02f,
        ),
        radius = radius * 0.98f,
        center = center,
        style = Stroke(width = radius * 0.08f),
    )
}

private fun DrawScope.drawBrandMoon(center: Offset) {
    val moonRadius = size.minDimension * 0.17f

    for (layer in 4 downTo 1) {
        val alpha = 0.07f * layer
        val r = moonRadius * (1.25f + layer * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = alpha),
                    Color(0xFF7C4DFF).copy(alpha = alpha * 0.6f),
                    Color.Transparent,
                ),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8F4FF),
                Color(0xFF80D4FF),
                Color(0xFF7C4DFF),
                Color(0xFF4A2080),
            ),
            center = center - Offset(moonRadius * 0.12f, moonRadius * 0.08f),
            radius = moonRadius * 1.1f,
        ),
        radius = moonRadius,
        center = center,
    )

    drawCircle(
        color = Color(0xFF14102A),
        radius = moonRadius * 0.88f,
        center = center + Offset(moonRadius * 0.42f, -moonRadius * 0.06f),
        blendMode = BlendMode.SrcOver,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = 0.9f),
                Color.Transparent,
            ),
            center = center - Offset(moonRadius * 0.35f, 0f),
            radius = moonRadius * 0.55f,
        ),
        radius = moonRadius * 0.5f,
        center = center - Offset(moonRadius * 0.2f, 0f),
        blendMode = BlendMode.Screen,
    )

    drawIconStars(center, moonRadius, cool = true)
}

private fun DrawScope.drawBrandSun(center: Offset) {
    val sunRadius = size.minDimension * 0.11f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF8E7).copy(alpha = 0.5f),
                Color(0xFFFFB74D).copy(alpha = 0.25f),
                Color.Transparent,
            ),
            center = center,
            radius = sunRadius * 2.2f,
        ),
        radius = sunRadius * 2.2f,
        center = center,
    )

    val rayCount = 8
    val rayInner = sunRadius * 1.15f
    val rayOuter = sunRadius * 1.75f
    for (i in 0 until rayCount) {
        val angle = (i.toFloat() / rayCount) * 2f * PI.toFloat()
        val cosA = cos(angle)
        val sinA = sin(angle)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFCC80).copy(alpha = 0.85f),
                    Color(0xFFFFCC80).copy(alpha = 0f),
                ),
                start = center + Offset(cosA * rayInner, sinA * rayInner),
                end = center + Offset(cosA * rayOuter, sinA * rayOuter),
            ),
            start = center + Offset(cosA * rayInner, sinA * rayInner),
            end = center + Offset(cosA * rayOuter, sinA * rayOuter),
            strokeWidth = sunRadius * 0.14f,
            cap = StrokeCap.Round,
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFB300), Color(0xFFFF8F00)),
            center = center,
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = center,
    )

    drawIconStars(center, sunRadius, cool = false)
}

private fun DrawScope.drawIconStars(
    anchor: Offset,
    radius: Float,
    cool: Boolean,
) {
    val starColor = if (cool) Color(0xFFB8E4FF) else Color(0xFFFFE082)
    val offsets = listOf(
        Offset(-radius * 1.4f, radius * 0.5f),
        Offset(radius * 1.1f, radius * 0.9f),
        Offset(radius * 0.3f, -radius * 1.2f),
        Offset(-radius * 0.9f, -radius * 0.7f),
    )
    val sizes = listOf(2.5f, 1.8f, 2f, 1.5f)
    offsets.zip(sizes).forEach { (offset, starSize) ->
        drawCircle(
            color = starColor.copy(alpha = 0.75f),
            radius = starSize,
            center = anchor + offset,
        )
    }
}
