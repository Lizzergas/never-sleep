import { useCallback, useEffect, useRef, useState } from "react";
import { createToggleFace, type ToggleFace } from "./toggleFace";
import { modeLabel, nextMode, type ToggleMode } from "./toggleMode";

interface Props {
    /** Which face to show first. Defaults to "never" (the app's headline state). */
    defaultMode?: ToggleMode;
}

const GLOW_NEVER = 1;
const GLOW_NORMAL = 0.45;
// Eased-transition speed (per second). ~matches the app's 400ms glow tween.
const EASE_RATE = 8;
// Frozen clock used when the visitor prefers reduced motion (no nebula/rim animation).
const STILL_TIME = 0;

function prefersReducedMotion(): boolean {
    return (
        typeof window !== "undefined" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches
    );
}

/**
 * The live hero toggle — a web port of NeverSleepToggleButton. It renders a real
 * `role="switch"` button so it toggles by click, tap, Enter, and Space, with the WebGL
 * shader face ({@link createToggleFace}) layered over a CSS gradient fallback.
 */
export function AppToggle({ defaultMode = "never" }: Props) {
    const [mode, setMode] = useState<ToggleMode>(defaultMode);

    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const faceRef = useRef<HTMLSpanElement | null>(null);
    // Mode mirror + eased animation values, read inside the rAF loop without re-subscribing.
    const targetRef = useRef<ToggleMode>(defaultMode);
    const glowRef = useRef(defaultMode === "never" ? GLOW_NEVER : GLOW_NORMAL);
    const neverRef = useRef(defaultMode === "never" ? 1 : 0);

    targetRef.current = mode;

    const toggle = useCallback(() => setMode((m) => nextMode(m)), []);

    useEffect(() => {
        const canvas = canvasRef.current;
        const stage = faceRef.current;
        if (!canvas || !stage) return;

        const face: ToggleFace | null = createToggleFace(canvas);
        if (face) canvas.dataset.active = "true";

        const dpr = Math.min(2, typeof window !== "undefined" ? window.devicePixelRatio || 1 : 1);
        const applySize = () => {
            const rect = stage.getBoundingClientRect();
            if (rect.width > 0) face?.resize(rect.width, rect.height, dpr);
        };
        applySize();

        const observer = new ResizeObserver(applySize);
        observer.observe(stage);

        const reduced = prefersReducedMotion();
        let raf = 0;
        let start = 0;
        let last = 0;

        const frame = (now: number) => {
            if (start === 0) {
                start = now;
                last = now;
            }
            const dt = Math.min(0.05, (now - last) / 1000);
            last = now;

            const glowTarget = targetRef.current === "never" ? GLOW_NEVER : GLOW_NORMAL;
            const neverTarget = targetRef.current === "never" ? 1 : 0;
            const k = Math.min(1, dt * EASE_RATE);
            glowRef.current += (glowTarget - glowRef.current) * k;
            neverRef.current += (neverTarget - neverRef.current) * k;

            const time = reduced ? STILL_TIME : (now - start) / 12;
            face?.render({ time, glow: glowRef.current, neverMode: neverRef.current });

            const settled =
                Math.abs(glowRef.current - glowTarget) < 0.002 &&
                Math.abs(neverRef.current - neverTarget) < 0.002;
            // With motion enabled we animate forever (nebula + rim pulse); with reduced motion we
            // only run long enough to cross-fade to the new state, then stop.
            if (!reduced || !settled) raf = requestAnimationFrame(frame);
        };
        raf = requestAnimationFrame(frame);

        return () => {
            cancelAnimationFrame(raf);
            observer.disconnect();
            face?.dispose();
        };
        // Re-arm the loop on mode changes so reduced-motion visitors get the cross-fade frames.
    }, [mode]);

    return (
        <button
            type="button"
            role="switch"
            aria-checked={mode === "never"}
            aria-label="Never Sleep mode — keep the screen awake"
            className="app-toggle"
            data-mode={mode}
            onClick={toggle}
        >
            <span ref={faceRef} className="app-toggle__face" aria-hidden="true">
                <span className="app-toggle__gradient" />
                <canvas ref={canvasRef} className="app-toggle__canvas" />
                <span className="app-toggle__icon app-toggle__icon--moon">
                    <MoonIcon />
                </span>
                <span className="app-toggle__icon app-toggle__icon--sun">
                    <SunIcon />
                </span>
            </span>
            <span className="app-toggle__label">{modeLabel(mode)}</span>
        </button>
    );
}

function MoonIcon() {
    return (
        <svg viewBox="0 0 100 100" width="100%" height="100%" aria-hidden="true">
            <defs>
                <radialGradient id="ns-moon" cx="38%" cy="42%" r="70%">
                    <stop offset="0%" stopColor="#e8f4ff" />
                    <stop offset="45%" stopColor="#80d4ff" />
                    <stop offset="80%" stopColor="#7c4dff" />
                    <stop offset="100%" stopColor="#4a2080" />
                </radialGradient>
                <mask id="ns-crescent">
                    <circle cx="50" cy="50" r="26" fill="#fff" />
                    <circle cx="62" cy="44" r="23" fill="#000" />
                </mask>
            </defs>
            <circle cx="50" cy="50" r="26" fill="url(#ns-moon)" mask="url(#ns-crescent)" />
        </svg>
    );
}

function SunIcon() {
    const rays = Array.from({ length: 8 }, (_, i) => i * 45);
    return (
        <svg viewBox="0 0 100 100" width="100%" height="100%" aria-hidden="true">
            <defs>
                <radialGradient id="ns-sun" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stopColor="#fffde7" />
                    <stop offset="60%" stopColor="#ffb300" />
                    <stop offset="100%" stopColor="#ff8f00" />
                </radialGradient>
            </defs>
            <g stroke="#ffcc80" strokeWidth="2.4" strokeLinecap="round">
                {rays.map((deg) => (
                    <line
                        key={deg}
                        x1="50"
                        y1="20"
                        x2="50"
                        y2="30"
                        transform={`rotate(${deg} 50 50)`}
                    />
                ))}
            </g>
            <circle cx="50" cy="50" r="14" fill="url(#ns-sun)" />
        </svg>
    );
}
