const vertexSource = "attribute vec2 p; void main(){gl_Position=vec4(p,0.,1.);}";

export function createShader(canvas: HTMLCanvasElement, source: string) {
    const gl = canvas.getContext("webgl", {
        preserveDrawingBuffer: true,
        premultipliedAlpha: false,
        antialias: true,
    });
    if (!gl) return null;
    const shaders: WebGLShader[] = [];
    const program = gl.createProgram();
    const buffer = gl.createBuffer();
    const dispose = () => {
        gl.deleteBuffer(buffer);
        gl.deleteProgram(program);
        shaders.forEach((shader) => gl.deleteShader(shader));
    };
    if (!program || !buffer) {
        dispose();
        return null;
    }
    for (const [type, text] of [
        [gl.VERTEX_SHADER, vertexSource],
        [gl.FRAGMENT_SHADER, source],
    ] as const) {
        const shader = gl.createShader(type);
        if (!shader) {
            dispose();
            return null;
        }
        shaders.push(shader);
        gl.shaderSource(shader, text);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
            dispose();
            return null;
        }
        gl.attachShader(program, shader);
    }
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        dispose();
        return null;
    }
    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const position = gl.getAttribLocation(program, "p");
    gl.enableVertexAttribArray(position);
    gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
    const uniforms = new Map<string, WebGLUniformLocation | null>();
    const location = (name: string) => {
        if (!uniforms.has(name)) uniforms.set(name, gl.getUniformLocation(program, name));
        return uniforms.get(name)!;
    };
    return {
        resize(width: number, height: number, dpr: number) {
            canvas.width = Math.round(width * dpr);
            canvas.height = Math.round(height * dpr);
            gl.viewport(0, 0, canvas.width, canvas.height);
        },
        draw(values: Record<string, number>) {
            gl.useProgram(program);
            gl.uniform2f(location("iResolution"), canvas.width, canvas.height);
            for (const [name, value] of Object.entries(values)) gl.uniform1f(location(name), value);
            gl.drawArrays(gl.TRIANGLES, 0, 3);
            gl.flush();
        },
        dispose,
    };
}
