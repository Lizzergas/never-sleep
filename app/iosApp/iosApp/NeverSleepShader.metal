#include <metal_stdlib>
using namespace metal;

static float hash21(float2 point) {
    point = fract(point * float2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

static float valueNoise(float2 point) {
    float2 cell = floor(point);
    float2 local = fract(point);
    local = local * local * local * (local * (local * 6.0 - 15.0) + 10.0);

    float a = hash21(cell);
    float b = hash21(cell + float2(1.0, 0.0));
    float c = hash21(cell + float2(0.0, 1.0));
    float d = hash21(cell + float2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

static float fbm(float2 point) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; octave++) {
        value += amplitude * valueNoise(point);
        point = float2(
            point.x * 0.80 - point.y * 0.60,
            point.x * 0.60 + point.y * 0.80
        ) * 2.0 + 7.3;
        amplitude *= 0.5;
    }
    return value;
}

static float fog(float2 uv, float2 drift) {
    float2 point = uv * 2.6 + drift;
    float2 warp = float2(fbm(point), fbm(point + 5.2));
    return fbm(point + warp * 0.7);
}

static float starLayer(
    float2 uv,
    float density,
    float time,
    float phase,
    float parallax
) {
    constexpr float tau = 6.28318530718;
    float2 drift = float2(
        sin(time * tau / 120.0 + phase) * 0.32,
        cos(time * tau / 150.0 + phase * 0.73) * 0.24
    ) * parallax;
    float2 grid = uv * density + drift;
    float2 cell = floor(grid);
    float2 local = fract(grid);
    float cellHash = hash21(cell);
    float present = step(0.93, cellHash);

    // Keep the complete glow inside its cell. The prior 0...1 positions let
    // the 0.22-radius halo cross a cell boundary, where it was cut flat.
    float2 position = mix(
        float2(0.24),
        float2(0.76),
        float2(hash21(cell + 3.1), hash21(cell + 7.7))
    );
    float distanceToStar = length(local - position);
    float core = 1.0 - smoothstep(0.0, 0.055, distanceToStar);
    float pulse = 0.5 + 0.5 * sin(time * tau / 8.0 + cellHash * 90.0);
    float haloRadius = mix(0.14, 0.22, pulse);
    float halo = 1.0 - smoothstep(0.0, haloRadius, distanceToStar);
    float brightness = mix(0.72, 1.08, pulse);
    float glow = mix(0.24, 0.60, pulse);
    return present * (core * brightness + glow * halo);
}

[[ stitchable ]] half4 neverSleepBackground(
    float2 position,
    half4 source,
    float2 size,
    float time,
    float mode
) {
    float2 uv = position / max(size, float2(1.0));
    float aspect = size.x / max(size.y, 1.0);

    float2 centered = uv - float2(0.5, 0.46);
    centered.x *= aspect;
    float radialDistance = length(centered);
    float bloom = smoothstep(0.70, 0.0, radialDistance);
    bloom *= bloom;
    bloom *= 0.93 + 0.07 * sin(time * 0.11);

    float raw = 0.928 * uv.x - 0.371 * uv.y;
    float windCoordinate = (raw + 0.371) / 1.30;
    float turbulence = fbm(uv * 3.0 + float2(time * 0.08, time * 0.05));
    float front = windCoordinate + (turbulence - 0.5) * 0.35;
    float edge = mode * 1.6 - 0.3;
    float sweep = smoothstep(front - 0.2, front + 0.2, edge);

    float3 normalColor = float3(0.05, 0.035, 0.045);
    normalColor += float3(1.0, 0.62, 0.28) * bloom * 0.9;
    float warmFog = fog(uv, float2(time * 0.011, -time * 0.006));
    normalColor += float3(0.60, 0.32, 0.15) * warmFog * 0.75;
    normalColor += float3(0.40, 0.13, 0.16) * warmFog * warmFog * 0.5;
    float rayAngle = atan2(centered.y, centered.x);
    float rays = 0.5 + 0.5 * sin(rayAngle * 6.0 + time * 0.045);
    rays *= smoothstep(0.66, 0.05, radialDistance);
    normalColor += float3(1.0, 0.72, 0.38) * rays * 0.06;

    float3 activeColor = float3(0.015, 0.02, 0.05);
    activeColor += float3(0.16, 0.09, 0.30) * bloom * 0.35;
    activeColor += float3(0.18, 0.32, 0.55) * bloom * 0.20;
    float coolFog = fog(uv + 11.0, float2(-time * 0.008, time * 0.011));
    activeColor += float3(0.11, 0.10, 0.30) * coolFog * 0.8;
    activeColor += float3(0.05, 0.15, 0.32) * coolFog * coolFog * 0.45;
    float2 starUV = uv * float2(aspect, 1.0);
    float stars = starLayer(starUV, 16.0, time, 0.0, 1.0)
        + 0.7 * starLayer(starUV, 27.0, time, 2.4, 0.65);
    activeColor += float3(0.80, 0.85, 1.0) * stars * (1.0 - bloom * 0.5);

    float3 color = mix(normalColor, activeColor, sweep);

    float band = smoothstep(0.30, 0.5, sweep) * smoothstep(0.70, 0.5, sweep);
    float flash = 4.0 * mode * (1.0 - mode);
    float3 streak = mix(float3(1.0, 0.7, 0.35), float3(0.45, 0.65, 1.0), mode);
    color += streak * band * flash * 0.5;

    float verticalVignette = smoothstep(0.0, 0.16, uv.y) * smoothstep(1.0, 0.84, uv.y);
    color *= mix(0.55, 1.0, verticalVignette);

    float grain = (hash21(position) - 0.5) / 255.0;
    color += grain;

    return half4(half3(clamp(color, 0.0, 1.0)), source.a);
}

[[ stitchable ]] half4 neverSleepControlFace(
    float2 position,
    half4 source,
    float2 size,
    float time,
    float mode
) {
    float2 uv = position / max(size, float2(1.0));
    float2 centered = uv - 0.5;
    float radius = length(centered) * 2.0;
    float angle = atan2(centered.y, centered.x);

    float3 normalCore = float3(0.055, 0.038, 0.027);
    float3 activeCore = float3(0.045, 0.026, 0.105);
    float3 color = mix(normalCore, activeCore, mode);

    float centerLight = exp(-5.4 * radius);
    color += mix(float3(0.34, 0.18, 0.055), float3(0.22, 0.09, 0.48), mode) * centerLight;

    float ringA = exp(-280.0 * pow(radius - 0.83, 2.0));
    float ringB = exp(-160.0 * pow(radius - 0.70, 2.0));
    float orbit = 0.5 + 0.5 * sin(angle * 3.0 - time * 0.24);
    float3 rim = mix(
        float3(0.64, 0.39, 0.12),
        mix(float3(0.20, 0.70, 0.92), float3(0.48, 0.20, 1.0), orbit),
        mode
    );
    color += rim * ringA * mix(0.19, 0.46, mode);
    color += rim * ringB * mix(0.035, 0.10, mode);

    float microFog = fbm(uv * 4.6 + float2(time * 0.012, -time * 0.008));
    color += rim * smoothstep(0.60, 0.92, microFog) * (1.0 - radius) * 0.055;

    float edge = smoothstep(1.0, 0.86, radius);
    color *= edge;
    return half4(half3(clamp(color, 0.0, 1.0)), source.a * half(edge));
}
