import { GodRays, MeshGradient } from "@paper-design/shaders-react";
import { useEffect, useState } from "react";

export function HeroBackdrop() {
    const [motionEnabled, setMotionEnabled] = useState(true);

    useEffect(() => {
        const media = window.matchMedia("(prefers-reduced-motion: reduce)");
        const update = () => setMotionEnabled(!media.matches);
        update();
        media.addEventListener("change", update);
        return () => media.removeEventListener("change", update);
    }, []);

    const meshSpeed = motionEnabled ? 0.14 : 0;
    const raysSpeed = motionEnabled ? 0.18 : 0;

    return (
        <div className="hero-backdrop" aria-hidden="true">
            <MeshGradient
                className="hero-backdrop__mesh"
                colors={["#0d0d1a", "#1a1035", "#7c4dff", "#3d2a6e", "#9e8cff"]}
                distortion={0.72}
                swirl={0.35}
                speed={meshSpeed}
                fit="cover"
            />
            <GodRays
                className="hero-backdrop__rays"
                colors={["#7c4dff", "#9e8cff", "#4a3080", "#1a1035"]}
                colorBack="#0d0d1a00"
                colorBloom="#7c4dff"
                bloom={0.22}
                intensity={0.42}
                density={0.55}
                spotty={0.45}
                midSize={0.28}
                midIntensity={0.55}
                speed={raysSpeed}
                offsetY={-0.42}
                fit="cover"
            />
            <div className="hero-backdrop__veil" />
        </div>
    );
}
