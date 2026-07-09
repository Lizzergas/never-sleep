/** Which face the hero toggle is showing — mirrors the in-app NeverSleepToggleButton. */
export type ToggleMode = "never" | "normal";

/** Pure state transition for the toggle. Kept separate from the DOM so it can be unit-tested. */
export function nextMode(mode: ToggleMode): ToggleMode {
    return mode === "never" ? "normal" : "never";
}

/** Uppercase label rendered inside the circle for a given mode ("NEVER" / "NORMAL"). */
export function modeLabel(mode: ToggleMode): string {
    return mode === "never" ? "NEVER" : "NORMAL";
}
