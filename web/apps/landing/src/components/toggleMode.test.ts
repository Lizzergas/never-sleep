import { describe, expect, it } from "vitest";
import { modeLabel, nextMode, type ToggleMode } from "./toggleMode";

describe("nextMode", () => {
    it("flips on → off and back", () => {
        expect(nextMode("on")).toBe("off");
        expect(nextMode("off")).toBe("on");
    });

    it("is its own inverse (two taps return to start)", () => {
        const modes: ToggleMode[] = ["on", "off"];
        for (const mode of modes) {
            expect(nextMode(nextMode(mode))).toBe(mode);
        }
    });
});

describe("modeLabel", () => {
    it("matches the in-app ON / OFF labels", () => {
        expect(modeLabel("on")).toBe("ON");
        expect(modeLabel("off")).toBe("OFF");
    });
});
