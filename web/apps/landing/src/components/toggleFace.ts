import { FACE_SHADER } from "./android/shaders.generated";
import { createShader } from "./android/shader";

export interface ToggleFaceFrame {
    time: number;
    glow: number;
    neverMode: number;
}
export interface ToggleFace {
    render(frame: ToggleFaceFrame): void;
    resize(width: number, height: number, dpr: number): void;
    dispose(): void;
}
export const TOGGLE_FACE_SHADER_SOURCE = { fragment: FACE_SHADER } as const;

/** Compatibility wrapper using the same generated source as the complete app demo. */
export function createToggleFace(canvas: HTMLCanvasElement): ToggleFace | null {
    const renderer = createShader(canvas, FACE_SHADER);
    if (!renderer) return null;
    return {
        resize: renderer.resize,
        dispose: renderer.dispose,
        render: ({ time, glow, neverMode }) =>
            renderer.draw({ iTime: time, iGlow: glow, iNeverMode: neverMode }),
    };
}
