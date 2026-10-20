import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { fastOutSlowIn, ANDROID_TIMING } from "./animation";

describe("Android demo parity", () => {
    it("has no drift from either Android shader", () => {
        const generator = fileURLToPath(
            new URL("../../../../../scripts/sync-android-demo.mjs", import.meta.url),
        );
        expect(() => execFileSync(process.execPath, [generator, "--check"])).not.toThrow();
    });
    it("uses Compose timing and its asymmetric easing", () => {
        expect(ANDROID_TIMING.background).toBe(1100);
        expect(ANDROID_TIMING.glow).toBe(400);
        expect(fastOutSlowIn(-1)).toBe(0);
        expect(fastOutSlowIn(2)).toBe(1);
        expect(fastOutSlowIn(0.5)).toBeCloseTo(0.77556, 4);
    });
});
