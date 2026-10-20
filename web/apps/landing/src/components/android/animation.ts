/** Compose FastOutSlowInEasing = cubic-bezier(0.4, 0, 0.2, 1). */
export function fastOutSlowIn(progress: number): number {
    const x = Math.max(0, Math.min(1, progress));
    if (x === 0 || x === 1) return x;
    let low = 0;
    let high = 1;
    let t = x;
    for (let i = 0; i < 18; i++) {
        const sample = 3 * (1 - t) * (1 - t) * t * 0.4 + 3 * (1 - t) * t * t * 0.2 + t ** 3;
        if (sample < x) low = t;
        else high = t;
        t = (low + high) / 2;
    }
    return 3 * (1 - t) * t * t + t ** 3;
}

export const ANDROID_TIMING = {
    background: 1100,
    glow: 400,
    backgroundLoop: 40000,
    faceLoop: 12000,
} as const;
