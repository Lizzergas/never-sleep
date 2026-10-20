import { describe, expect, it } from "vitest";
import { playCampaignUrl } from "./attribution";

describe("Play campaign attribution", () => {
    it("works without JavaScript-provided campaign parameters", () => {
        const url = new URL(playCampaignUrl());
        expect(url.searchParams.get("utm_source")).toBe("website");
        expect(url.searchParams.get("utm_campaign")).toBe("launch_v1");
    });
    it("preserves a known video campaign through the website", () => {
        const url = new URL(playCampaignUrl("?utm_source=youtube&utm_campaign=recipe_v1"));
        expect(url.searchParams.get("utm_source")).toBe("youtube");
        expect(url.searchParams.get("utm_campaign")).toBe("recipe_v1");
    });
    it("does not send unknown values, personal data, or destinations to Play", () => {
        const url = new URL(
            playCampaignUrl(
                "?utm_source=home@example.com&utm_campaign=secret&next=https://evil.example&email=private",
            ),
        );
        expect(url.hostname).toBe("play.google.com");
        expect(url.searchParams.get("utm_source")).toBe("website");
        expect(url.searchParams.get("utm_campaign")).toBe("launch_v1");
        expect([...url.searchParams.keys()]).toEqual(["id", "utm_source", "utm_campaign"]);
    });
});
