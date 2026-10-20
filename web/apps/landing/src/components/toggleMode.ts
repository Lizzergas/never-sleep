/** Which face the hero toggle is showing — mirrors the in-app NeverSleepToggleButton. */
export type ToggleMode = "on" | "off";

/** Pure state transition for the toggle. Kept separate from the DOM so it can be unit-tested. */
export function nextMode(mode: ToggleMode): ToggleMode {
    return mode === "on" ? "off" : "on";
}

/** Keep screen on is the fixed label; this is its current state. */
export function modeLabel(mode: ToggleMode): string {
    return mode === "on" ? "ON" : "OFF";
}
