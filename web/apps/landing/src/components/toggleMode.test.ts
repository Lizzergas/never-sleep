import { describe, expect, it } from "vitest";
import { modeLabel, nextMode, type ToggleMode } from "./toggleMode";

describe("nextMode", () => {
    it("flips never → normal and back", () => {
        expect(nextMode("never")).toBe("normal");
        expect(nextMode("normal")).toBe("never");
    });

    it("is its own inverse (two taps return to start)", () => {
        const modes: ToggleMode[] = ["never", "normal"];
        for (const mode of modes) {
            expect(nextMode(nextMode(mode))).toBe(mode);
        }
    });
});

describe("modeLabel", () => {
    it("matches the in-app NEVER / NORMAL labels", () => {
        expect(modeLabel("never")).toBe("NEVER");
        expect(modeLabel("normal")).toBe("NORMAL");
    });
});
