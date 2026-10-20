import{r as u}from"./index.saqyeS7l.js";var F={exports:{}},M={};var _;function B(){if(_)return M;_=1;var s=Symbol.for("react.transitional.element"),f=Symbol.for("react.fragment");function e(h,t,o){var r=null;if(o!==void 0&&(r=""+o),t.key!==void 0&&(r=""+t.key),"key"in t){o={};for(var v in t)v!=="key"&&(o[v]=t[v])}else o=t;return t=o.ref,{$$typeof:s,type:h,key:r,ref:t!==void 0?t:null,props:o}}return M.Fragment=f,M.jsx=e,M.jsxs=e,M}var P;function $(){return P||(P=1,F.exports=B()),F.exports}var l=$();const q=`precision highp float;

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
`,H=`precision highp float;

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
`,U="attribute vec2 p; void main(){gl_Position=vec4(p,0.,1.);}";function k(s,f){const e=s.getContext("webgl",{preserveDrawingBuffer:!0,premultipliedAlpha:!1,antialias:!0});if(!e)return null;const h=[],t=e.createProgram(),o=e.createBuffer(),r=()=>{e.deleteBuffer(o),e.deleteProgram(t),h.forEach(d=>e.deleteShader(d))};if(!t||!o)return r(),null;for(const[d,a]of[[e.VERTEX_SHADER,U],[e.FRAGMENT_SHADER,f]]){const n=e.createShader(d);if(!n||(h.push(n),e.shaderSource(n,a),e.compileShader(n),!e.getShaderParameter(n,e.COMPILE_STATUS)))return r(),null;e.attachShader(t,n)}if(e.linkProgram(t),!e.getProgramParameter(t,e.LINK_STATUS))return r(),null;e.useProgram(t),e.bindBuffer(e.ARRAY_BUFFER,o),e.bufferData(e.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),e.STATIC_DRAW);const v=e.getAttribLocation(t,"p");e.enableVertexAttribArray(v),e.vertexAttribPointer(v,2,e.FLOAT,!1,0,0);const g=new Map,i=d=>(g.has(d)||g.set(d,e.getUniformLocation(t,d)),g.get(d));return{resize(d,a,n){s.width=Math.round(d*n),s.height=Math.round(a*n),e.viewport(0,0,s.width,s.height)},draw(d){e.useProgram(t),e.uniform2f(i("iResolution"),s.width,s.height);for(const[a,n]of Object.entries(d))e.uniform1f(i(a),n);e.drawArrays(e.TRIANGLES,0,3),e.flush()},dispose:r}}function V(s,f,e=210,h=2){s.width=Math.round(e*h),s.height=Math.round(e*h);const t=s.getContext("2d");if(!t)return;t.scale(h,h);const o=e/2,r=e*.38,v=(a,n,c,b)=>{t.fillStyle=b,t.beginPath(),t.arc(a,n,c,0,Math.PI*2),t.fill()},g=(a,n,c,b)=>{const m=t.createRadialGradient(a,n,0,a,n,c);return b.forEach((x,p)=>m.addColorStop(p/(b.length-1),x)),m},i=e*(f?.11:.17);if(f){v(o,r,i*2.2,g(o,r,i*2.2,["#fff8e780","#ffb74d40","#ffb74d00"]));for(let a=0;a<8;a++){const n=a/8*Math.PI*2,c=o+Math.cos(n)*i*1.15,b=r+Math.sin(n)*i*1.15,m=o+Math.cos(n)*i*1.75,x=r+Math.sin(n)*i*1.75,p=t.createLinearGradient(c,b,m,x);p.addColorStop(0,"#ffcc80d9"),p.addColorStop(1,"#ffcc8000"),t.strokeStyle=p,t.lineWidth=i*.14,t.lineCap="round",t.beginPath(),t.moveTo(c,b),t.lineTo(m,x),t.stroke()}v(o,r,i,g(o,r,i,["#fffde7","#ffb300","#ff8f00"]))}else{for(let a=4;a>=1;a--){const n=.07*a,c=i*(1.25+a*.12);v(o,r,c,g(o,r,c,[`rgba(0,229,255,${n})`,`rgba(124,77,255,${n*.6})`,"#7c4dff00"]))}v(o,r,i,g(o-i*.12,r-i*.08,i*1.1,["#e8f4ff","#80d4ff","#7c4dff","#4a2080"])),v(o+i*.42,r-i*.06,i*.88,"#14102a"),t.globalCompositeOperation="screen",v(o-i*.2,r,i*.5,g(o-i*.35,r,i*.55,["#00e5ffe6","#00e5ff00"])),t.globalCompositeOperation="source-over"}[[-1.4,.5,2.5],[1.1,.9,1.8],[.3,-1.2,2],[-.9,-.7,1.5]].forEach(([a,n,c])=>{v(o+a*i,r+n*i,c/3,f?"#ffe082bf":"#b8e4ffbf")})}function I(s){const f=Math.max(0,Math.min(1,s));if(f===0||f===1)return f;let e=0,h=1,t=f;for(let o=0;o<18;o++)3*(1-t)*(1-t)*t*.4+3*(1-t)*t*t*.2+t**3<f?e=t:h=t,t=(e+h)/2;return 3*(1-t)*t*t+t**3}const E={background:1100,glow:400,backgroundLoop:4e4,faceLoop:12e3};function Y({enabled:s,night:f,glow:e,timeMs:h,clock:t="9:41",onToggle:o,fontUrl:r="/fonts/Roboto.ttf"}){const v=u.useRef(null),g=u.useRef(null),i=u.useRef(null),d=u.useRef(null),a=u.useRef(null);return u.useLayoutEffect(()=>{const n=k(v.current,q),c=k(g.current,H);return n?.resize(360,780,3),c?.resize(210,210,3),a.current={background:n,face:c},()=>{n?.dispose(),c?.dispose()}},[]),u.useLayoutEffect(()=>{a.current?.background?.draw({iMode:f,iTime:h%E.backgroundLoop/40}),a.current?.face?.draw({iNeverMode:s?0:1,iGlow:e,iTime:h%E.faceLoop/12})},[s,f,e,h]),u.useLayoutEffect(()=>{V(i.current,s,210,3)},[s]),u.useLayoutEffect(()=>{const n=d.current,c=210*1.14;n.width=Math.round(c*3),n.height=Math.round(c*3);const b=n.getContext("2d");if(!b)return;b.scale(3,3);const m=c/2,x=c*.52,p=s?"255,183,77":"124,77,255",w=b.createRadialGradient(m,m,0,m,m,x);w.addColorStop(0,`rgba(${p},${.35*e})`),w.addColorStop(.5,`rgba(${p},${.08*e})`),w.addColorStop(1,`rgba(${p},0)`),b.fillStyle=w,b.fillRect(0,0,c,c)},[s,e]),l.jsxs("div",{className:"android-screen","data-mode":s?"on":"off",children:[l.jsx("style",{children:`@font-face { font-family: "Android Roboto"; src: url("${r}") format("truetype"); font-style: normal; font-weight: 100 900; font-display: swap; }`}),l.jsx("canvas",{ref:v,className:"android-background","aria-hidden":"true"}),l.jsxs("div",{className:"android-status","aria-hidden":"true",children:[l.jsx("span",{children:t}),l.jsxs("svg",{width:"44",height:"14",viewBox:"0 0 44 14",fill:"currentColor",children:[l.jsx("path",{d:"M0 3Q8-3 16 3L8 11Z"}),l.jsx("path",{d:"M20 12L30 2V12Z"}),l.jsx("path",{d:"M36 2H38V0H41V2H43V13H36Z"})]})]}),l.jsxs("div",{className:"android-content",children:[l.jsx("div",{className:"android-title",children:"Keep screen on"}),l.jsxs("button",{className:"android-toggle",role:"switch",type:"button","aria-label":"Keep screen on","aria-checked":s,onClick:o,tabIndex:o?0:-1,children:[l.jsx("canvas",{ref:d,className:"android-bloom","aria-hidden":"true"}),l.jsx("canvas",{ref:g,className:"android-face","aria-hidden":"true"}),l.jsx("canvas",{ref:i,className:"android-icon","aria-hidden":"true"}),l.jsx("span",{className:"android-label",children:s?"ON":"OFF"})]})]}),l.jsx("div",{className:"android-navigation","aria-hidden":"true"})]})}function W({defaultMode:s="on"}){const f=s==="on",[e,h]=u.useState(f),[t,o]=u.useState(""),[r,v]=u.useState({night:f?0:1,glow:f?1:.45,timeMs:0}),g=u.useRef(null),i=u.useRef(null),d=u.useRef(r),a=u.useRef(f),n=u.useRef({started:0,night:r.night,glow:r.glow}),c=u.useRef(()=>{}),b=u.useCallback(()=>{a.current=!a.current,n.current={started:performance.now(),night:d.current.night,glow:d.current.glow},h(a.current),c.current()},[]);return u.useEffect(()=>{const m=new Intl.DateTimeFormat(void 0,{hour:"numeric",minute:"2-digit"});let x;const p=()=>{clearTimeout(x),!document.hidden&&(o(m.format(new Date)),x=setTimeout(p,6e4-Date.now()%6e4))};return p(),document.addEventListener("visibilitychange",p),()=>{clearTimeout(x),document.removeEventListener("visibilitychange",p)}},[]),u.useEffect(()=>{const m=window.matchMedia("(prefers-reduced-motion: reduce)"),x=k(i.current,q),p=()=>{x?.resize(window.innerWidth,window.innerHeight,Math.min(1,960/window.innerWidth)),x?.draw({iMode:d.current.night,iTime:d.current.timeMs%E.backgroundLoop/40})};p(),window.addEventListener("resize",p);let w=!0,C=0,R=0,L=0;const S=T=>{if(C=0,!w||document.hidden){R=0;return}if(!m.matches&&R&&T-R<1e3/30){C=requestAnimationFrame(S);return}const D=R?T-R:0;R=T,m.matches||(L+=D);const N=T-n.current.started,G=a.current?0:1,O=a.current?1:.45,A={night:n.current.night+(G-n.current.night)*(m.matches?1:I(N/E.background)),glow:n.current.glow+(O-n.current.glow)*(m.matches?1:I(N/E.glow)),timeMs:L};x?.draw({iMode:A.night,iTime:A.timeMs%E.backgroundLoop/40}),d.current=A,v(A),m.matches||(C=requestAnimationFrame(S))},y=()=>{if(document.hidden){cancelAnimationFrame(C),C=0,R=0;return}C||(C=requestAnimationFrame(S))};c.current=y;const j=new IntersectionObserver(([T])=>{T&&(w=T.isIntersecting,w&&y())});return j.observe(g.current),document.addEventListener("visibilitychange",y),m.addEventListener("change",y),y(),()=>{cancelAnimationFrame(C),j.disconnect(),window.removeEventListener("resize",p),x?.dispose(),document.removeEventListener("visibilitychange",y),m.removeEventListener("change",y)}},[]),l.jsxs("div",{className:"live-demo",ref:g,children:[l.jsx("div",{className:"page-ambient","data-mode":e?"on":"off","aria-hidden":"true",children:l.jsx("canvas",{ref:i})}),l.jsx("div",{className:"device-frame",children:l.jsx("div",{className:"device-viewport",children:l.jsx(Y,{enabled:e,...r,clock:t,onToggle:b})})})]})}export{W as AppToggle};
