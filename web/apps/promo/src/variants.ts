export const VIDEO_VARIANTS = [
    {
        id: "app-demo-screen-off",
        campaign: "screen_off_v1",
        opening: "Screen keeps turning off?",
        durationInFrames: 420,
        turnOff: true,
    },
    {
        id: "app-demo-one-tap",
        campaign: "one_tap_v1",
        opening: "Keep your screen on with one tap.",
        durationInFrames: 360,
        turnOff: false,
    },
    {
        id: "app-demo-stay-on-task",
        campaign: "stay_on_task_v1",
        opening: "Stay on task. Even between taps.",
        durationInFrames: 420,
        turnOff: true,
    },
] as const;
