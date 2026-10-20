import { describe, expect, it } from "vitest";
import { playCampaignUrl } from "./attribution";

describe("Play campaign attribution", () => {
    it("works without JavaScript-provided campaign parameters", () => {
        const url = new URL(playCampaignUrl());
        expect(url.searchParams.get("utm_source")).toBe("website");
        expect(url.searchParams.get("utm_campaign")).toBe("launch_v1");
    });
    it.each(["screen_off_v1", "one_tap_v1", "stay_on_task_v1"])(
        "preserves the %s video campaign through the website",
        (campaign) => {
            const url = new URL(playCampaignUrl(`?utm_source=youtube&utm_campaign=${campaign}`));
            expect(url.searchParams.get("utm_source")).toBe("youtube");
            expect(url.searchParams.get("utm_campaign")).toBe(campaign);
        },
    );
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
