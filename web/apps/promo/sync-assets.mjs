import { readFileSync, writeFileSync, mkdirSync, copyFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "../../..");
const kotlin = readFileSync(
    path.join(root, "app/androidApp/src/main/kotlin/com/lizz/neversleep/NeverSleepBackground.kt"),
    "utf8",
);
const match = kotlin.match(/(?:private )?const val MORPH_SHADER = """([\s\S]*?)"""/);
if (!match) throw new Error("Cannot find Android background shader");
const source =
    "precision highp float;\n" +
    match[1]
        .replace(/\b(?:half|float)([234])\b/g, "vec$1")
        .replace(
            "vec4 main(vec2 fragCoord) {",
            "void main() {\n vec2 fragCoord = vec2(gl_FragCoord.x, iResolution.y - gl_FragCoord.y);",
        )
        .replace("return vec4(col, 1.0);", "gl_FragColor = vec4(col, 1.0);");
mkdirSync(path.join(here, "src/generated"), { recursive: true });
writeFileSync(
    path.join(here, "src/generated/background.ts"),
    "// Generated from Android MORPH_SHADER by sync-assets.mjs\nexport const BACKGROUND_SHADER = " +
        JSON.stringify(source) +
        ";\n",
);
copyFileSync(path.join(root, "assets/play-store/icon-512.png"), path.join(here, "public/icon.png"));
copyFileSync(
    path.join(root, "web/apps/landing/public/badges/en_badge_web_generic.png"),
    path.join(here, "public/play-badge.png"),
);
