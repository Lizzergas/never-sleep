import{r as p}from"./index.saqyeS7l.js";var A={exports:{}},R={};var F;function P(){if(F)return R;F=1;var s=Symbol.for("react.transitional.element"),l=Symbol.for("react.fragment");function t(m,e,r){var a=null;if(r!==void 0&&(a=""+r),e.key!==void 0&&(a=""+e.key),"key"in e){r={};for(var f in e)f!=="key"&&(r[f]=e[f])}else r=e;return e=r.ref,{$$typeof:s,type:m,key:a,ref:e!==void 0?e:null,props:r}}return R.Fragment=l,R.jsx=t,R.jsxs=t,R}var S;function I(){return S||(S=1,A.exports=P()),A.exports}var c=I();const q=`precision highp float;

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
`,O=`precision highp float;

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
`,G="attribute vec2 p; void main(){gl_Position=vec4(p,0.,1.);}";function j(s,l){const t=s.getContext("webgl",{preserveDrawingBuffer:!0,premultipliedAlpha:!1,antialias:!0});if(!t)return null;const m=[],e=t.createProgram(),r=t.createBuffer(),a=()=>{t.deleteBuffer(r),t.deleteProgram(e),m.forEach(v=>t.deleteShader(v))};if(!e||!r)return a(),null;for(const[v,i]of[[t.VERTEX_SHADER,G],[t.FRAGMENT_SHADER,l]]){const n=t.createShader(v);if(!n||(m.push(n),t.shaderSource(n,i),t.compileShader(n),!t.getShaderParameter(n,t.COMPILE_STATUS)))return a(),null;t.attachShader(e,n)}if(t.linkProgram(e),!t.getProgramParameter(e,t.LINK_STATUS))return a(),null;t.useProgram(e),t.bindBuffer(t.ARRAY_BUFFER,r),t.bufferData(t.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),t.STATIC_DRAW);const f=t.getAttribLocation(e,"p");t.enableVertexAttribArray(f),t.vertexAttribPointer(f,2,t.FLOAT,!1,0,0);const h=new Map,o=v=>(h.has(v)||h.set(v,t.getUniformLocation(e,v)),h.get(v));return{resize(v,i,n){s.width=Math.round(v*n),s.height=Math.round(i*n),t.viewport(0,0,s.width,s.height)},draw(v){t.useProgram(e),t.uniform2f(o("iResolution"),s.width,s.height);for(const[i,n]of Object.entries(v))t.uniform1f(o(i),n);t.drawArrays(t.TRIANGLES,0,3),t.flush()},dispose:a}}function B(s,l,t=210,m=2){s.width=Math.round(t*m),s.height=Math.round(t*m);const e=s.getContext("2d");if(!e)return;e.scale(m,m);const r=t/2,a=t*.38,f=(i,n,d,u)=>{e.fillStyle=u,e.beginPath(),e.arc(i,n,d,0,Math.PI*2),e.fill()},h=(i,n,d,u)=>{const g=e.createRadialGradient(i,n,0,i,n,d);return u.forEach((b,x)=>g.addColorStop(x/(u.length-1),b)),g},o=t*(l?.11:.17);if(l){f(r,a,o*2.2,h(r,a,o*2.2,["#fff8e780","#ffb74d40","#ffb74d00"]));for(let i=0;i<8;i++){const n=i/8*Math.PI*2,d=r+Math.cos(n)*o*1.15,u=a+Math.sin(n)*o*1.15,g=r+Math.cos(n)*o*1.75,b=a+Math.sin(n)*o*1.75,x=e.createLinearGradient(d,u,g,b);x.addColorStop(0,"#ffcc80d9"),x.addColorStop(1,"#ffcc8000"),e.strokeStyle=x,e.lineWidth=o*.14,e.lineCap="round",e.beginPath(),e.moveTo(d,u),e.lineTo(g,b),e.stroke()}f(r,a,o,h(r,a,o,["#fffde7","#ffb300","#ff8f00"]))}else{for(let i=4;i>=1;i--){const n=.07*i,d=o*(1.25+i*.12);f(r,a,d,h(r,a,d,[`rgba(0,229,255,${n})`,`rgba(124,77,255,${n*.6})`,"#7c4dff00"]))}f(r,a,o,h(r-o*.12,a-o*.08,o*1.1,["#e8f4ff","#80d4ff","#7c4dff","#4a2080"])),f(r+o*.42,a-o*.06,o*.88,"#14102a"),e.globalCompositeOperation="screen",f(r-o*.2,a,o*.5,h(r-o*.35,a,o*.55,["#00e5ffe6","#00e5ff00"])),e.globalCompositeOperation="source-over"}[[-1.4,.5,2.5],[1.1,.9,1.8],[.3,-1.2,2],[-.9,-.7,1.5]].forEach(([i,n,d])=>{f(r+i*o,a+n*o,d/3,l?"#ffe082bf":"#b8e4ffbf")})}function k(s){const l=Math.max(0,Math.min(1,s));if(l===0||l===1)return l;let t=0,m=1,e=l;for(let r=0;r<18;r++)3*(1-e)*(1-e)*e*.4+3*(1-e)*e*e*.2+e**3<l?t=e:m=e,e=(t+m)/2;return 3*(1-e)*e*e+e**3}const y={background:1100,glow:400,backgroundLoop:4e4,faceLoop:12e3};function D({enabled:s,night:l,glow:t,timeMs:m,onToggle:e,fontUrl:r="/fonts/Roboto.ttf"}){const a=p.useRef(null),f=p.useRef(null),h=p.useRef(null),o=p.useRef(null),v=p.useRef(null);return p.useLayoutEffect(()=>{const i=j(a.current,q),n=j(f.current,O);return i?.resize(360,780,3),n?.resize(210,210,3),v.current={background:i,face:n},()=>{i?.dispose(),n?.dispose()}},[]),p.useLayoutEffect(()=>{v.current?.background?.draw({iMode:l,iTime:m%y.backgroundLoop/40}),v.current?.face?.draw({iNeverMode:s?0:1,iGlow:t,iTime:m%y.faceLoop/12})},[s,l,t,m]),p.useLayoutEffect(()=>{B(h.current,s,210,3)},[s]),p.useLayoutEffect(()=>{const i=o.current,n=210*1.14;i.width=Math.round(n*3),i.height=Math.round(n*3);const d=i.getContext("2d");if(!d)return;d.scale(3,3);const u=n/2,g=n*.52,b=s?"255,183,77":"124,77,255",x=d.createRadialGradient(u,u,0,u,u,g);x.addColorStop(0,`rgba(${b},${.35*t})`),x.addColorStop(.5,`rgba(${b},${.08*t})`),x.addColorStop(1,`rgba(${b},0)`),d.fillStyle=x,d.fillRect(0,0,n,n)},[s,t]),c.jsxs("div",{className:"android-screen","data-mode":s?"on":"off",children:[c.jsx("style",{children:`@font-face { font-family: "Android Roboto"; src: url("${r}") format("truetype"); font-style: normal; font-weight: 100 900; font-display: swap; }`}),c.jsx("canvas",{ref:a,className:"android-background","aria-hidden":"true"}),c.jsxs("div",{className:"android-status","aria-hidden":"true",children:[c.jsx("span",{children:"9:41"}),c.jsxs("svg",{width:"44",height:"14",viewBox:"0 0 44 14",fill:"currentColor",children:[c.jsx("path",{d:"M0 3Q8-3 16 3L8 11Z"}),c.jsx("path",{d:"M20 12L30 2V12Z"}),c.jsx("path",{d:"M36 2H38V0H41V2H43V13H36Z"})]})]}),c.jsxs("div",{className:"android-content",children:[c.jsx("div",{className:"android-title",children:"Keep screen on"}),c.jsxs("button",{className:"android-toggle",role:"switch",type:"button","aria-label":"Keep screen on","aria-checked":s,onClick:e,tabIndex:e?0:-1,children:[c.jsx("canvas",{ref:o,className:"android-bloom","aria-hidden":"true"}),c.jsx("canvas",{ref:f,className:"android-face","aria-hidden":"true"}),c.jsx("canvas",{ref:h,className:"android-icon","aria-hidden":"true"}),c.jsx("span",{className:"android-label",children:s?"ON":"OFF"})]})]}),c.jsx("div",{className:"android-navigation","aria-hidden":"true"})]})}function H({defaultMode:s="on"}){const l=s==="on",[t,m]=p.useState(l),[e,r]=p.useState({night:l?0:1,glow:l?1:.45,timeMs:0}),a=p.useRef(null),f=p.useRef(e),h=p.useRef(l),o=p.useRef({started:0,night:e.night,glow:e.glow}),v=p.useRef(()=>{}),i=p.useCallback(()=>{h.current=!h.current,o.current={started:performance.now(),night:f.current.night,glow:f.current.glow},m(h.current),v.current()},[]);return p.useEffect(()=>{const n=window.matchMedia("(prefers-reduced-motion: reduce)");let d=!0,u=0,g=0,b=0;const x=C=>{if(u=0,!d||document.hidden){g=0;return}if(!n.matches&&g&&C-g<1e3/30){u=requestAnimationFrame(x);return}const N=g?C-g:0;g=C,n.matches||(b+=N);const M=C-o.current.started,L=h.current?0:1,_=h.current?1:.45,T={night:o.current.night+(L-o.current.night)*(n.matches?1:k(M/y.background)),glow:o.current.glow+(_-o.current.glow)*(n.matches?1:k(M/y.glow)),timeMs:b};f.current=T,r(T),n.matches||(u=requestAnimationFrame(x))},w=()=>{if(document.hidden){cancelAnimationFrame(u),u=0,g=0;return}u||(u=requestAnimationFrame(x))};v.current=w;const E=new IntersectionObserver(([C])=>{C&&(d=C.isIntersecting,d&&w())});return E.observe(a.current),document.addEventListener("visibilitychange",w),n.addEventListener("change",w),w(),()=>{cancelAnimationFrame(u),E.disconnect(),document.removeEventListener("visibilitychange",w),n.removeEventListener("change",w)}},[]),c.jsxs("div",{className:"live-demo",ref:a,children:[c.jsx("div",{className:"device-frame",children:c.jsx("div",{className:"device-viewport",children:c.jsx(D,{enabled:t,...e,onToggle:i})})}),c.jsxs("div",{className:"demo-caption",children:[c.jsx("span",{children:"Interactive preview"}),c.jsx("span",{"aria-live":"polite",children:t?"ON keeps your screen awake":"OFF restores your timeout"})]})]})}export{H as AppToggle};
