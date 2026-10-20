import{j as t}from"./jsx-runtime.u17CrQMm.js";import{r as c}from"./index.saqyeS7l.js";const F=`
attribute vec2 aPos;
void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
}
`,k=`
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
`;function T(o,e,s){const n=o.createShader(e);return n?(o.shaderSource(n,s),o.compileShader(n),o.getShaderParameter(n,o.COMPILE_STATUS)?n:(o.deleteShader(n),null)):null}function L(o){const e=o.getContext("webgl",{premultipliedAlpha:!1,antialias:!0})??o.getContext("experimental-webgl",{premultipliedAlpha:!1});if(!e)return null;const s=T(e,e.VERTEX_SHADER,F),n=T(e,e.FRAGMENT_SHADER,k),r=e.createProgram();if(!s||!n||!r||(e.attachShader(r,s),e.attachShader(r,n),e.linkProgram(r),!e.getProgramParameter(r,e.LINK_STATUS)))return null;e.useProgram(r);const d=e.createBuffer();e.bindBuffer(e.ARRAY_BUFFER,d),e.bufferData(e.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),e.STATIC_DRAW);const l=e.getAttribLocation(r,"aPos");e.enableVertexAttribArray(l),e.vertexAttribPointer(l,2,e.FLOAT,!1,0,0);const m=e.getUniformLocation(r,"iResolution"),x=e.getUniformLocation(r,"iTime"),f=e.getUniformLocation(r,"iGlow"),p=e.getUniformLocation(r,"iNeverMode");return{render({time:i,glow:h,neverMode:u}){e.uniform2f(m,e.drawingBufferWidth,e.drawingBufferHeight),e.uniform1f(x,i),e.uniform1f(f,h),e.uniform1f(p,u),e.drawArrays(e.TRIANGLES,0,3)},resize(i,h,u){const g=Math.max(1,Math.round(i*u)),v=Math.max(1,Math.round(h*u));o.width=g,o.height=v,e.viewport(0,0,g,v)},dispose(){e.deleteBuffer(d),e.deleteProgram(r),e.deleteShader(s),e.deleteShader(n)}}}function P(o){return o==="on"?"off":"on"}function G(o){return o==="on"?"ON":"OFF"}const M=1,y=.45,B=8,O=0;function I(){return typeof window<"u"&&window.matchMedia("(prefers-reduced-motion: reduce)").matches}function H({defaultMode:o="on"}){const[e,s]=c.useState(o),n=c.useRef(null),r=c.useRef(null),d=c.useRef(o),l=c.useRef(o==="on"?M:y),m=c.useRef(o==="off"?1:0);d.current=e;const x=c.useCallback(()=>s(f=>P(f)),[]);return c.useEffect(()=>{const f=n.current,p=r.current;if(!f||!p)return;const i=L(f);i&&(f.dataset.active="true");const h=Math.min(2,typeof window<"u"&&window.devicePixelRatio||1),u=()=>{const a=p.getBoundingClientRect();a.width>0&&i?.resize(a.width,a.height,h)};u();const g=new ResizeObserver(u);g.observe(p);const v=I();let R=0,_=0,j=0;const w=a=>{_===0&&(_=a,j=a);const C=Math.min(.05,(a-j)/1e3);j=a;const b=d.current==="on"?M:y,A=d.current==="off"?1:0,S=Math.min(1,C*B);l.current+=(b-l.current)*S,m.current+=(A-m.current)*S;const E=v?O:(a-_)/12;i?.render({time:E,glow:l.current,neverMode:m.current});const N=Math.abs(l.current-b)<.002&&Math.abs(m.current-A)<.002;(!v||!N)&&(R=requestAnimationFrame(w))};return R=requestAnimationFrame(w),()=>{cancelAnimationFrame(R),g.disconnect(),i?.dispose()}},[e]),t.jsxs("button",{type:"button",role:"switch","aria-checked":e==="on","aria-label":"Keep screen on",className:"app-toggle","data-mode":e,onClick:x,children:[t.jsxs("span",{ref:r,className:"app-toggle__face","aria-hidden":"true",children:[t.jsx("span",{className:"app-toggle__gradient"}),t.jsx("canvas",{ref:n,className:"app-toggle__canvas"}),t.jsx("span",{className:"app-toggle__icon app-toggle__icon--moon",children:t.jsx(U,{})}),t.jsx("span",{className:"app-toggle__icon app-toggle__icon--sun",children:t.jsx(W,{})})]}),t.jsx("span",{className:"app-toggle__label",children:G(e)})]})}function U(){return t.jsxs("svg",{viewBox:"0 0 100 100",width:"100%",height:"100%","aria-hidden":"true",children:[t.jsxs("defs",{children:[t.jsxs("radialGradient",{id:"ns-moon",cx:"38%",cy:"42%",r:"70%",children:[t.jsx("stop",{offset:"0%",stopColor:"var(--color-ink)"}),t.jsx("stop",{offset:"45%",stopColor:"var(--color-state-never-cool)"}),t.jsx("stop",{offset:"80%",stopColor:"var(--color-state-never)"}),t.jsx("stop",{offset:"100%",stopColor:"var(--color-toggle-never-core)"})]}),t.jsxs("mask",{id:"ns-crescent",children:[t.jsx("circle",{cx:"50",cy:"50",r:"26",fill:"white"}),t.jsx("circle",{cx:"62",cy:"44",r:"23",fill:"black"})]})]}),t.jsx("circle",{cx:"50",cy:"50",r:"26",fill:"url(#ns-moon)",mask:"url(#ns-crescent)"})]})}function W(){const o=Array.from({length:8},(e,s)=>s*45);return t.jsxs("svg",{viewBox:"0 0 100 100",width:"100%",height:"100%","aria-hidden":"true",children:[t.jsx("defs",{children:t.jsxs("radialGradient",{id:"ns-sun",cx:"50%",cy:"50%",r:"50%",children:[t.jsx("stop",{offset:"0%",stopColor:"var(--color-ink)"}),t.jsx("stop",{offset:"60%",stopColor:"var(--color-state-normal-soft)"}),t.jsx("stop",{offset:"100%",stopColor:"var(--color-state-normal)"})]})}),t.jsx("g",{stroke:"var(--color-state-normal-soft)",strokeWidth:"2.4",strokeLinecap:"round",children:o.map(e=>t.jsx("line",{x1:"50",y1:"20",x2:"50",y2:"30",transform:`rotate(${e} 50 50)`},e))}),t.jsx("circle",{cx:"50",cy:"50",r:"14",fill:"url(#ns-sun)"})]})}export{H as AppToggle};
