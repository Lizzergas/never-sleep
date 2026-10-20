import { AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Background, Face } from "./Shader";

const ease = (frame: number, start: number, end: number) =>
    interpolate(frame, [start, end], [0, 1], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
        easing: (t) => t * t * (3 - 2 * t),
    });

function SunMoon({ on }: { on: number }) {
    return (
        <svg
            viewBox="0 0 200 200"
            style={{ position: "absolute", width: 230, height: 230, left: 75, top: 35 }}
        >
            <defs>
                <radialGradient id="sun">
                    <stop stopColor="#fff7bd" />
                    <stop offset=".45" stopColor="#ffd25d" />
                    <stop offset="1" stopColor="#f29232" />
                </radialGradient>
                <radialGradient id="moon">
                    <stop stopColor="#a6f8ff" />
                    <stop offset=".4" stopColor="#5bd9ea" />
                    <stop offset="1" stopColor="#7936f3" />
                </radialGradient>
                <mask id="crescent">
                    <rect width="200" height="200" fill="white" />
                    <circle cx="116" cy="82" r="42" fill="black" />
                </mask>
            </defs>
            <g opacity={1 - on}>
                <circle cx="95" cy="95" r="50" fill="url(#moon)" mask="url(#crescent)" />
            </g>
            <g opacity={on}>
                {Array.from({ length: 8 }, (_, i) => (
                    <line
                        key={i}
                        x1="100"
                        y1="28"
                        x2="100"
                        y2="47"
                        stroke="#ffd67b"
                        strokeWidth="3"
                        strokeLinecap="round"
                        transform={`rotate(${i * 45} 100 100)`}
                    />
                ))}
                <circle cx="100" cy="100" r="36" fill="url(#sun)" />
            </g>
        </svg>
    );
}

function App({ on, frame, tap }: { on: number; frame: number; tap: number }) {
    return (
        <AbsoluteFill>
            <Background night={1 - on} time={(frame / 30) * 25} />
            <div
                style={{
                    position: "absolute",
                    top: 265,
                    left: 0,
                    width: "100%",
                    textAlign: "center",
                }}
            >
                <span
                    style={{
                        display: "inline-block",
                        color: "white",
                        background: "#0009",
                        padding: "12px 24px",
                        borderRadius: 50,
                        fontSize: 30,
                    }}
                >
                    Keep screen on
                </span>
            </div>
            <div
                style={{
                    position: "absolute",
                    width: 380,
                    height: 380,
                    top: 365,
                    left: 80,
                    borderRadius: "50%",
                    background: "#0b0916",
                    boxShadow: `0 0 ${40 * on}px #ffa63633`,
                }}
            >
                <Face night={1 - on} time={(frame / 30) * 83.333} />
                <SunMoon on={on} />
                <div
                    style={{
                        position: "absolute",
                        bottom: 55,
                        width: "100%",
                        textAlign: "center",
                        fontSize: 27,
                        letterSpacing: 4,
                        fontWeight: 700,
                        color: "white",
                    }}
                >
                    {on > 0.5 ? "ON" : "OFF"}
                </div>
                {tap > 0 && tap < 1 && (
                    <div
                        style={{
                            position: "absolute",
                            left: "50%",
                            top: "50%",
                            width: 140,
                            height: 140,
                            border: "4px solid white",
                            borderRadius: "50%",
                            opacity: 1 - tap,
                            transform: `translate(-50%,-50%) scale(${0.3 + tap * 1.7})`,
                        }}
                    />
                )}
            </div>
        </AbsoluteFill>
    );
}

export function Promo() {
    const frame = useCurrentFrame();
    // OFF, tap ON and hold, tap OFF, then a quiet closing card.
    const on = ease(frame, 45, 90) * (1 - ease(frame, 210, 255));
    const closing = ease(frame, 285, 315);
    const tap = frame < 180 ? ease(frame, 42, 66) : ease(frame, 207, 231);
    return (
        <AbsoluteFill
            style={{ background: "#0a0a13", color: "#f7f4ff", fontFamily: "Arial, sans-serif" }}
        >
            <AbsoluteFill
                style={{
                    background: "radial-gradient(ellipse at 50% 55%, #28204b 0%, #0a0a13 72%)",
                }}
            />
            <AbsoluteFill
                style={{
                    opacity: on * (1 - closing),
                    background: "radial-gradient(ellipse at 50% 55%, #6b381d 0%, #0a0a13 72%)",
                }}
            />
            <AbsoluteFill style={{ opacity: 1 - closing }}>
                <div
                    style={{
                        position: "absolute",
                        top: 180,
                        width: "100%",
                        textAlign: "center",
                        fontSize: 52,
                        letterSpacing: -1,
                        fontWeight: 700,
                    }}
                >
                    Never Sleep
                </div>
                <div
                    style={{
                        position: "absolute",
                        top: 360,
                        left: 217,
                        width: 566,
                        height: 1012,
                        borderRadius: 64,
                        padding: 13,
                        background: "linear-gradient(135deg,#555460,#13131d 35%,#373540)",
                        boxShadow: "0 40px 90px #0008",
                        transform: "scale(1.14)",
                        transformOrigin: "top center",
                    }}
                >
                    <div
                        style={{
                            position: "relative",
                            width: 540,
                            height: 986,
                            borderRadius: 51,
                            overflow: "hidden",
                            background: "black",
                        }}
                    >
                        <App on={on} frame={frame} tap={tap} />
                        <div
                            style={{
                                position: "absolute",
                                top: 23,
                                left: 32,
                                right: 32,
                                fontSize: 17,
                                color: "white",
                                display: "flex",
                                justifyContent: "space-between",
                            }}
                        >
                            <span>9:41</span>
                            <span>▰</span>
                        </div>
                        <div
                            style={{
                                position: "absolute",
                                top: 17,
                                left: 243,
                                width: 54,
                                height: 15,
                                borderRadius: 20,
                                background: "#060608",
                            }}
                        />
                        <div
                            style={{
                                position: "absolute",
                                bottom: 14,
                                left: 195,
                                width: 150,
                                height: 5,
                                borderRadius: 5,
                                background: "#fffa",
                            }}
                        />
                    </div>
                </div>
            </AbsoluteFill>
            <AbsoluteFill
                style={{
                    opacity: closing,
                    alignItems: "center",
                    justifyContent: "center",
                    padding: "180px 90px",
                    textAlign: "center",
                    transform: `translateY(${(1 - closing) * 24}px)`,
                }}
            >
                <Img
                    src={staticFile("icon.png")}
                    style={{ width: 160, height: 160, borderRadius: 40, marginBottom: 55 }}
                />
                <div style={{ fontSize: 44, letterSpacing: -1, marginBottom: 24 }}>Never Sleep</div>
                <div style={{ fontSize: 94, fontWeight: 750, letterSpacing: -4, lineHeight: 1.08 }}>
                    Always awake
                </div>
                <div style={{ fontSize: 32, color: "#beb8cf", lineHeight: 1.5, marginTop: 34 }}>
                    Stay on task, even between taps.
                </div>
                <Img src={staticFile("play-badge.png")} style={{ width: 420, marginTop: 64 }} />
                <div style={{ fontSize: 29, color: "#c3b5ff", marginTop: 12 }}>neversleep.app</div>
            </AbsoluteFill>
        </AbsoluteFill>
    );
}
