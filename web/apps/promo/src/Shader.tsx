import { useLayoutEffect, useRef } from "react";
import { BACKGROUND_SHADER } from "./generated/background";
import { createToggleFace, type ToggleFace } from "../../landing/src/components/toggleFace";

export function Background({ night, time }: { night: number; time: number }) {
    const canvas = useRef<HTMLCanvasElement>(null);
    const renderer = useRef<{
        draw: (night: number, time: number) => void;
        dispose: () => void;
    } | null>(null);
    useLayoutEffect(() => {
        const gl = canvas.current?.getContext("webgl", { preserveDrawingBuffer: true });
        if (!gl) throw new Error("WebGL is required to render the Android shader");
        const compile = (type: number, source: string) => {
            const shader = gl.createShader(type)!;
            gl.shaderSource(shader, source);
            gl.compileShader(shader);
            if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS))
                throw new Error(gl.getShaderInfoLog(shader) ?? "Shader compilation failed");
            return shader;
        };
        const vertex = compile(
            gl.VERTEX_SHADER,
            "attribute vec2 p; void main(){gl_Position=vec4(p,0.,1.);}",
        );
        const fragment = compile(gl.FRAGMENT_SHADER, BACKGROUND_SHADER);
        const program = gl.createProgram()!;
        gl.attachShader(program, vertex);
        gl.attachShader(program, fragment);
        gl.linkProgram(program);
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error("Shader link failed");
        gl.useProgram(program);
        const buffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
        const pos = gl.getAttribLocation(program, "p");
        gl.enableVertexAttribArray(pos);
        gl.vertexAttribPointer(pos, 2, gl.FLOAT, false, 0, 0);
        const resolution = gl.getUniformLocation(program, "iResolution");
        const mode = gl.getUniformLocation(program, "iMode");
        const clock = gl.getUniformLocation(program, "iTime");
        gl.viewport(0, 0, 540, 960);
        renderer.current = {
            draw(n, t) {
                gl.uniform2f(resolution, 540, 960);
                gl.uniform1f(mode, n);
                gl.uniform1f(clock, t);
                gl.drawArrays(gl.TRIANGLES, 0, 3);
                gl.finish();
            },
            dispose() {
                gl.deleteBuffer(buffer);
                gl.deleteProgram(program);
                gl.deleteShader(vertex);
                gl.deleteShader(fragment);
            },
        };
        return () => renderer.current?.dispose();
    }, []);
    useLayoutEffect(() => {
        renderer.current?.draw(night, time);
    }, [night, time]);
    return (
        <canvas
            ref={canvas}
            width={540}
            height={960}
            style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}
        />
    );
}

export function Face({ night, time }: { night: number; time: number }) {
    const canvas = useRef<HTMLCanvasElement>(null);
    const face = useRef<ToggleFace | null>(null);
    useLayoutEffect(() => {
        face.current = createToggleFace(canvas.current!);
        face.current?.resize(380, 380, 1);
        return () => face.current?.dispose();
    }, []);
    useLayoutEffect(() => {
        face.current?.render({ time, neverMode: night, glow: 1 - night * 0.55 });
    }, [night, time]);
    return (
        <canvas
            ref={canvas}
            style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}
        />
    );
}
