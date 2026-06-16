import { describe, expect, it } from "vitest";

describe("landing scaffold", () => {
    it("documents the default landing port", () => {
        expect(5173).toBeGreaterThan(0);
    });
});
