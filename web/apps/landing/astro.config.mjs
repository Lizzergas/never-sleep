import { defineConfig } from "astro/config";
import react from "@astrojs/react";

export default defineConfig({
    site: "https://neversleep.app",
    // Custom domain at repo root — not username.github.io/never-sleep
    base: "/",
    trailingSlash: "ignore",
    integrations: [react()],
    server: {
        host: "127.0.0.1",
        port: 5173,
    },
});
