import { describe, expect, it } from "vitest";
import { createToggleFace, TOGGLE_FACE_SHADER_SOURCE } from "./toggleFace";

describe("toggle face shader source", () => {
    it("exposes the same uniforms the app's AGSL face drives", () => {
        // Guards the AGSL → GLSL port: these are the four inputs set every frame.
        for (const uniform of ["iResolution", "iTime", "iGlow", "iNeverMode"]) {
            expect(TOGGLE_FACE_SHADER_SOURCE.fragment).toContain(uniform);
        }
    });

    it("keeps the never-mode nebula and rim pulse from the original", () => {
        expect(TOGGLE_FACE_SHADER_SOURCE.fragment).toContain("nebula");
        expect(TOGGLE_FACE_SHADER_SOURCE.fragment).toContain("pulse");
    });
});

describe("createToggleFace", () => {
    it("returns null (CSS fallback) when the canvas has no WebGL context", () => {
        // jsdom canvases have no GL context — this exercises the documented fallback path.
        const canvas = { getContext: () => null } as unknown as HTMLCanvasElement;
        expect(createToggleFace(canvas)).toBeNull();
    });
});
