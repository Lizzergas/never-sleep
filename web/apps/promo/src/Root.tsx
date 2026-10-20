import { Composition } from "remotion";
import { Promo } from "./Promo";
export const RemotionRoot = () => (
    <Composition
        id="app-demo"
        component={Promo}
        durationInFrames={420}
        fps={30}
        width={1080}
        height={1920}
    />
);
