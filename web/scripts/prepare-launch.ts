import { mkdir, readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";
import { SOURCES, playCampaignUrl } from "../apps/landing/src/lib/attribution";
import { VIDEO_VARIANTS } from "../apps/promo/src/variants";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const output = path.join(root, "dist/marketing");
await mkdir(output, { recursive: true });
const links = SOURCES.flatMap((source) =>
    ["launch_v1", ...VIDEO_VARIANTS.map((variant) => variant.campaign)].map((campaign) => {
        const query = new URLSearchParams({ utm_source: source, utm_campaign: campaign });
        return {
            source,
            campaign,
            website: `https://neversleep.app/?${query}`,
            play: playCampaignUrl(query.toString()),
        };
    }),
);
await writeFile(path.join(output, "campaign-links.json"), JSON.stringify(links, null, 2) + "\n");
for (const { id: name, opening, campaign } of VIDEO_VARIANTS) {
    const promise = "Keep your screen on with one tap.";
    const lead = opening === promise ? promise : `${opening}\n\n${promise}`;
    const caption = `${lead}\nNever sleep. Always awake.\n\nGet it on Google Play.`;
    const destinations = links.filter(
        (link) =>
            link.campaign === campaign && ["youtube", "instagram", "tiktok"].includes(link.source),
    );
    await writeFile(
        path.join(output, `${name}-post.txt`),
        caption +
            "\n\n" +
            destinations
                .map((link) => `${link.source}\n${link.website}\nDirect Play link: ${link.play}`)
                .join("\n\n") +
            "\n",
    );
}
await writeFile(
    path.join(output, "measurement-template.json"),
    await readFile(path.join(root, "marketing/measurement-template.json")),
);
console.log(
    `Prepared ${links.length} campaign links, three app demo captions, and a blank measurement template in ${output}`,
);

// Package only public launch materials, never reports or release credentials.
const { existsSync } = await import("node:fs");
const { createHash } = await import("node:crypto");
const { spawnSync } = await import("node:child_process");
const files = ["campaign-links.json", "measurement-template.json"];
await writeFile(
    path.join(output, "README.md"),
    await readFile(path.join(root, "marketing/README.md")),
);
files.push("README.md");
const listing = Object.fromEntries(
    await Promise.all(
        ["title", "short_description", "full_description"].map(async (name) => [
            name,
            (
                await readFile(
                    path.join(root, "fastlane/metadata/android/en-US", `${name}.txt`),
                    "utf8",
                )
            ).trim(),
        ]),
    ),
);
await writeFile(path.join(output, "play-listing.json"), JSON.stringify(listing, null, 2) + "\n");
files.push("play-listing.json");
for (const { id: name, durationInFrames } of VIDEO_VARIANTS)
    files.push(
        `${name}-post.txt`,
        `${name}.mp4`,
        `${name}-15.png`,
        `${name}-150.png`,
        `${name}-${durationInFrames - 60}.png`,
    );
if (files.every((file) => existsSync(path.join(output, file)))) {
    const hashes = Object.fromEntries(
        await Promise.all(
            files.map(async (file) => [
                file,
                createHash("sha256")
                    .update(await readFile(path.join(output, file)))
                    .digest("hex"),
            ]),
        ),
    );
    await writeFile(
        path.join(output, "manifest.json"),
        JSON.stringify({ sha256: hashes }, null, 2) + "\n",
    );
    const result = spawnSync(
        "python3",
        [
            "-c",
            'import sys,zipfile; z=zipfile.ZipFile(sys.argv[1],"w",zipfile.ZIP_DEFLATED); [z.write(p,p) for p in sys.argv[2:]]',
            "launch-kit.zip",
            ...files,
            "manifest.json",
        ],
        { cwd: output, stdio: "inherit" },
    );
    if (result.status !== 0) throw new Error("Launch ZIP packaging failed");
    console.log(`Packaged ${path.join(output, "launch-kit.zip")}`);
}
