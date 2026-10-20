import{r as v}from"./index.saqyeS7l.js";var T={exports:{}},M={};var j;function O(){if(j)return M;j=1;var s=Symbol.for("react.transitional.element"),f=Symbol.for("react.fragment");function t(h,e,r){var i=null;if(r!==void 0&&(i=""+r),e.key!==void 0&&(i=""+e.key),"key"in e){r={};for(var u in e)u!=="key"&&(r[u]=e[u])}else r=e;return e=r.ref,{$$typeof:s,type:h,key:i,ref:e!==void 0?e:null,props:r}}return M.Fragment=f,M.jsx=t,M.jsxs=t,M}var N;function B(){return N||(N=1,T.exports=O()),T.exports}var d=B();const P=`precision highp float;

uniform vec2 iResolution;
uniform float iTime;
uniform float iMode;

float hash21(vec2 p) {
    vec2 h = fract(p * vec2(123.34, 456.21));
    h += dot(h, h + 45.32);
    return fract(h.x * h.y);
}

// ~37° rotation applied per fbm octave so value-noise lattices never line up
// into axis-aligned bands ("scanlines").
vec2 rot37(vec2 p) {
    return vec2(p.x * 0.80 - p.y * 0.60, p.x * 0.60 + p.y * 0.80);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    // Quintic smoothstep — smoother than cubic, hides the grid.
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    vec2 pp = p;
    for (int i = 0; i < 4; i++) {
        v += amp * vnoise(pp);
        pp = rot37(pp) * 2.0 + 7.3;
        amp = amp * 0.5;
    }
    return v;
}

// Billowy fog via domain warping (fbm of fbm) — soft, cloudy, no banding.
float fog(vec2 uv, vec2 drift) {
    vec2 p = uv * 2.6 + drift;
    vec2 warp = vec2(fbm(p), fbm(p + 5.2));
    return fbm(p + warp * 0.7);
}

float starLayer(vec2 uv, float density, float time) {
    vec2 g = uv * density;
    vec2 id = floor(g);
    vec2 f = fract(g);
    float present = step(0.93, hash21(id));
    vec2 pos = vec2(hash21(id + 3.1), hash21(id + 7.7));
    float d = length(f - pos);
    float core = smoothstep(0.055, 0.0, d) + 0.35 * smoothstep(0.22, 0.0, d);
    float tw = 0.6 + 0.4 * sin(time * 0.02 + hash21(id) * 90.0);
    return present * core * tw;
}

void main() {
    vec2 fragCoord = vec2(gl_FragCoord.x, iResolution.y - gl_FragCoord.y);
    vec2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;

    // Radial glow anchored on the toggle (screen centre, slightly high).
    vec2 q = uv - vec2(0.5, 0.46);
    q.x = q.x * aspect;
    float dist = length(q);
    float bloom = smoothstep(0.70, 0.0, dist);
    bloom = bloom * bloom;
    bloom = bloom * (0.93 + 0.07 * sin(iTime * 0.11));

    // Wind-warped sweep between the two states (the "swoosh").
    float raw = 0.928 * uv.x - 0.371 * uv.y;
    float wc = (raw + 0.371) / 1.30;
    float turb = fbm(uv * 3.0 + vec2(iTime * 0.08, iTime * 0.05));
    float front = wc + (turb - 0.5) * 0.35;
    float edge = iMode * 1.6 - 0.3;
    float sweep = smoothstep(front - 0.2, front + 0.2, edge);

    // --- Normal: warm, cloudy dawn ---
    vec3 normalCol = vec3(0.05, 0.035, 0.045);
    normalCol = normalCol + vec3(1.0, 0.62, 0.28) * bloom * 0.9;
    if (sweep < 0.995) {
        float warmFog = fog(uv, vec2(iTime * 0.011, -iTime * 0.006));
        normalCol = normalCol + vec3(0.60, 0.32, 0.15) * warmFog * 0.75;
        normalCol = normalCol + vec3(0.40, 0.13, 0.16) * warmFog * warmFog * 0.5;
        float ang = atan(q.y, q.x);
        float rays = 0.5 + 0.5 * sin(ang * 6.0 + iTime * 0.045);
        rays = rays * smoothstep(0.66, 0.05, dist);
        normalCol = normalCol + vec3(1.0, 0.72, 0.38) * rays * 0.06;
    }

    // --- Never: dark, foggy night + stars ---
    vec3 neverCol = vec3(0.015, 0.02, 0.05);
    neverCol = neverCol + vec3(0.16, 0.09, 0.30) * bloom * 0.35;
    neverCol = neverCol + vec3(0.18, 0.32, 0.55) * bloom * 0.20;
    if (sweep > 0.005) {
        float coolFog = fog(uv + 11.0, vec2(-iTime * 0.008, iTime * 0.011));
        neverCol = neverCol + vec3(0.11, 0.10, 0.30) * coolFog * 0.8;
        neverCol = neverCol + vec3(0.05, 0.15, 0.32) * coolFog * coolFog * 0.45;
        float stars = starLayer(uv * vec2(aspect, 1.0), 16.0, iTime)
            + 0.7 * starLayer(uv * vec2(aspect, 1.0), 27.0, iTime + 50.0);
        neverCol = neverCol + vec3(0.80, 0.85, 1.0) * stars * (1.0 - bloom * 0.5);
    }

    vec3 col = mix(normalCol, neverCol, sweep);

    // Light-streak riding the moving front — peaks mid-transition, colour follows destination.
    float band = smoothstep(0.30, 0.5, sweep) * smoothstep(0.70, 0.5, sweep);
    float flash = 4.0 * iMode * (1.0 - iMode);
    vec3 streak = mix(vec3(1.0, 0.7, 0.35), vec3(0.45, 0.65, 1.0), iMode);
    col = col + streak * band * flash * 0.5;

    // Keep the top (status bar) and bottom (optional ad) darker for legibility.
    float vY = smoothstep(0.0, 0.16, uv.y) * smoothstep(1.0, 0.84, uv.y);
    col = col * mix(0.55, 1.0, vY);

    // Per-pixel dither to break 8-bit gradient banding on the smooth glows.
    float dither = (hash21(fragCoord) - 0.5) / 255.0;
    col = col + vec3(dither);

    gl_FragColor = vec4(col, 1.0);
}
`,D=`precision highp float;

uniform vec2 iResolution;
uniform float iTime;
uniform float iGlow;
uniform float iNeverMode;

void main() {
    vec2 fragCoord = vec2(gl_FragCoord.x, iResolution.y - gl_FragCoord.y);
    vec2 uv = fragCoord / iResolution;
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

    gl_FragColor = vec4(base, 1.0);
}
`,$="attribute vec2 p; void main(){gl_Position=vec4(p,0.,1.);}";function S(s,f){const t=s.getContext("webgl",{preserveDrawingBuffer:!0,premultipliedAlpha:!1,antialias:!0});if(!t)return null;const h=[],e=t.createProgram(),r=t.createBuffer(),i=()=>{t.deleteBuffer(r),t.deleteProgram(e),h.forEach(l=>t.deleteShader(l))};if(!e||!r)return i(),null;for(const[l,a]of[[t.VERTEX_SHADER,$],[t.FRAGMENT_SHADER,f]]){const n=t.createShader(l);if(!n||(h.push(n),t.shaderSource(n,a),t.compileShader(n),!t.getShaderParameter(n,t.COMPILE_STATUS)))return i(),null;t.attachShader(e,n)}if(t.linkProgram(e),!t.getProgramParameter(e,t.LINK_STATUS))return i(),null;t.useProgram(e),t.bindBuffer(t.ARRAY_BUFFER,r),t.bufferData(t.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),t.STATIC_DRAW);const u=t.getAttribLocation(e,"p");t.enableVertexAttribArray(u),t.vertexAttribPointer(u,2,t.FLOAT,!1,0,0);const p=new Map,o=l=>(p.has(l)||p.set(l,t.getUniformLocation(e,l)),p.get(l));return{resize(l,a,n){s.width=Math.round(l*n),s.height=Math.round(a*n),t.viewport(0,0,s.width,s.height)},draw(l){t.useProgram(e),t.uniform2f(o("iResolution"),s.width,s.height);for(const[a,n]of Object.entries(l))t.uniform1f(o(a),n);t.drawArrays(t.TRIANGLES,0,3),t.flush()},dispose:i}}function H(s,f,t=210,h=2){s.width=Math.round(t*h),s.height=Math.round(t*h);const e=s.getContext("2d");if(!e)return;e.scale(h,h);const r=t/2,i=t*.38,u=(a,n,c,g)=>{e.fillStyle=g,e.beginPath(),e.arc(a,n,c,0,Math.PI*2),e.fill()},p=(a,n,c,g)=>{const x=e.createRadialGradient(a,n,0,a,n,c);return g.forEach((b,m)=>x.addColorStop(m/(g.length-1),b)),x},o=t*(f?.11:.17);if(f){u(r,i,o*2.2,p(r,i,o*2.2,["#fff8e780","#ffb74d40","#ffb74d00"]));for(let a=0;a<8;a++){const n=a/8*Math.PI*2,c=r+Math.cos(n)*o*1.15,g=i+Math.sin(n)*o*1.15,x=r+Math.cos(n)*o*1.75,b=i+Math.sin(n)*o*1.75,m=e.createLinearGradient(c,g,x,b);m.addColorStop(0,"#ffcc80d9"),m.addColorStop(1,"#ffcc8000"),e.strokeStyle=m,e.lineWidth=o*.14,e.lineCap="round",e.beginPath(),e.moveTo(c,g),e.lineTo(x,b),e.stroke()}u(r,i,o,p(r,i,o,["#fffde7","#ffb300","#ff8f00"]))}else{for(let a=4;a>=1;a--){const n=.07*a,c=o*(1.25+a*.12);u(r,i,c,p(r,i,c,[`rgba(0,229,255,${n})`,`rgba(124,77,255,${n*.6})`,"#7c4dff00"]))}u(r,i,o,p(r-o*.12,i-o*.08,o*1.1,["#e8f4ff","#80d4ff","#7c4dff","#4a2080"])),u(r+o*.42,i-o*.06,o*.88,"#14102a"),e.globalCompositeOperation="screen",u(r-o*.2,i,o*.5,p(r-o*.35,i,o*.55,["#00e5ffe6","#00e5ff00"])),e.globalCompositeOperation="source-over"}[[-1.4,.5,2.5],[1.1,.9,1.8],[.3,-1.2,2],[-.9,-.7,1.5]].forEach(([a,n,c])=>{u(r+a*o,i+n*o,c/3,f?"#ffe082bf":"#b8e4ffbf")})}function _(s){const f=Math.max(0,Math.min(1,s));if(f===0||f===1)return f;let t=0,h=1,e=f;for(let r=0;r<18;r++)3*(1-e)*(1-e)*e*.4+3*(1-e)*e*e*.2+e**3<f?t=e:h=e,e=(t+h)/2;return 3*(1-e)*e*e+e**3}const y={background:1100,glow:400,backgroundLoop:4e4,faceLoop:12e3};function U({enabled:s,night:f,glow:t,timeMs:h,onToggle:e,fontUrl:r="/fonts/Roboto.ttf"}){const i=v.useRef(null),u=v.useRef(null),p=v.useRef(null),o=v.useRef(null),l=v.useRef(null);return v.useLayoutEffect(()=>{const a=S(i.current,P),n=S(u.current,D);return a?.resize(360,780,3),n?.resize(210,210,3),l.current={background:a,face:n},()=>{a?.dispose(),n?.dispose()}},[]),v.useLayoutEffect(()=>{l.current?.background?.draw({iMode:f,iTime:h%y.backgroundLoop/40}),l.current?.face?.draw({iNeverMode:s?0:1,iGlow:t,iTime:h%y.faceLoop/12})},[s,f,t,h]),v.useLayoutEffect(()=>{H(p.current,s,210,3)},[s]),v.useLayoutEffect(()=>{const a=o.current,n=210*1.14;a.width=Math.round(n*3),a.height=Math.round(n*3);const c=a.getContext("2d");if(!c)return;c.scale(3,3);const g=n/2,x=n*.52,b=s?"255,183,77":"124,77,255",m=c.createRadialGradient(g,g,0,g,g,x);m.addColorStop(0,`rgba(${b},${.35*t})`),m.addColorStop(.5,`rgba(${b},${.08*t})`),m.addColorStop(1,`rgba(${b},0)`),c.fillStyle=m,c.fillRect(0,0,n,n)},[s,t]),d.jsxs("div",{className:"android-screen","data-mode":s?"on":"off",children:[d.jsx("style",{children:`@font-face { font-family: "Android Roboto"; src: url("${r}") format("truetype"); font-style: normal; font-weight: 100 900; font-display: swap; }`}),d.jsx("canvas",{ref:i,className:"android-background","aria-hidden":"true"}),d.jsxs("div",{className:"android-status","aria-hidden":"true",children:[d.jsx("span",{children:"9:41"}),d.jsxs("svg",{width:"44",height:"14",viewBox:"0 0 44 14",fill:"currentColor",children:[d.jsx("path",{d:"M0 3Q8-3 16 3L8 11Z"}),d.jsx("path",{d:"M20 12L30 2V12Z"}),d.jsx("path",{d:"M36 2H38V0H41V2H43V13H36Z"})]})]}),d.jsxs("div",{className:"android-content",children:[d.jsx("div",{className:"android-title",children:"Keep screen on"}),d.jsxs("button",{className:"android-toggle",role:"switch",type:"button","aria-label":"Keep screen on","aria-checked":s,onClick:e,tabIndex:e?0:-1,children:[d.jsx("canvas",{ref:o,className:"android-bloom","aria-hidden":"true"}),d.jsx("canvas",{ref:u,className:"android-face","aria-hidden":"true"}),d.jsx("canvas",{ref:p,className:"android-icon","aria-hidden":"true"}),d.jsx("span",{className:"android-label",children:s?"ON":"OFF"})]})]}),d.jsx("div",{className:"android-navigation","aria-hidden":"true"})]})}function Y({defaultMode:s="on"}){const f=s==="on",[t,h]=v.useState(f),[e,r]=v.useState({night:f?0:1,glow:f?1:.45,timeMs:0}),i=v.useRef(null),u=v.useRef(null),p=v.useRef(e),o=v.useRef(f),l=v.useRef({started:0,night:e.night,glow:e.glow}),a=v.useRef(()=>{}),n=v.useCallback(()=>{o.current=!o.current,l.current={started:performance.now(),night:p.current.night,glow:p.current.glow},h(o.current),a.current()},[]);return v.useEffect(()=>{const c=window.matchMedia("(prefers-reduced-motion: reduce)"),g=S(u.current,P),x=()=>{g?.resize(window.innerWidth,window.innerHeight,Math.min(1,960/window.innerWidth)),g?.draw({iMode:p.current.night,iTime:p.current.timeMs%y.backgroundLoop/40})};x(),window.addEventListener("resize",x);let b=!0,m=0,w=0,F=0;const E=R=>{if(m=0,!b||document.hidden){w=0;return}if(!c.matches&&w&&R-w<1e3/30){m=requestAnimationFrame(E);return}const I=w?R-w:0;w=R,c.matches||(F+=I);const L=R-l.current.started,q=o.current?0:1,G=o.current?1:.45,A={night:l.current.night+(q-l.current.night)*(c.matches?1:_(L/y.background)),glow:l.current.glow+(G-l.current.glow)*(c.matches?1:_(L/y.glow)),timeMs:F};g?.draw({iMode:A.night,iTime:A.timeMs%y.backgroundLoop/40}),p.current=A,r(A),c.matches||(m=requestAnimationFrame(E))},C=()=>{if(document.hidden){cancelAnimationFrame(m),m=0,w=0;return}m||(m=requestAnimationFrame(E))};a.current=C;const k=new IntersectionObserver(([R])=>{R&&(b=R.isIntersecting,b&&C())});return k.observe(i.current),document.addEventListener("visibilitychange",C),c.addEventListener("change",C),C(),()=>{cancelAnimationFrame(m),k.disconnect(),window.removeEventListener("resize",x),g?.dispose(),document.removeEventListener("visibilitychange",C),c.removeEventListener("change",C)}},[]),d.jsxs("div",{className:"live-demo",ref:i,children:[d.jsx("div",{className:"page-ambient","data-mode":t?"on":"off","aria-hidden":"true",children:d.jsx("canvas",{ref:u})}),d.jsx("div",{className:"device-frame",children:d.jsx("div",{className:"device-viewport",children:d.jsx(U,{enabled:t,...e,onToggle:n})})})]})}export{Y as AppToggle};
