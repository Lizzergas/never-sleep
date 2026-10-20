import { mkdirSync, copyFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "../../..");
// Both the interactive website and Remotion use this generated Android source.
await import("../../scripts/sync-android-demo.mjs");
mkdirSync(path.join(here, "public/fonts"), { recursive: true });
copyFileSync(
    path.join(root, "web/apps/landing/public/fonts/Roboto.ttf"),
    path.join(here, "public/fonts/Roboto.ttf"),
);
copyFileSync(
    path.join(root, "web/apps/landing/public/fonts/OFL-Roboto.txt"),
    path.join(here, "public/fonts/OFL-Roboto.txt"),
);
copyFileSync(path.join(root, "assets/play-store/icon-512.png"), path.join(here, "public/icon.png"));
copyFileSync(
    path.join(root, "web/apps/landing/public/badges/en_badge_web_generic.png"),
    path.join(here, "public/play-badge.png"),
);
