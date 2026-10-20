import { useCallback, useEffect, useRef, useState } from "react";
import { AppScreen } from "./android/AppScreen";
import { ANDROID_TIMING, fastOutSlowIn } from "./android/animation";
import type { ToggleMode } from "./toggleMode";
import { createShader } from "./android/shader";
import { BACKGROUND_SHADER } from "./android/shaders.generated";

export function AppToggle({ defaultMode = "on" }: { defaultMode?: ToggleMode }) {
    const initial = defaultMode === "on";
    const [enabled, setEnabled] = useState(initial);
    const [clock, setClock] = useState("");
    const [frame, setFrame] = useState({
        night: initial ? 0 : 1,
        glow: initial ? 1 : 0.45,
        timeMs: 0,
    });
    const stage = useRef<HTMLDivElement>(null);
    const ambientCanvas = useRef<HTMLCanvasElement>(null);
    const values = useRef(frame);
    const target = useRef(initial);
    const transition = useRef({ started: 0, night: frame.night, glow: frame.glow });
    const restart = useRef<() => void>(() => {});
    const toggle = useCallback(() => {
        target.current = !target.current;
        transition.current = {
            started: performance.now(),
            night: values.current.night,
            glow: values.current.glow,
        };
        setEnabled(target.current);
        restart.current();
    }, []);

    useEffect(() => {
        const formatter = new Intl.DateTimeFormat(undefined, {
            hour: "numeric",
            minute: "2-digit",
        });
        let timer: ReturnType<typeof setTimeout>;
        const update = () => {
            clearTimeout(timer);
            if (document.hidden) return;
            setClock(formatter.format(new Date()));
            timer = setTimeout(update, 60_000 - (Date.now() % 60_000));
        };
        update();
        document.addEventListener("visibilitychange", update);
        return () => {
            clearTimeout(timer);
            document.removeEventListener("visibilitychange", update);
        };
    }, []);

    useEffect(() => {
        const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
        const ambient = createShader(ambientCanvas.current!, BACKGROUND_SHADER);
        const resizeAmbient = () => {
            ambient?.resize(
                window.innerWidth,
                window.innerHeight,
                Math.min(1, 960 / window.innerWidth),
            );
            ambient?.draw({
                iMode: values.current.night,
                iTime: (values.current.timeMs % ANDROID_TIMING.backgroundLoop) / 40,
            });
        };
        resizeAmbient();
        window.addEventListener("resize", resizeAmbient);
        let visible = true;
        let raf = 0;
        let last = 0;
        let timeMs = 0;
        const tick = (now: number) => {
            raf = 0;
            if (!visible || document.hidden) {
                last = 0;
                return;
            }
            if (!reduced.matches && last && now - last < 1000 / 30) {
                raf = requestAnimationFrame(tick);
                return;
            }
            const delta = last ? now - last : 0;
            last = now;
            if (!reduced.matches) timeMs += delta;
            const elapsed = now - transition.current.started;
            const n = target.current ? 0 : 1;
            const g = target.current ? 1 : 0.45;
            const next = {
                night:
                    transition.current.night +
                    (n - transition.current.night) *
                        (reduced.matches ? 1 : fastOutSlowIn(elapsed / ANDROID_TIMING.background)),
                glow:
                    transition.current.glow +
                    (g - transition.current.glow) *
                        (reduced.matches ? 1 : fastOutSlowIn(elapsed / ANDROID_TIMING.glow)),
                timeMs,
            };
            ambient?.draw({
                iMode: next.night,
                iTime: (next.timeMs % ANDROID_TIMING.backgroundLoop) / 40,
            });
            values.current = next;
            setFrame(next);
            if (!reduced.matches) raf = requestAnimationFrame(tick);
        };
        const start = () => {
            if (document.hidden) {
                cancelAnimationFrame(raf);
                raf = 0;
                last = 0;
                return;
            }
            if (!raf) raf = requestAnimationFrame(tick);
        };
        restart.current = start;
        const observer = new IntersectionObserver(([entry]) => {
            if (!entry) return;
            visible = entry.isIntersecting;
            if (visible) start();
        });
        observer.observe(stage.current!);
        document.addEventListener("visibilitychange", start);
        reduced.addEventListener("change", start);
        start();
        return () => {
            cancelAnimationFrame(raf);
            observer.disconnect();
            window.removeEventListener("resize", resizeAmbient);
            ambient?.dispose();
            document.removeEventListener("visibilitychange", start);
            reduced.removeEventListener("change", start);
        };
    }, []);

    return (
        <div className="live-demo" ref={stage}>
            <div className="page-ambient" data-mode={enabled ? "on" : "off"} aria-hidden="true">
                <canvas ref={ambientCanvas} />
            </div>
            <div className="device-frame">
                <div className="device-viewport">
                    <AppScreen enabled={enabled} {...frame} clock={clock} onToggle={toggle} />
                </div>
            </div>
        </div>
    );
}
