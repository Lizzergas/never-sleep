import { describe, expect, it } from "vitest";
import { getApiStatusLabel } from "./App";

describe("getApiStatusLabel", () => {
    it("describes the idle API state", () => {
        expect(getApiStatusLabel({ kind: "idle" })).toBe("API not checked yet");
    });

    it("uses the server message when the API succeeds", () => {
        expect(
            getApiStatusLabel({
                kind: "success",
                data: {
                    message: "Hello from Ktor",
                    serverTime: "2026-06-16T00:00:00Z",
                },
            }),
        ).toBe("Hello from Ktor");
    });
});
