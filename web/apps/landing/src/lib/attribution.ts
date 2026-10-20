/** Public campaign labels only. Never forward arbitrary visitor input to Google. */
export const SOURCES = ["website", "youtube", "instagram", "tiktok", "community"] as const;
export const CAMPAIGNS = ["launch_v1", "recipe_v1", "music_v1", "workshop_v1"] as const;
export const PLAY_URL = "https://play.google.com/store/apps/details?id=com.lizz.neversleep";

export function playCampaignUrl(search = ""): string {
    const input = new URLSearchParams(search);
    const source = input.get("utm_source") ?? "";
    const campaign = input.get("utm_campaign") ?? "";
    const approvedSource = SOURCES.find((value) => value === source) ?? "website";
    const approvedCampaign = CAMPAIGNS.find((value) => value === campaign) ?? "launch_v1";
    const url = new URL(PLAY_URL);
    url.searchParams.set("utm_source", approvedSource);
    url.searchParams.set("utm_campaign", approvedCampaign);
    return url.href;
}
