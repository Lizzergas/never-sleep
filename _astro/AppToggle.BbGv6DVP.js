import{j as t}from"./jsx-runtime.u17CrQMm.js";import{r as c}from"./index.saqyeS7l.js";const k=`
attribute vec2 aPos;
void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
}
`,L=`
precision highp float;
uniform vec2 iResolution;
uniform float iTime;
uniform float iGlow;
uniform float iNeverMode;

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution;
    vec2 p = uv - 0.5;
    float dist = length(p) * 2.0;

    vec3 deep = vec3(0.04, 0.04, 0.10);
    vec3 purple = vec3(0.14, 0.08, 0.26);
    vec3 warm = vec3(0.16, 0.12, 0.10);
    vec3 base = mix(warm, mix(deep, purple, smoothstep(0.0, 0.7, 1.0 - dist)), iNeverMode);

    float t = iTime * 0.001;
    float nebula = 0.06 * sin(p.x * 9.0 + t) * sin(p.y * 7.0 - t * 1.3);
    base += vec3(nebula * iNeverMode);

    float rim = smoothstep(0.78, 0.92, dist) * smoothstep(1.02, 0.86, dist);
    float pulse = 0.82 + 0.18 * sin(t * 4.5);
    vec3 cyan = vec3(0.2, 0.85, 0.95);
    vec3 violet = vec3(0.55, 0.35, 1.0);
    vec3 rimColor = mix(vec3(0.45, 0.55, 0.75), mix(cyan, violet, 0.5 + 0.5 * sin(t * 2.0)), iNeverMode);
    base += rimColor * rim * pulse * iGlow;

    float vignette = smoothstep(1.1, 0.35, dist);
    base *= vignette;

    float mask = smoothstep(1.0, 0.985, dist);
    gl_FragColor = vec4(base, mask);
}
`;function M(r,e,s){const n=r.createShader(e);return n?(r.shaderSource(n,s),r.compileShader(n),r.getShaderParameter(n,r.COMPILE_STATUS)?n:(r.deleteShader(n),null)):null}function P(r){const e=r.getContext("webgl",{premultipliedAlpha:!1,antialias:!0})??r.getContext("experimental-webgl",{premultipliedAlpha:!1});if(!e)return null;const s=M(e,e.VERTEX_SHADER,k),n=M(e,e.FRAGMENT_SHADER,L),o=e.createProgram();if(!s||!n||!o||(e.attachShader(o,s),e.attachShader(o,n),e.linkProgram(o),!e.getProgramParameter(o,e.LINK_STATUS)))return null;e.useProgram(o);const d=e.createBuffer();e.bindBuffer(e.ARRAY_BUFFER,d),e.bufferData(e.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),e.STATIC_DRAW);const l=e.getAttribLocation(o,"aPos");e.enableVertexAttribArray(l),e.vertexAttribPointer(l,2,e.FLOAT,!1,0,0);const m=e.getUniformLocation(o,"iResolution"),x=e.getUniformLocation(o,"iTime"),u=e.getUniformLocation(o,"iGlow"),p=e.getUniformLocation(o,"iNeverMode");return{render({time:i,glow:h,neverMode:f}){e.uniform2f(m,e.drawingBufferWidth,e.drawingBufferHeight),e.uniform1f(x,i),e.uniform1f(u,h),e.uniform1f(p,f),e.drawArrays(e.TRIANGLES,0,3)},resize(i,h,f){const v=Math.max(1,Math.round(i*f)),g=Math.max(1,Math.round(h*f));r.width=v,r.height=g,e.viewport(0,0,v,g)},dispose(){e.deleteBuffer(d),e.deleteProgram(o),e.deleteShader(s),e.deleteShader(n)}}}function F(r){return r==="never"?"normal":"never"}function G(r){return r==="never"?"NEVER":"NORMAL"}const E=1,N=.45,B=8,I=0;function U(){return typeof window<"u"&&window.matchMedia("(prefers-reduced-motion: reduce)").matches}function D({defaultMode:r="never"}){const[e,s]=c.useState(r),n=c.useRef(null),o=c.useRef(null),d=c.useRef(r),l=c.useRef(r==="never"?E:N),m=c.useRef(r==="never"?1:0);d.current=e;const x=c.useCallback(()=>s(u=>F(u)),[]);return c.useEffect(()=>{const u=n.current,p=o.current;if(!u||!p)return;const i=P(u);i&&(u.dataset.active="true");const h=Math.min(2,typeof window<"u"&&window.devicePixelRatio||1),f=()=>{const a=p.getBoundingClientRect();a.width>0&&i?.resize(a.width,a.height,h)};f();const v=new ResizeObserver(f);v.observe(p);const g=U();let R=0,w=0,_=0;const j=a=>{w===0&&(w=a,_=a);const T=Math.min(.05,(a-_)/1e3);_=a;const A=d.current==="never"?E:N,b=d.current==="never"?1:0,S=Math.min(1,T*B);l.current+=(A-l.current)*S,m.current+=(b-m.current)*S;const y=g?I:(a-w)/12;i?.render({time:y,glow:l.current,neverMode:m.current});const C=Math.abs(l.current-A)<.002&&Math.abs(m.current-b)<.002;(!g||!C)&&(R=requestAnimationFrame(j))};return R=requestAnimationFrame(j),()=>{cancelAnimationFrame(R),v.disconnect(),i?.dispose()}},[e]),t.jsxs("button",{type:"button",role:"switch","aria-checked":e==="never","aria-label":e==="never"?"Switch to Normal mode and restore the system timeout":"Switch to Never mode and keep the screen awake",className:"app-toggle","data-mode":e,onClick:x,children:[t.jsxs("span",{ref:o,className:"app-toggle__face","aria-hidden":"true",children:[t.jsx("span",{className:"app-toggle__gradient"}),t.jsx("canvas",{ref:n,className:"app-toggle__canvas"}),t.jsx("span",{className:"app-toggle__icon app-toggle__icon--moon",children:t.jsx(O,{})}),t.jsx("span",{className:"app-toggle__icon app-toggle__icon--sun",children:t.jsx(V,{})})]}),t.jsx("span",{className:"app-toggle__label",children:G(e)})]})}function O(){return t.jsxs("svg",{viewBox:"0 0 100 100",width:"100%",height:"100%","aria-hidden":"true",children:[t.jsxs("defs",{children:[t.jsxs("radialGradient",{id:"ns-moon",cx:"38%",cy:"42%",r:"70%",children:[t.jsx("stop",{offset:"0%",stopColor:"var(--color-ink)"}),t.jsx("stop",{offset:"45%",stopColor:"var(--color-state-never-cool)"}),t.jsx("stop",{offset:"80%",stopColor:"var(--color-state-never)"}),t.jsx("stop",{offset:"100%",stopColor:"var(--color-toggle-never-core)"})]}),t.jsxs("mask",{id:"ns-crescent",children:[t.jsx("circle",{cx:"50",cy:"50",r:"26",fill:"white"}),t.jsx("circle",{cx:"62",cy:"44",r:"23",fill:"black"})]})]}),t.jsx("circle",{cx:"50",cy:"50",r:"26",fill:"url(#ns-moon)",mask:"url(#ns-crescent)"})]})}function V(){const r=Array.from({length:8},(e,s)=>s*45);return t.jsxs("svg",{viewBox:"0 0 100 100",width:"100%",height:"100%","aria-hidden":"true",children:[t.jsx("defs",{children:t.jsxs("radialGradient",{id:"ns-sun",cx:"50%",cy:"50%",r:"50%",children:[t.jsx("stop",{offset:"0%",stopColor:"var(--color-ink)"}),t.jsx("stop",{offset:"60%",stopColor:"var(--color-state-normal-soft)"}),t.jsx("stop",{offset:"100%",stopColor:"var(--color-state-normal)"})]})}),t.jsx("g",{stroke:"var(--color-state-normal-soft)",strokeWidth:"2.4",strokeLinecap:"round",children:r.map(e=>t.jsx("line",{x1:"50",y1:"20",x2:"50",y2:"30",transform:`rotate(${e} 50 50)`},e))}),t.jsx("circle",{cx:"50",cy:"50",r:"14",fill:"url(#ns-sun)"})]})}export{D as AppToggle};
