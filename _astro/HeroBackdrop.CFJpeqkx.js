import{j as _}from"./jsx-runtime.u17CrQMm.js";import{r as d}from"./index.saqyeS7l.js";const w=`#version 300 es
precision mediump float;

layout(location = 0) in vec4 a_position;

uniform vec2 u_resolution;
uniform float u_pixelRatio;
uniform float u_imageAspectRatio;

uniform float u_originX;
uniform float u_originY;
uniform float u_worldWidth;
uniform float u_worldHeight;
uniform float u_fit;

uniform float u_scale;
uniform float u_rotation;
uniform float u_offsetX;
uniform float u_offsetY;

uniform float u_pxSize;

out vec2 v_objectUV;
out vec2 v_objectBoxSize;
out vec2 v_objectHelperBox;

out vec2 v_responsiveUV;
out vec2 v_responsiveBoxSize;
out vec2 v_responsiveHelperBox;
out vec2 v_responsiveBoxGivenSize;

out vec2 v_patternUV;
out vec2 v_patternBoxSize;
out vec2 v_patternHelperBox;

out vec2 v_imageUV;

// #define ADD_HELPERS

vec3 getBoxSize(float boxRatio, vec2 givenBoxSize, vec2 maxBoxSize) {
  vec2 box = vec2(0.);
  // fit = none
  box.x = boxRatio * min(givenBoxSize.x / boxRatio, givenBoxSize.y);
  float noFitBoxWidth = box.x;
  if (u_fit == 1.) { // fit = contain
    box.x = boxRatio * min(maxBoxSize[0] / boxRatio, maxBoxSize[1]);
  } else if (u_fit == 2.) { // fit = cover
    box.x = boxRatio * max(maxBoxSize[0] / boxRatio, maxBoxSize[1]);
  }
  box.y = box.x / boxRatio;
  return vec3(box, noFitBoxWidth);
}

void main() {
  gl_Position = a_position;

  vec2 uv = gl_Position.xy * .5;
  vec2 boxOrigin = vec2(.5 - u_originX, u_originY - .5);
  vec2 givenBoxSize = vec2(u_worldWidth, u_worldHeight);
  givenBoxSize = max(givenBoxSize, vec2(1.)) * u_pixelRatio;
  vec2 maxBoxSize = vec2(max(u_resolution.x, givenBoxSize.x), max(u_resolution.y, givenBoxSize.y));
  float r = u_rotation * 3.14159265358979323846 / 180.;
  mat2 graphicRotation = mat2(cos(r), sin(r), -sin(r), cos(r));
  vec2 graphicOffset = vec2(-u_offsetX, u_offsetY);


  // ===================================================
  // Sizing api for graphic objects with fixed ratio
  // (currently supports only ratio = 1)

  float fixedRatio = 1.;
  vec2 fixedRatioBoxGivenSize = vec2(
  (u_worldWidth == 0.) ? u_resolution.x : givenBoxSize.x,
  (u_worldHeight == 0.) ? u_resolution.y : givenBoxSize.y
  );

  v_objectBoxSize = getBoxSize(fixedRatio, fixedRatioBoxGivenSize, maxBoxSize).xy;
  vec2 objectWorldScale = u_resolution.xy / v_objectBoxSize;

  #ifdef ADD_HELPERS
  v_objectHelperBox = uv;
  v_objectHelperBox *= objectWorldScale;
  v_objectHelperBox += boxOrigin * (objectWorldScale - 1.);
  #endif

  v_objectUV = uv;
  v_objectUV *= objectWorldScale;
  v_objectUV += boxOrigin * (objectWorldScale - 1.);
  v_objectUV += graphicOffset;
  v_objectUV /= u_scale;
  v_objectUV = graphicRotation * v_objectUV;


  // ===================================================


  // ===================================================
  // Sizing api for graphic objects with either givenBoxSize ratio or canvas ratio.
  // Full-screen mode available with u_worldWidth = u_worldHeight = 0

  v_responsiveBoxGivenSize = vec2(
  (u_worldWidth == 0.) ? u_resolution.x : givenBoxSize.x,
  (u_worldHeight == 0.) ? u_resolution.y : givenBoxSize.y
  );
  float responsiveRatio = v_responsiveBoxGivenSize.x / v_responsiveBoxGivenSize.y;
  v_responsiveBoxSize = getBoxSize(responsiveRatio, v_responsiveBoxGivenSize, maxBoxSize).xy;
  vec2 responsiveBoxScale = u_resolution.xy / v_responsiveBoxSize;

  #ifdef ADD_HELPERS
  v_responsiveHelperBox = uv;
  v_responsiveHelperBox *= responsiveBoxScale;
  v_responsiveHelperBox += boxOrigin * (responsiveBoxScale - 1.);
  #endif

  v_responsiveUV = uv;
  v_responsiveUV *= responsiveBoxScale;
  v_responsiveUV += boxOrigin * (responsiveBoxScale - 1.);
  v_responsiveUV += graphicOffset;
  v_responsiveUV /= u_scale;
  v_responsiveUV.x *= responsiveRatio;
  v_responsiveUV = graphicRotation * v_responsiveUV;
  v_responsiveUV.x /= responsiveRatio;

  // ===================================================


  // ===================================================
  // Sizing api for patterns
  // (treating graphics as a image u_worldWidth x u_worldHeight size)

  float patternBoxRatio = givenBoxSize.x / givenBoxSize.y;
  vec2 patternBoxGivenSize = vec2(
  (u_worldWidth == 0.) ? u_resolution.x : givenBoxSize.x,
  (u_worldHeight == 0.) ? u_resolution.y : givenBoxSize.y
  );
  patternBoxRatio = patternBoxGivenSize.x / patternBoxGivenSize.y;

  vec3 boxSizeData = getBoxSize(patternBoxRatio, patternBoxGivenSize, maxBoxSize);
  v_patternBoxSize = boxSizeData.xy;
  float patternBoxNoFitBoxWidth = boxSizeData.z;
  vec2 patternBoxScale = u_resolution.xy / v_patternBoxSize;

  #ifdef ADD_HELPERS
  v_patternHelperBox = uv;
  v_patternHelperBox *= patternBoxScale;
  v_patternHelperBox += boxOrigin * (patternBoxScale - 1.);
  #endif

  v_patternUV = uv;
  v_patternUV += graphicOffset / patternBoxScale;
  v_patternUV += boxOrigin;
  v_patternUV -= boxOrigin / patternBoxScale;
  v_patternUV *= u_resolution.xy;
  v_patternUV /= u_pixelRatio;
  if (u_fit > 0.) {
    v_patternUV *= (patternBoxNoFitBoxWidth / v_patternBoxSize.x);
  }
  v_patternUV /= u_scale;
  v_patternUV = graphicRotation * v_patternUV;
  v_patternUV += boxOrigin / patternBoxScale;
  v_patternUV -= boxOrigin;
  // x100 is a default multiplier between vertex and fragmant shaders
  // we use it to avoid UV presision issues
  v_patternUV *= .01;

  // ===================================================


  // ===================================================
  // Sizing api for images

  vec2 imageBoxSize;
  if (u_fit == 1.) { // contain
    imageBoxSize.x = min(maxBoxSize.x / u_imageAspectRatio, maxBoxSize.y) * u_imageAspectRatio;
  } else if (u_fit == 2.) { // cover
    imageBoxSize.x = max(maxBoxSize.x / u_imageAspectRatio, maxBoxSize.y) * u_imageAspectRatio;
  } else {
    imageBoxSize.x = min(10.0, 10.0 / u_imageAspectRatio * u_imageAspectRatio);
  }
  imageBoxSize.y = imageBoxSize.x / u_imageAspectRatio;
  vec2 imageBoxScale = u_resolution.xy / imageBoxSize;

  #ifdef ADD_HELPERS
  vec2 imageHelperBox = uv;
  imageHelperBox *= imageBoxScale;
  imageHelperBox += boxOrigin * (imageBoxScale - 1.);
  #endif

  v_imageUV = uv;
  v_imageUV *= imageBoxScale;
  v_imageUV += boxOrigin * (imageBoxScale - 1.);
  v_imageUV += graphicOffset;
  v_imageUV /= u_scale;
  v_imageUV.x *= u_imageAspectRatio;
  v_imageUV = graphicRotation * v_imageUV;
  v_imageUV.x /= u_imageAspectRatio;

  v_imageUV += .5;
  v_imageUV.y = 1. - v_imageUV.y;

  // ===================================================

}`,b=1920*1080*4;let A=class{parentElement;canvasElement;gl;program=null;uniformLocations={};fragmentShader;rafId=null;lastRenderTime=0;currentFrame=0;speed=0;providedUniforms;hasBeenDisposed=!1;resolutionChanged=!0;textures=new Map;minPixelRatio;maxPixelCount;isSafari=T();uniformCache={};textureUnitMap=new Map;constructor(e,r,i,a,o=0,s=0,n=2,u=b){if(e instanceof HTMLElement)this.parentElement=e;else throw new Error("Paper Shaders: parent element must be an HTMLElement");if(!document.querySelector("style[data-paper-shader]")){const f=document.createElement("style");f.innerHTML=V,f.setAttribute("data-paper-shader",""),document.head.prepend(f)}const c=document.createElement("canvas");this.canvasElement=c,this.parentElement.prepend(c),this.fragmentShader=r,this.providedUniforms=i,this.currentFrame=s,this.minPixelRatio=n,this.maxPixelCount=u;const l=c.getContext("webgl2",a);if(!l)throw new Error("Paper Shaders: WebGL is not supported in this browser");this.gl=l,this.initProgram(),this.setupPositionAttribute(),this.setupUniforms(),this.setUniformValues(this.providedUniforms),this.setupResizeObserver(),this.setSpeed(o),this.parentElement.setAttribute("data-paper-shader",""),this.parentElement.paperShaderMount=this}initProgram=()=>{const e=y(this.gl,w,this.fragmentShader);e&&(this.program=e)};setupPositionAttribute=()=>{const e=this.gl.getAttribLocation(this.program,"a_position"),r=this.gl.createBuffer();this.gl.bindBuffer(this.gl.ARRAY_BUFFER,r);const i=[-1,-1,1,-1,-1,1,-1,1,1,-1,1,1];this.gl.bufferData(this.gl.ARRAY_BUFFER,new Float32Array(i),this.gl.STATIC_DRAW),this.gl.enableVertexAttribArray(e),this.gl.vertexAttribPointer(e,2,this.gl.FLOAT,!1,0,0)};setupUniforms=()=>{const e={u_time:this.gl.getUniformLocation(this.program,"u_time"),u_pixelRatio:this.gl.getUniformLocation(this.program,"u_pixelRatio"),u_resolution:this.gl.getUniformLocation(this.program,"u_resolution")};Object.entries(this.providedUniforms).forEach(([r,i])=>{if(e[r]=this.gl.getUniformLocation(this.program,r),i instanceof HTMLImageElement){const a=`${r}AspectRatio`;e[a]=this.gl.getUniformLocation(this.program,a)}}),this.uniformLocations=e};renderScale=1;parentWidth=0;parentHeight=0;resizeObserver=null;setupResizeObserver=()=>{this.resizeObserver=new ResizeObserver(([r])=>{r?.borderBoxSize[0]&&(this.parentWidth=r.borderBoxSize[0].inlineSize,this.parentHeight=r.borderBoxSize[0].blockSize),this.handleResize()}),this.resizeObserver.observe(this.parentElement),visualViewport?.addEventListener("resize",this.handleVisualViewportChange);const e=this.parentElement.getBoundingClientRect();this.parentWidth=e.width,this.parentHeight=e.height,this.handleResize()};resizeRafId=null;handleVisualViewportChange=()=>{this.resizeRafId!==null&&cancelAnimationFrame(this.resizeRafId),this.resizeRafId=requestAnimationFrame(()=>{this.resizeRafId=requestAnimationFrame(()=>{this.handleResize()})})};handleResize=()=>{this.resizeRafId!==null&&cancelAnimationFrame(this.resizeRafId);const e=visualViewport?.scale??1,r=window.innerWidth-document.documentElement.clientWidth,i=visualViewport?visualViewport.scale*visualViewport.width+r:window.innerWidth,a=Math.round(1e4*window.outerWidth/i)/1e4,o=this.isSafari?devicePixelRatio:devicePixelRatio/a,n=Math.max(o,this.minPixelRatio)*a*e,u=this.parentWidth*n,c=this.parentHeight*n,l=Math.sqrt(this.maxPixelCount)/Math.sqrt(u*c),f=n*Math.min(1,l),g=Math.round(this.parentWidth*f),h=Math.round(this.parentHeight*f);(this.canvasElement.width!==g||this.canvasElement.height!==h||this.renderScale!==f)&&(this.renderScale=f,this.canvasElement.width=g,this.canvasElement.height=h,this.resolutionChanged=!0,this.gl.viewport(0,0,this.gl.canvas.width,this.gl.canvas.height),this.render(performance.now()))};render=e=>{if(this.hasBeenDisposed)return;if(this.program===null){console.warn("Tried to render before program or gl was initialized");return}const r=e-this.lastRenderTime;this.lastRenderTime=e,this.speed!==0&&(this.currentFrame+=r*this.speed),this.gl.clear(this.gl.COLOR_BUFFER_BIT),this.gl.useProgram(this.program),this.gl.uniform1f(this.uniformLocations.u_time,this.currentFrame*.001),this.resolutionChanged&&(this.gl.uniform2f(this.uniformLocations.u_resolution,this.gl.canvas.width,this.gl.canvas.height),this.gl.uniform1f(this.uniformLocations.u_pixelRatio,this.renderScale),this.resolutionChanged=!1),this.gl.drawArrays(this.gl.TRIANGLES,0,6),this.speed!==0?this.requestRender():this.rafId=null};requestRender=()=>{this.rafId!==null&&cancelAnimationFrame(this.rafId),this.rafId=requestAnimationFrame(this.render)};setTextureUniform=(e,r)=>{if(!r.complete||r.naturalWidth===0)throw new Error(`Paper Shaders: image for uniform ${e} must be fully loaded`);const i=this.textures.get(e);i&&this.gl.deleteTexture(i),this.textureUnitMap.has(e)||this.textureUnitMap.set(e,this.textureUnitMap.size);const a=this.textureUnitMap.get(e);this.gl.activeTexture(this.gl.TEXTURE0+a);const o=this.gl.createTexture();this.gl.bindTexture(this.gl.TEXTURE_2D,o),this.gl.texParameteri(this.gl.TEXTURE_2D,this.gl.TEXTURE_WRAP_S,this.gl.CLAMP_TO_EDGE),this.gl.texParameteri(this.gl.TEXTURE_2D,this.gl.TEXTURE_WRAP_T,this.gl.CLAMP_TO_EDGE),this.gl.texParameteri(this.gl.TEXTURE_2D,this.gl.TEXTURE_MIN_FILTER,this.gl.LINEAR),this.gl.texParameteri(this.gl.TEXTURE_2D,this.gl.TEXTURE_MAG_FILTER,this.gl.LINEAR),this.gl.texImage2D(this.gl.TEXTURE_2D,0,this.gl.RGBA,this.gl.RGBA,this.gl.UNSIGNED_BYTE,r);const s=this.gl.getError();if(s!==this.gl.NO_ERROR||o===null){console.error("Paper Shaders: WebGL error when uploading texture:",s);return}this.textures.set(e,o);const n=this.uniformLocations[e];if(n){this.gl.uniform1i(n,a);const u=`${e}AspectRatio`,c=this.uniformLocations[u];if(c){const l=r.naturalWidth/r.naturalHeight;this.gl.uniform1f(c,l)}}};areUniformValuesEqual=(e,r)=>e===r?!0:Array.isArray(e)&&Array.isArray(r)&&e.length===r.length?e.every((i,a)=>this.areUniformValuesEqual(i,r[a])):!1;setUniformValues=e=>{this.gl.useProgram(this.program),Object.entries(e).forEach(([r,i])=>{let a=i;if(i instanceof HTMLImageElement&&(a=`${i.src.slice(0,200)}|${i.naturalWidth}x${i.naturalHeight}`),this.areUniformValuesEqual(this.uniformCache[r],a))return;this.uniformCache[r]=a;const o=this.uniformLocations[r];if(!o){console.warn(`Uniform location for ${r} not found`);return}if(i instanceof HTMLImageElement)this.setTextureUniform(r,i);else if(Array.isArray(i)){let s=null,n=null;if(i[0]!==void 0&&Array.isArray(i[0])){const u=i[0].length;if(i.every(c=>c.length===u))s=i.flat(),n=u;else{console.warn(`All child arrays must be the same length for ${r}`);return}}else s=i,n=s.length;switch(n){case 2:this.gl.uniform2fv(o,s);break;case 3:this.gl.uniform3fv(o,s);break;case 4:this.gl.uniform4fv(o,s);break;case 9:this.gl.uniformMatrix3fv(o,!1,s);break;case 16:this.gl.uniformMatrix4fv(o,!1,s);break;default:console.warn(`Unsupported uniform array length: ${n}`)}}else typeof i=="number"?this.gl.uniform1f(o,i):typeof i=="boolean"?this.gl.uniform1i(o,i?1:0):console.warn(`Unsupported uniform type for ${r}: ${typeof i}`)})};getCurrentFrame=()=>this.currentFrame;setFrame=e=>{this.currentFrame=e,this.lastRenderTime=performance.now(),this.render(performance.now())};setSpeed=(e=1)=>{this.speed=e,this.rafId===null&&e!==0&&(this.lastRenderTime=performance.now(),this.rafId=requestAnimationFrame(this.render)),this.rafId!==null&&e===0&&(cancelAnimationFrame(this.rafId),this.rafId=null)};setMaxPixelCount=(e=b)=>{this.maxPixelCount=e,this.handleResize()};setMinPixelRatio=(e=2)=>{this.minPixelRatio=e,this.handleResize()};setUniforms=e=>{this.setUniformValues(e),this.providedUniforms={...this.providedUniforms,...e},this.render(performance.now())};dispose=()=>{this.hasBeenDisposed=!0,this.rafId!==null&&(cancelAnimationFrame(this.rafId),this.rafId=null),this.gl&&this.program&&(this.textures.forEach(e=>{this.gl.deleteTexture(e)}),this.textures.clear(),this.gl.deleteProgram(this.program),this.program=null,this.gl.bindBuffer(this.gl.ARRAY_BUFFER,null),this.gl.bindBuffer(this.gl.ELEMENT_ARRAY_BUFFER,null),this.gl.bindRenderbuffer(this.gl.RENDERBUFFER,null),this.gl.bindFramebuffer(this.gl.FRAMEBUFFER,null),this.gl.getError()),this.resizeObserver&&(this.resizeObserver.disconnect(),this.resizeObserver=null),visualViewport?.removeEventListener("resize",this.handleVisualViewportChange),this.uniformLocations={},this.parentElement.paperShaderMount=void 0}};function B(t,e,r){const i=t.createShader(e);return i?(t.shaderSource(i,r),t.compileShader(i),t.getShaderParameter(i,t.COMPILE_STATUS)?i:(console.error("An error occurred compiling the shaders: "+t.getShaderInfoLog(i)),t.deleteShader(i),null)):null}function y(t,e,r){const i=t.getShaderPrecisionFormat(t.FRAGMENT_SHADER,t.MEDIUM_FLOAT),a=i?i.precision:null;a&&a<23&&(e=e.replace(/precision\s+(lowp|mediump)\s+float;/g,"precision highp float;"),r=r.replace(/precision\s+(lowp|mediump)\s+float/g,"precision highp float").replace(/\b(uniform|varying|attribute)\s+(lowp|mediump)\s+(\w+)/g,"$1 highp $3"));const o=B(t,t.VERTEX_SHADER,e),s=B(t,t.FRAGMENT_SHADER,r);if(!o||!s)return null;const n=t.createProgram();return n?(t.attachShader(n,o),t.attachShader(n,s),t.linkProgram(n),t.getProgramParameter(n,t.LINK_STATUS)?(t.detachShader(n,o),t.detachShader(n,s),t.deleteShader(o),t.deleteShader(s),n):(console.error("Unable to initialize the shader program: "+t.getProgramInfoLog(n)),t.deleteProgram(n),t.deleteShader(o),t.deleteShader(s),null)):null}const V=`@layer paper-shaders {
  :where([data-paper-shader]) {
    isolation: isolate;
    position: relative;

    & canvas {
      contain: strict;
      display: block;
      position: absolute;
      inset: 0;
      z-index: -1;
      width: 100%;
      height: 100%;
      border-radius: inherit;
    }
  }
}`;function T(){const t=navigator.userAgent.toLowerCase();return t.includes("safari")&&!t.includes("chrome")&&!t.includes("android")}const I=`
in vec2 v_objectUV;
in vec2 v_responsiveUV;
in vec2 v_responsiveBoxGivenSize;
in vec2 v_patternUV;
in vec2 v_imageUV;`,F={fit:"contain",scale:1,rotation:0,offsetX:0,offsetY:0,originX:.5,originY:.5,worldWidth:0,worldHeight:0},L={none:0,contain:1,cover:2},M=`
#define TWO_PI 6.28318530718
#define PI 3.14159265358979323846
`,P=`
vec2 rotate(vec2 uv, float th) {
  return mat2(cos(th), sin(th), -sin(th), cos(th)) * uv;
}
`,H=`
  color += 1. / 256. * (fract(sin(dot(.014 * gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453123) - .5);
`,E={maxColorCount:10},W=`#version 300 es
precision mediump float;

uniform float u_time;

uniform vec4 u_colors[${E.maxColorCount}];
uniform float u_colorsCount;

uniform float u_distortion;
uniform float u_swirl;

${I}

out vec4 fragColor;

${M}
${P}

vec2 getPosition(int i, float t) {
  float a = float(i) * .37;
  float b = .6 + mod(float(i), 3.) * .3;
  float c = .8 + mod(float(i + 1), 4.) * 0.25;

  float x = sin(t * b + a);
  float y = cos(t * c + a * 1.5);

  return .5 + .5 * vec2(x, y);
}

void main() {
  vec2 shape_uv = v_objectUV;

  shape_uv += .5;

  float t = .5 * u_time;

  float radius = smoothstep(0., 1., length(shape_uv - .5));
  float center = 1. - radius;
  for (float i = 1.; i <= 2.; i++) {
    shape_uv.x += u_distortion * center / i * sin(t + i * .4 * smoothstep(.0, 1., shape_uv.y)) * cos(.2 * t + i * 2.4 * smoothstep(.0, 1., shape_uv.y));
    shape_uv.y += u_distortion * center / i * cos(t + i * 2. * smoothstep(.0, 1., shape_uv.x));
  }

  vec2 uvRotated = shape_uv;
  uvRotated -= vec2(.5);
  float angle = 3. * u_swirl * radius;
  uvRotated = rotate(uvRotated, -angle);
  uvRotated += vec2(.5);

  vec3 color = vec3(0.);
  float opacity = 0.;
  float totalWeight = 0.;

  for (int i = 0; i < ${E.maxColorCount}; i++) {
    if (i >= int(u_colorsCount)) break;

    vec2 pos = getPosition(i, t);
    vec3 colorFraction = u_colors[i].rgb * u_colors[i].a;
    float opacityFraction = u_colors[i].a;

    float dist = length(uvRotated - pos);

    dist = pow(dist, 3.5);
    float weight = 1. / (dist + 1e-3);
    color += colorFraction * weight;
    opacity += opacityFraction * weight;
    totalWeight += weight;
  }

  color /= totalWeight;
  opacity /= totalWeight;

  ${H}

  fragColor = vec4(color, opacity);
}
`;function O(t){if(Array.isArray(t))return t.length===4?t:t.length===3?[...t,1]:S;if(typeof t!="string")return S;let e,r,i,a=1;if(t.startsWith("#"))[e,r,i,a]=C(t);else if(t.startsWith("rgb"))[e,r,i,a]=j(t);else if(t.startsWith("hsl"))[e,r,i,a]=G(D(t));else return console.error("Unsupported color format",t),S;return[R(e,0,1),R(r,0,1),R(i,0,1),R(a,0,1)]}function C(t){t=t.replace(/^#/,""),t.length===3&&(t=t.split("").map(o=>o+o).join("")),t.length===6&&(t=t+"ff");const e=parseInt(t.slice(0,2),16)/255,r=parseInt(t.slice(2,4),16)/255,i=parseInt(t.slice(4,6),16)/255,a=parseInt(t.slice(6,8),16)/255;return[e,r,i,a]}function j(t){const e=t.match(/^rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([0-9.]+))?\s*\)$/i);return e?[parseInt(e[1]??"0")/255,parseInt(e[2]??"0")/255,parseInt(e[3]??"0")/255,e[4]===void 0?1:parseFloat(e[4])]:[0,0,0,1]}function D(t){const e=t.match(/^hsla?\s*\(\s*(\d+)\s*,\s*(\d+)%\s*,\s*(\d+)%\s*(?:,\s*([0-9.]+))?\s*\)$/i);return e?[parseInt(e[1]??"0"),parseInt(e[2]??"0"),parseInt(e[3]??"0"),e[4]===void 0?1:parseFloat(e[4])]:[0,0,0,1]}function G(t){const[e,r,i,a]=t,o=e/360,s=r/100,n=i/100;let u,c,l;if(r===0)u=c=l=n;else{const f=(v,x,m)=>(m<0&&(m+=1),m>1&&(m-=1),m<.16666666666666666?v+(x-v)*6*m:m<.5?x:m<.6666666666666666?v+(x-v)*(.6666666666666666-m)*6:v),g=n<.5?n*(1+s):n+s-n*s,h=2*n-g;u=f(h,g,o+1/3),c=f(h,g,o),l=f(h,g,o-1/3)}return[u,c,l,a]}const R=(t,e,r)=>Math.min(Math.max(t,e),r),S=[0,0,0,1];function $(t){const e=d.useRef(void 0),r=d.useCallback(i=>{const a=t.map(o=>{if(o!=null){if(typeof o=="function"){const s=o,n=s(i);return typeof n=="function"?n:()=>{s(null)}}return o.current=i,()=>{o.current=null}}});return()=>{a.forEach(o=>o?.())}},t);return d.useMemo(()=>t.every(i=>i==null)?null:i=>{e.current&&(e.current(),e.current=void 0),i!=null&&(e.current=r(i))},t)}async function U(t){const e={},r=[],i=o=>{try{return o.startsWith("/")||new URL(o),!0}catch{return!1}},a=o=>{try{return o.startsWith("/")?!1:new URL(o,window.location.origin).origin!==window.location.origin}catch{return!1}};return Object.entries(t).forEach(([o,s])=>{if(typeof s=="string"){if(!i(s)){console.warn(`Uniform "${o}" has invalid URL "${s}". Skipping image loading.`);return}const n=new Promise((u,c)=>{const l=new Image;a(s)&&(l.crossOrigin="anonymous"),l.onload=()=>{e[o]=l,u()},l.onerror=()=>{console.error(`Could not set uniforms. Failed to load image at ${s}`),c()},l.src=s});r.push(n)}else e[o]=s}),await Promise.all(r),e}const z=d.forwardRef(function({fragmentShader:e,uniforms:r,webGlContextAttributes:i,speed:a=0,frame:o=0,minPixelRatio:s,maxPixelCount:n,...u},c){const[l,f]=d.useState(!1),g=d.useRef(null),h=d.useRef(null);d.useEffect(()=>((async()=>{const m=await U(r);g.current&&!h.current&&(h.current=new A(g.current,e,m,i,a,o,s,n),f(!0))})(),()=>{h.current?.dispose(),h.current=null}),[e,i]),d.useEffect(()=>{(async()=>{const m=await U(r);h.current?.setUniforms(m)})()},[r,l]),d.useEffect(()=>{h.current?.setSpeed(a)},[a,l]),d.useEffect(()=>{h.current?.setMaxPixelCount(n)},[n,l]),d.useEffect(()=>{h.current?.setMinPixelRatio(s)},[s,l]),d.useEffect(()=>{h.current?.setFrame(o)},[o,l]);const v=$([g,c]);return _.jsx("div",{ref:v,...u})});z.displayName="ShaderMount";function X(t,e){for(const r in t){if(r==="colors"){const i=Array.isArray(t.colors),a=Array.isArray(e.colors);if(!i||!a){if(Object.is(t.colors,e.colors)===!1)return!1;continue}if(t.colors?.length!==e.colors?.length||!t.colors?.every((o,s)=>o===e.colors?.[s]))return!1;continue}if(Object.is(t[r],e[r])===!1)return!1}return!0}const p={params:{...F,speed:1,frame:0,colors:["#e0eaff","#241d9a","#f75092","#9f50d3"],distortion:.8,swirl:.1}},N=d.memo(function({speed:e=p.params.speed,frame:r=p.params.frame,colors:i=p.params.colors,distortion:a=p.params.distortion,swirl:o=p.params.swirl,fit:s=p.params.fit,rotation:n=p.params.rotation,scale:u=p.params.scale,originX:c=p.params.originX,originY:l=p.params.originY,offsetX:f=p.params.offsetX,offsetY:g=p.params.offsetY,worldWidth:h=p.params.worldWidth,worldHeight:v=p.params.worldHeight,...x}){const m={u_colors:i.map(O),u_colorsCount:i.length,u_distortion:a,u_swirl:o,u_fit:L[s],u_rotation:n,u_scale:u,u_offsetX:f,u_offsetY:g,u_originX:c,u_originY:l,u_worldWidth:h,u_worldHeight:v};return _.jsx(z,{...x,speed:e,frame:r,fragmentShader:W,uniforms:m})},X),Y=["#0d0d1a","#151328","#241c42","#49356f","#72609c"];function K(){const[t,e]=d.useState(!1);return d.useEffect(()=>{const r=window.matchMedia("(prefers-reduced-motion: reduce)"),i=()=>e(!r.matches);return i(),r.addEventListener("change",i),()=>r.removeEventListener("change",i)},[]),_.jsxs("div",{className:"hero-backdrop","aria-hidden":"true",children:[_.jsx(N,{className:"hero-backdrop__mesh",colors:Y,distortion:.56,swirl:.16,speed:t?.07:0,fit:"cover"}),_.jsx("div",{className:"hero-backdrop__veil"})]})}export{K as HeroBackdrop};
