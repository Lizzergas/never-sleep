import { Composition } from "remotion";
import { Promo } from "./Promo";
import { VIDEO_VARIANTS } from "./variants";
export const RemotionRoot = () => (
    <>
        {VIDEO_VARIANTS.map(({ id, opening, durationInFrames, turnOff }) => (
            <Composition
                key={id}
                id={id}
                component={Promo}
                defaultProps={{ opening, turnOff }}
                durationInFrames={durationInFrames}
                fps={30}
                width={1080}
                height={1920}
            />
        ))}
    </>
);
