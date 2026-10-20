import { useLayoutEffect, useRef } from "react";
import { BACKGROUND_SHADER, FACE_SHADER } from "./shaders.generated";
import { createShader } from "./shader";
import { drawBrandIcon } from "./brandIcon";
import { ANDROID_TIMING } from "./animation";
import "./app-screen.css";

export interface AppScreenProps {
    enabled: boolean;
    night: number;
    glow: number;
    timeMs: number;
    clock?: string;
    onToggle?: () => void;
    fontUrl?: string;
}

/** A 360 × 780 dp Android viewport, with ads hidden as in screenshot mode.
 * Layout: 24 dp system insets, 18 sp title + 16/8 dp padding, 32 dp gap,
 * 210 dp toggle. Matches NeverSleepScreen and NeverSleepTheme.
 */
export function AppScreen({
    enabled,
    night,
    glow,
    timeMs,
    clock = "9:41",
    onToggle,
    fontUrl = "/fonts/Roboto.ttf",
}: AppScreenProps) {
    const background = useRef<HTMLCanvasElement>(null);
    const face = useRef<HTMLCanvasElement>(null);
    const icon = useRef<HTMLCanvasElement>(null);
    const bloom = useRef<HTMLCanvasElement>(null);
    const renderers = useRef<{
        background: ReturnType<typeof createShader>;
        face: ReturnType<typeof createShader>;
    } | null>(null);
    useLayoutEffect(() => {
        const bg = createShader(background.current!, BACKGROUND_SHADER);
        const button = createShader(face.current!, FACE_SHADER);
        bg?.resize(360, 780, 3);
        button?.resize(210, 210, 3);
        renderers.current = { background: bg, face: button };
        return () => {
            bg?.dispose();
            button?.dispose();
        };
    }, []);
    useLayoutEffect(() => {
        renderers.current?.background?.draw({
            iMode: night,
            iTime: (timeMs % ANDROID_TIMING.backgroundLoop) / 40,
        });
        renderers.current?.face?.draw({
            iNeverMode: enabled ? 0 : 1,
            iGlow: glow,
            iTime: (timeMs % ANDROID_TIMING.faceLoop) / 12,
        });
    }, [enabled, night, glow, timeMs]);
    useLayoutEffect(() => {
        drawBrandIcon(icon.current!, enabled, 210, 3);
    }, [enabled]);
    useLayoutEffect(() => {
        const canvas = bloom.current!;
        const size = 210 * 1.14;
        canvas.width = Math.round(size * 3);
        canvas.height = Math.round(size * 3);
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.scale(3, 3);
        const center = size / 2;
        const radius = size * 0.52;
        const color = enabled ? "255,183,77" : "124,77,255";
        const gradient = ctx.createRadialGradient(center, center, 0, center, center, radius);
        gradient.addColorStop(0, `rgba(${color},${0.35 * glow})`);
        gradient.addColorStop(0.5, `rgba(${color},${0.08 * glow})`);
        gradient.addColorStop(1, `rgba(${color},0)`);
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, size, size);
    }, [enabled, glow]);
    return (
        <div className="android-screen" data-mode={enabled ? "on" : "off"}>
            <style>{`@font-face { font-family: "Android Roboto"; src: url("${fontUrl}") format("truetype"); font-style: normal; font-weight: 100 900; font-display: swap; }`}</style>
            <canvas ref={background} className="android-background" aria-hidden="true" />
            <div className="android-status" aria-hidden="true">
                <span>{clock}</span>
                <svg width="44" height="14" viewBox="0 0 44 14" fill="currentColor">
                    <path d="M0 3Q8-3 16 3L8 11Z" />
                    <path d="M20 12L30 2V12Z" />
                    <path d="M36 2H38V0H41V2H43V13H36Z" />
                </svg>
            </div>
            <div className="android-content">
                <div className="android-title">Keep screen on</div>
                <button
                    className="android-toggle"
                    role="switch"
                    type="button"
                    aria-label="Keep screen on"
                    aria-checked={enabled}
                    onClick={onToggle}
                    tabIndex={onToggle ? 0 : -1}
                >
                    <canvas ref={bloom} className="android-bloom" aria-hidden="true" />
                    <canvas ref={face} className="android-face" aria-hidden="true" />
                    <canvas ref={icon} className="android-icon" aria-hidden="true" />
                    <span className="android-label">{enabled ? "ON" : "OFF"}</span>
                </button>
            </div>
            <div className="android-navigation" aria-hidden="true" />
        </div>
    );
}
