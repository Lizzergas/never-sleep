import { useState } from "react";

type HelloIslandProps = {
    apiPath: string;
};

export function HelloIsland({ apiPath }: HelloIslandProps) {
    const [count, setCount] = useState(0);

    return (
        <div className="island" aria-label="Interactive React island">
            <div>
                <span className="eyebrow">React island</span>
                <p>This component hydrates on the client while the page stays Astro-first.</p>
            </div>
            <button type="button" onClick={() => setCount((value) => value + 1)}>
                Clicked {count} times
            </button>
            <code>{apiPath}</code>
        </div>
    );
}
