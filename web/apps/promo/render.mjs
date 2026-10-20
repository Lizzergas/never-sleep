import { bundle } from "@remotion/bundler";
import { ensureBrowser, getCompositions, renderMedia, renderStill } from "@remotion/renderer";
import { mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import "./sync-assets.mjs";
const here = path.dirname(fileURLToPath(import.meta.url));
const output = path.resolve(here, "../../../dist/marketing");
mkdirSync(output, { recursive: true });
await ensureBrowser();
const serveUrl = await bundle({
    entryPoint: path.join(here, "src/index.ts"),
    publicDir: path.join(here, "public"),
});
const compositions = await getCompositions(serveUrl);
const preview = process.argv.includes("--stills");
const requested = process.argv.find((arg) => arg.startsWith("--only="))?.split("=")[1];
for (const composition of compositions) {
    if (requested && composition.id !== requested) continue;
    const common = { serveUrl, composition, chromiumOptions: { gl: "angle" } };
    const frames = [15, 65, 90, 150, 230, 270, composition.durationInFrames - 60];
    for (const frame of new Set(frames)) {
        await renderStill({
            ...common,
            frame,
            output: path.join(output, `${composition.id}-${frame}.png`),
        });
    }
    if (preview) continue;
    let previous = -1;
    await renderMedia({
        ...common,
        codec: "h264",
        pixelFormat: "yuv420p",
        crf: 18,
        concurrency: 2,
        outputLocation: path.join(output, `${composition.id}.mp4`),
        onProgress: ({ progress }) => {
            const pct = Math.floor(progress * 10) * 10;
            if (pct !== previous) {
                console.log(`${composition.id}: ${pct}%`);
                previous = pct;
            }
        },
    });
}
