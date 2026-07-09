import { GodRays, MeshGradient } from "@paper-design/shaders-react";
import { useEffect, useRef, useState } from "react";

// One full left↔right drift of the ray source. Deliberately long so it reads as
// "alive" rather than "animated" — a slow searchlight, never a strobe.
const SWAY_PERIOD_S = 28;
const SWAY_AMOUNT = 0.14;

export function HeroBackdrop() {
    const [motionEnabled, setMotionEnabled] = useState(true);
    const [sway, setSway] = useState(0);
    const lastSway = useRef(0);

    useEffect(() => {
        const media = window.matchMedia("(prefers-reduced-motion: reduce)");
        const update = () => setMotionEnabled(!media.matches);
        update();
        media.addEventListener("change", update);
        return () => media.removeEventListener("change", update);
    }, []);

    useEffect(() => {
        if (!motionEnabled) {
            setSway(0);
            return;
        }
        let raf = 0;
        let start = 0;
        const loop = (now: number) => {
            if (start === 0) start = now;
            const t = (now - start) / 1000;
            const value = Math.sin((t / SWAY_PERIOD_S) * Math.PI * 2);
            // Only re-render when the drift moved enough to see — a sway this slow
            // doesn't need to reconcile the shader tree every frame.
            if (Math.abs(value - lastSway.current) > 0.008) {
                lastSway.current = value;
                setSway(value);
            }
            raf = requestAnimationFrame(loop);
        };
        raf = requestAnimationFrame(loop);
        return () => cancelAnimationFrame(raf);
    }, [motionEnabled]);

    const meshSpeed = motionEnabled ? 0.22 : 0;
    const raysSpeed = motionEnabled ? 0.26 : 0;

    return (
        <div className="hero-backdrop" aria-hidden="true">
            <MeshGradient
                className="hero-backdrop__mesh"
                colors={["#0d0d1a", "#1a1035", "#7c4dff", "#3d2a6e", "#9e8cff"]}
                distortion={0.82}
                swirl={0.42}
                speed={meshSpeed}
                fit="cover"
            />
            <GodRays
                className="hero-backdrop__rays"
                colors={["#7c4dff", "#9e8cff", "#4a3080", "#1a1035"]}
                colorBack="#0d0d1a00"
                colorBloom="#7c4dff"
                bloom={0.26}
                intensity={0.46}
                density={0.55}
                spotty={0.45}
                midSize={0.28}
                midIntensity={0.55}
                speed={raysSpeed}
                offsetX={sway * SWAY_AMOUNT}
                offsetY={-0.42}
                fit="cover"
            />
            <div className="hero-backdrop__veil" />
        </div>
    );
}
