import { MeshGradient } from "@paper-design/shaders-react";
import { useEffect, useState } from "react";

// These values mirror the site's dark-violet design tokens. The palette stays
// close in lightness so the motion reads as atmosphere, not a second subject.
const AMBIENT_COLORS = ["#0d0d1a", "#151328", "#241c42", "#49356f", "#72609c"];

export function HeroBackdrop() {
    const [motionEnabled, setMotionEnabled] = useState(false);

    useEffect(() => {
        const media = window.matchMedia("(prefers-reduced-motion: reduce)");
        const update = () => setMotionEnabled(!media.matches);
        update();
        media.addEventListener("change", update);
        return () => media.removeEventListener("change", update);
    }, []);

    return (
        <div className="hero-backdrop" aria-hidden="true">
            <MeshGradient
                className="hero-backdrop__mesh"
                colors={AMBIENT_COLORS}
                distortion={0.56}
                swirl={0.16}
                speed={motionEnabled ? 0.07 : 0}
                fit="cover"
            />
            <div className="hero-backdrop__veil" />
        </div>
    );
}
