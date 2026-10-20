import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const android = path.join(root, "app/androidApp/src/main/kotlin/com/lizz/neversleep");
const destination = path.join(root, "web/apps/landing/src/components/android");

export function androidShader(file, constant) {
    const kotlin = readFileSync(path.join(android, file), "utf8");
    const source = kotlin.match(new RegExp(`const val ${constant} = """([\\s\\S]*?)"""`))?.[1];
    if (!source) throw new Error(`Missing Android shader: ${constant}`);
    return (
        "precision highp float;\n" +
        source
            .replace(/\b(?:half|float)([234])\b/g, "vec$1")
            .replace(
                "vec4 main(vec2 fragCoord) {",
                "void main() {\n    vec2 fragCoord = vec2(gl_FragCoord.x, iResolution.y - gl_FragCoord.y);",
            )
            .replace(/return vec4\(([^;]+)\);/g, "gl_FragColor = vec4($1);")
    );
}

const shaders = {
    BACKGROUND_SHADER: androidShader("NeverSleepBackground.kt", "MORPH_SHADER"),
    FACE_SHADER: androidShader("NeverSleepToggleButton.kt", "TOGGLE_FACE_SHADER"),
};
const output =
    "// Generated from Android. Run: node web/scripts/sync-android-demo.mjs\n" +
    Object.entries(shaders)
        .map(([name, source]) => `export const ${name} = ${JSON.stringify(source)};\n`)
        .join("");
mkdirSync(destination, { recursive: true });
const target = path.join(destination, "shaders.generated.ts");
if (process.argv.includes("--check")) {
    if (readFileSync(target, "utf8") !== output) {
        throw new Error("Android shaders changed. Run node web/scripts/sync-android-demo.mjs");
    }
} else {
    writeFileSync(target, output);
}
