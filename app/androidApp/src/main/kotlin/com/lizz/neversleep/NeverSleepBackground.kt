package com.lizz.neversleep

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen background that morphs with the toggle: a warm "dawn" for Normal mode
 * and a dark, star-lit night for Never mode. The change plays as a wind-warped
 * "swoosh" sweeping the new state across the screen.
 *
 * Deep module: the whole interface is `NeverSleepBackground(isEnabled, modifier)`.
 * Everything else — the AGSL shader, the API-31/32 Canvas fallback, the eased mode
 * animation, and the palette maths — stays hidden. Mirrors the API-guard pattern in
 * [NeverSleepToggleButton]; the light is anchored on the toggle so brightness stays
 * centred and the status bar / bottom panel remain legible.
 */
@Composable
fun NeverSleepBackground(
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    // 0 = Normal (warm dawn), 1 = Never (dark + stars). Eased so the swoosh glides
    // rather than snaps (the button snaps its own iNeverMode; the background must not).
    val mode by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "neverMode",
    )

    if (Build.VERSION.SDK_INT >= 33) {
        AgslMorphBackground(mode = mode, modifier = modifier)
    } else {
        CanvasMorphBackground(mode = mode, modifier = modifier)
    }
}

// AGSL fragment shader. `iMode` eases 0→1; the sweep threshold is offset per-pixel by
// flowing fbm noise so the transition front reads as a gust, not a straight wipe.
private const val MORPH_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iMode;

float hash21(float2 p) {
    float2 h = fract(p * float2(123.34, 456.21));
    h += dot(h, h + 45.32);
    return fract(h.x * h.y);
}

// ~37° rotation applied per fbm octave so value-noise lattices never line up
// into axis-aligned bands ("scanlines").
float2 rot37(float2 p) {
    return float2(p.x * 0.80 - p.y * 0.60, p.x * 0.60 + p.y * 0.80);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    // Quintic smoothstep — smoother than cubic, hides the grid.
    float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    float2 pp = p;
    for (int i = 0; i < 4; i++) {
        v += amp * vnoise(pp);
        pp = rot37(pp) * 2.0 + 7.3;
        amp = amp * 0.5;
    }
    return v;
}

// Billowy fog via domain warping (fbm of fbm) — soft, cloudy, no banding.
float fog(float2 uv, float2 drift) {
    float2 p = uv * 2.6 + drift;
    float2 warp = float2(fbm(p), fbm(p + 5.2));
    return fbm(p + warp * 0.7);
}

float starLayer(float2 uv, float density, float time) {
    float2 g = uv * density;
    float2 id = floor(g);
    float2 f = fract(g);
    float present = step(0.93, hash21(id));
    float2 pos = float2(hash21(id + 3.1), hash21(id + 7.7));
    float d = length(f - pos);
    float core = smoothstep(0.055, 0.0, d) + 0.35 * smoothstep(0.22, 0.0, d);
    float tw = 0.6 + 0.4 * sin(time * 0.02 + hash21(id) * 90.0);
    return present * core * tw;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;

    // Radial glow anchored on the toggle (screen centre, slightly high).
    float2 q = uv - float2(0.5, 0.46);
    q.x = q.x * aspect;
    float dist = length(q);
    float bloom = smoothstep(0.70, 0.0, dist);
    bloom = bloom * bloom;
    bloom = bloom * (0.93 + 0.07 * sin(iTime * 0.11));

    // Wind-warped sweep between the two states (the "swoosh").
    float raw = 0.928 * uv.x - 0.371 * uv.y;
    float wc = (raw + 0.371) / 1.30;
    float turb = fbm(uv * 3.0 + float2(iTime * 0.08, iTime * 0.05));
    float front = wc + (turb - 0.5) * 0.35;
    float edge = iMode * 1.6 - 0.3;
    float sweep = smoothstep(front - 0.2, front + 0.2, edge);

    // --- Normal: warm, cloudy dawn ---
    half3 normalCol = half3(0.05, 0.035, 0.045);
    normalCol = normalCol + half3(1.0, 0.62, 0.28) * bloom * 0.9;
    if (sweep < 0.995) {
        float warmFog = fog(uv, float2(iTime * 0.011, -iTime * 0.006));
        normalCol = normalCol + half3(0.60, 0.32, 0.15) * warmFog * 0.75;
        normalCol = normalCol + half3(0.40, 0.13, 0.16) * warmFog * warmFog * 0.5;
        float ang = atan(q.y, q.x);
        float rays = 0.5 + 0.5 * sin(ang * 6.0 + iTime * 0.045);
        rays = rays * smoothstep(0.66, 0.05, dist);
        normalCol = normalCol + half3(1.0, 0.72, 0.38) * rays * 0.06;
    }

    // --- Never: dark, foggy night + stars ---
    half3 neverCol = half3(0.015, 0.02, 0.05);
    neverCol = neverCol + half3(0.16, 0.09, 0.30) * bloom * 0.35;
    neverCol = neverCol + half3(0.18, 0.32, 0.55) * bloom * 0.20;
    if (sweep > 0.005) {
        float coolFog = fog(uv + 11.0, float2(-iTime * 0.008, iTime * 0.011));
        neverCol = neverCol + half3(0.11, 0.10, 0.30) * coolFog * 0.8;
        neverCol = neverCol + half3(0.05, 0.15, 0.32) * coolFog * coolFog * 0.45;
        float stars = starLayer(uv * float2(aspect, 1.0), 16.0, iTime)
            + 0.7 * starLayer(uv * float2(aspect, 1.0), 27.0, iTime + 50.0);
        neverCol = neverCol + half3(0.80, 0.85, 1.0) * stars * (1.0 - bloom * 0.5);
    }

    half3 col = mix(normalCol, neverCol, sweep);

    // Light-streak riding the moving front — peaks mid-transition, colour follows destination.
    float band = smoothstep(0.30, 0.5, sweep) * smoothstep(0.70, 0.5, sweep);
    float flash = 4.0 * iMode * (1.0 - iMode);
    half3 streak = mix(half3(1.0, 0.7, 0.35), half3(0.45, 0.65, 1.0), iMode);
    col = col + streak * band * flash * 0.5;

    // Keep the top (status bar) and bottom (disclaimer panel) darker for legibility.
    float vY = smoothstep(0.0, 0.16, uv.y) * smoothstep(1.0, 0.84, uv.y);
    col = col * mix(0.55, 1.0, vY);

    // Per-pixel dither to break 8-bit gradient banding on the smooth glows.
    float dither = (hash21(fragCoord) - 0.5) / 255.0;
    col = col + half3(dither);

    return half4(col, 1.0);
}
"""

@RequiresApi(33)
@Composable
private fun AgslMorphBackground(
    mode: Float,
    modifier: Modifier,
) {
    // Compile defensively: a malformed shader or an unsupported GPU driver would
    // otherwise throw at construction. On failure we degrade to the Canvas version
    // instead of crashing the home screen.
    val shader = remember { runCatching { RuntimeShader(MORPH_SHADER) }.getOrNull() }
    if (shader == null) {
        CanvasMorphBackground(mode = mode, modifier = modifier)
        return
    }
    val brush = remember(shader) { ShaderBrush(shader) }

    val transition = rememberInfiniteTransition(label = "bgTime")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iMode", mode)
        drawRect(brush = brush)
    }
}

/**
 * API 31–32 fallback: a clean crossfade of the warm/cold centre bloom plus a
 * star field that fades in with [mode]. No per-pixel wind wipe, but the same feel.
 */
@Composable
private fun CanvasMorphBackground(
    mode: Float,
    modifier: Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "bgFallback")
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "twinkle",
    )

    // Fixed star field (x, y, twinkle phase), seeded so it stays put across recompositions.
    val stars = remember {
        val rnd = Random(1234)
        List(64) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.46f)

        drawRect(color = lerp(Color(0xFF0C0806), Color(0xFF06060F), mode))

        val bloomColor = lerp(Color(0xFFE0954A), Color(0xFF44589A), mode)
        val radius = size.minDimension * 0.7f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(bloomColor.copy(alpha = 0.5f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            center = center,
            radius = radius,
        )

        if (mode > 0.01f) {
            stars.forEach { (sx, sy, phase) ->
                val cx = sx * w
                val cy = sy * h
                val distNorm = (hypot(cx - center.x, cy - center.y) / size.minDimension).coerceIn(0f, 1f)
                val tw = 0.5f + 0.5f * sin(twinkle + phase * 6.28f)
                val alpha = (mode * tw * (0.35f + 0.65f * distNorm) * 0.75f).coerceIn(0f, 0.85f)
                drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.6f, center = Offset(cx, cy))
            }
        }

        // Vignette top (status bar) and bottom (disclaimer panel).
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                startY = 0f,
                endY = h * 0.16f,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                startY = h * 0.82f,
                endY = h,
            ),
        )
    }
}
