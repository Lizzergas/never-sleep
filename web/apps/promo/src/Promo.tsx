import { useEffect, useState } from "react";
import {
    AbsoluteFill,
    Img,
    interpolate,
    staticFile,
    useCurrentFrame,
    delayRender,
    continueRender,
    cancelRender,
} from "remotion";
import { AppScreen } from "../../landing/src/components/android/AppScreen";
import { fastOutSlowIn } from "../../landing/src/components/android/animation";

const ease = (frame: number, start: number, end: number) =>
    interpolate(frame, [start, end], [0, 1], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
        easing: (t) => t * t * (3 - 2 * t),
    });

export function Promo({ opening, turnOff = true }: { opening: string; turnOff?: boolean }) {
    const frame = useCurrentFrame();
    const [fontHandle] = useState(() => delayRender("Loading Android Roboto"));
    useEffect(() => {
        Promise.all([
            document.fonts.load('500 18px "Android Roboto"'),
            document.fonts.load('700 14px "Android Roboto"'),
        ])
            .then(() => continueRender(fontHandle))
            .catch(cancelRender);
    }, [fontHandle]);
    // The one-tap cut goes directly from day into the ending.
    const turningOff = turnOff && frame >= 210;
    const enabled = frame >= 45 && !turningOff;
    const elapsed = ((frame - (turningOff ? 210 : 45)) / 30) * 1000;
    const on = turningOff ? 1 - fastOutSlowIn(elapsed / 1100) : fastOutSlowIn(elapsed / 1100);
    const glow = turningOff
        ? 1 - 0.55 * fastOutSlowIn(elapsed / 400)
        : 0.45 + 0.55 * fastOutSlowIn(elapsed / 400);
    const closing = turnOff ? ease(frame, 285, 315) : ease(frame, 165, 195);

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
                        top: 130,
                        left: 90,
                        width: 900,
                        textAlign: "center",
                        fontSize: 64,
                        lineHeight: 1.12,
                        letterSpacing: -1,
                        fontWeight: 700,
                        whiteSpace: "pre-line",
                        textWrap: "balance",
                        opacity: 1 - ease(frame, 105, 120),
                    }}
                >
                    {opening.replace(". ", ".\n")}
                </div>
                <div
                    style={{
                        position: "absolute",
                        top: 320,
                        left: 257,
                        width: 566,
                        height: 1196,
                        borderRadius: 64,
                        padding: 13,
                        background: "linear-gradient(135deg,#555460,#13131d 35%,#373540)",
                        boxShadow: "0 40px 90px #0008",

                        transformOrigin: "top center",
                    }}
                >
                    <div
                        style={{
                            position: "relative",
                            width: 540,
                            height: 1170,
                            borderRadius: 51,
                            overflow: "hidden",
                            background: "black",
                        }}
                    >
                        <div
                            style={{
                                width: 360,
                                height: 780,
                                transform: "scale(1.5)",
                                transformOrigin: "top left",
                            }}
                        >
                            <AppScreen
                                enabled={enabled}
                                night={1 - on}
                                glow={glow}
                                timeMs={(frame / 30) * 1000}
                                fontUrl={staticFile("fonts/Roboto.ttf")}
                            />
                        </div>
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
                <div
                    style={{
                        fontSize: 94,
                        fontWeight: 750,
                        letterSpacing: -4,
                        lineHeight: 1.08,
                        color: "#b9abf3",
                    }}
                >
                    Never sleep.
                </div>
                <div
                    style={{
                        fontSize: 94,
                        fontWeight: 750,
                        letterSpacing: -4,
                        lineHeight: 1.08,
                        color: "#ffd184",
                    }}
                >
                    Always awake.
                </div>
                <Img src={staticFile("play-badge.png")} style={{ width: 420, marginTop: 64 }} />
            </AbsoluteFill>
        </AbsoluteFill>
    );
}
