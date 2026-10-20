/** Canvas counterpart of drawBrandSun / drawBrandMoon in NeverSleepToggleButton.kt.
 * Coordinates are Android dp; the small star radii use physical pixels in Android.
 */
export function drawBrandIcon(canvas: HTMLCanvasElement, enabled: boolean, size = 210, dpr = 2) {
    canvas.width = Math.round(size * dpr);
    canvas.height = Math.round(size * dpr);
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.scale(dpr, dpr);
    const x = size / 2;
    const y = size * 0.38;
    const circle = (cx: number, cy: number, radius: number, fill: string | CanvasGradient) => {
        ctx.fillStyle = fill;
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fill();
    };
    const radial = (cx: number, cy: number, radius: number, colors: string[]) => {
        const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
        colors.forEach((color, i) => gradient.addColorStop(i / (colors.length - 1), color));
        return gradient;
    };
    const radius = size * (enabled ? 0.11 : 0.17);
    if (enabled) {
        circle(
            x,
            y,
            radius * 2.2,
            radial(x, y, radius * 2.2, ["#fff8e780", "#ffb74d40", "#ffb74d00"]),
        );
        for (let i = 0; i < 8; i++) {
            const angle = (i / 8) * Math.PI * 2;
            const sx = x + Math.cos(angle) * radius * 1.15;
            const sy = y + Math.sin(angle) * radius * 1.15;
            const ex = x + Math.cos(angle) * radius * 1.75;
            const ey = y + Math.sin(angle) * radius * 1.75;
            const gradient = ctx.createLinearGradient(sx, sy, ex, ey);
            gradient.addColorStop(0, "#ffcc80d9");
            gradient.addColorStop(1, "#ffcc8000");
            ctx.strokeStyle = gradient;
            ctx.lineWidth = radius * 0.14;
            ctx.lineCap = "round";
            ctx.beginPath();
            ctx.moveTo(sx, sy);
            ctx.lineTo(ex, ey);
            ctx.stroke();
        }
        circle(x, y, radius, radial(x, y, radius, ["#fffde7", "#ffb300", "#ff8f00"]));
    } else {
        for (let layer = 4; layer >= 1; layer--) {
            const alpha = 0.07 * layer;
            const r = radius * (1.25 + layer * 0.12);
            circle(
                x,
                y,
                r,
                radial(x, y, r, [
                    `rgba(0,229,255,${alpha})`,
                    `rgba(124,77,255,${alpha * 0.6})`,
                    "#7c4dff00",
                ]),
            );
        }
        circle(
            x,
            y,
            radius,
            radial(x - radius * 0.12, y - radius * 0.08, radius * 1.1, [
                "#e8f4ff",
                "#80d4ff",
                "#7c4dff",
                "#4a2080",
            ]),
        );
        circle(x + radius * 0.42, y - radius * 0.06, radius * 0.88, "#14102a");
        ctx.globalCompositeOperation = "screen";
        circle(
            x - radius * 0.2,
            y,
            radius * 0.5,
            radial(x - radius * 0.35, y, radius * 0.55, ["#00e5ffe6", "#00e5ff00"]),
        );
        ctx.globalCompositeOperation = "source-over";
    }
    const stars = [
        [-1.4, 0.5, 2.5],
        [1.1, 0.9, 1.8],
        [0.3, -1.2, 2],
        [-0.9, -0.7, 1.5],
    ] as const;
    stars.forEach(([dx, dy, radiusPx]) => {
        // Reference phone density: 3 physical pixels per Android dp.
        circle(x + dx * radius, y + dy * radius, radiusPx / 3, enabled ? "#ffe082bf" : "#b8e4ffbf");
    });
}
