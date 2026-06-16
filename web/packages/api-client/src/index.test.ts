import { describe, expect, it } from "vitest";
import { apiBaseUrl, hello } from "./index";

describe("api client scaffold", () => {
    it("uses same-origin API requests by default", () => {
        expect(apiBaseUrl).toBe("");
    });

    it("decodes the hello response", async () => {
        const response = new Response(
            JSON.stringify({
                message: "Hello",
                serverTime: "2026-06-16T00:00:00Z",
            }),
        );

        const data = await hello(async () => response);

        expect(data.message).toBe("Hello");
    });
});
