/**
 * WebGL port of the app's AGSL toggle face (NeverSleepToggleButton.kt: TOGGLE_FACE_SHADER).
 *
 * Deep module: the whole interface is {@link createToggleFace} + {@link ToggleFace}. Everything
 * about WebGL — context, program, uniforms, the fullscreen triangle, DPR sizing — stays hidden.
 * Returns `null` when WebGL is unavailable so the caller can fall back to a static CSS face,
 * mirroring the app's own API 31–32 canvas fallback.
 */

/** One frame's worth of shader inputs. Units match the AGSL original so the look is identical. */
export interface ToggleFaceFrame {
    /** Clock fed to `iTime`. App scale: 0→1000 across ~12s (`elapsedMs / 12`). */
    time: number;
    /** `iGlow` — rim brightness. App eases 0.45 (normal) ↔ 1.0 (never). */
    glow: number;
    /** `iNeverMode` — 0 = normal (warm), 1 = never (purple/nebula). Eased for a smooth cross-fade. */
    neverMode: number;
}

export interface ToggleFace {
    /** Draw one frame. Cheap enough to call every rAF tick. */
    render(frame: ToggleFaceFrame): void;
    /** Match the drawing buffer to a CSS size (device-pixel-ratio aware). */
    resize(cssWidth: number, cssHeight: number, dpr: number): void;
    /** Release GL resources. */
    dispose(): void;
}

// Fullscreen triangle — no vertex buffer gymnastics, the fragment shader does all the work.
const VERTEX_SRC = `
attribute vec2 aPos;
void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
}
`;

// Ported 1:1 from TOGGLE_FACE_SHADER (AGSL half3/half4 -> GLSL vec3/vec4). The trailing
// circular mask keeps the square canvas transparent outside the disc so corners stay clean.
const FRAGMENT_SRC = `
precision highp float;
uniform vec2 iResolution;
uniform float iTime;
uniform float iGlow;
uniform float iNeverMode;

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution;
    vec2 p = uv - 0.5;
    float dist = length(p) * 2.0;

    vec3 deep = vec3(0.04, 0.04, 0.10);
    vec3 purple = vec3(0.14, 0.08, 0.26);
    vec3 warm = vec3(0.16, 0.12, 0.10);
    vec3 base = mix(warm, mix(deep, purple, smoothstep(0.0, 0.7, 1.0 - dist)), iNeverMode);

    float t = iTime * 0.001;
    float nebula = 0.06 * sin(p.x * 9.0 + t) * sin(p.y * 7.0 - t * 1.3);
    base += vec3(nebula * iNeverMode);

    float rim = smoothstep(0.78, 0.92, dist) * smoothstep(1.02, 0.86, dist);
    float pulse = 0.82 + 0.18 * sin(t * 4.5);
    vec3 cyan = vec3(0.2, 0.85, 0.95);
    vec3 violet = vec3(0.55, 0.35, 1.0);
    vec3 rimColor = mix(vec3(0.45, 0.55, 0.75), mix(cyan, violet, 0.5 + 0.5 * sin(t * 2.0)), iNeverMode);
    base += rimColor * rim * pulse * iGlow;

    float vignette = smoothstep(1.1, 0.35, dist);
    base *= vignette;

    float mask = smoothstep(1.0, 0.985, dist);
    gl_FragColor = vec4(base, mask);
}
`;

/** Shader sources, exported so tests can guard the AGSL→GLSL port without a GL context. */
export const TOGGLE_FACE_SHADER_SOURCE = { vertex: VERTEX_SRC, fragment: FRAGMENT_SRC } as const;

function compile(gl: WebGLRenderingContext, type: number, src: string): WebGLShader | null {
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, src);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        gl.deleteShader(shader);
        return null;
    }
    return shader;
}

/**
 * Build a toggle face on `canvas`, or return `null` if WebGL / shader compilation is unavailable.
 * The `null` path is expected (older hardware, blocked WebGL, SSR) — treat it as "use CSS fallback",
 * never an error.
 */
export function createToggleFace(canvas: HTMLCanvasElement): ToggleFace | null {
    const gl =
        canvas.getContext("webgl", { premultipliedAlpha: false, antialias: true }) ??
        (canvas.getContext("experimental-webgl", {
            premultipliedAlpha: false,
        }) as WebGLRenderingContext | null);
    if (!gl) return null;

    const vertex = compile(gl, gl.VERTEX_SHADER, VERTEX_SRC);
    const fragment = compile(gl, gl.FRAGMENT_SHADER, FRAGMENT_SRC);
    const program = gl.createProgram();
    if (!vertex || !fragment || !program) return null;

    gl.attachShader(program, vertex);
    gl.attachShader(program, fragment);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return null;

    gl.useProgram(program);

    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const aPos = gl.getAttribLocation(program, "aPos");
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

    const uResolution = gl.getUniformLocation(program, "iResolution");
    const uTime = gl.getUniformLocation(program, "iTime");
    const uGlow = gl.getUniformLocation(program, "iGlow");
    const uNeverMode = gl.getUniformLocation(program, "iNeverMode");

    return {
        render({ time, glow, neverMode }) {
            gl.uniform2f(uResolution, gl.drawingBufferWidth, gl.drawingBufferHeight);
            gl.uniform1f(uTime, time);
            gl.uniform1f(uGlow, glow);
            gl.uniform1f(uNeverMode, neverMode);
            gl.drawArrays(gl.TRIANGLES, 0, 3);
        },
        resize(cssWidth, cssHeight, dpr) {
            const w = Math.max(1, Math.round(cssWidth * dpr));
            const h = Math.max(1, Math.round(cssHeight * dpr));
            canvas.width = w;
            canvas.height = h;
            gl.viewport(0, 0, w, h);
        },
        dispose() {
            gl.deleteBuffer(buffer);
            gl.deleteProgram(program);
            gl.deleteShader(vertex);
            gl.deleteShader(fragment);
        },
    };
}
