# Design — Never Sleep

A locked design system for the public Never Sleep web surfaces. Every public
page reads this file before visual changes. Extend this system instead of
inventing a page-local theme.

## Genre

Atmospheric, but product-led: the Android utility is the visual subject. The
canvas is dark and quiet. The marketing homepage preserves the branded Paper
shader as a restrained atmospheric layer; legal and utility pages do not use
it. No surface uses glass or coloured shadow glows.

## Macrostructure family

- Marketing pages: Workbench — the real Normal/Never control leads the tour.
- App pages: not applicable; the shipping product is the native Android app.
- Content pages: Long Document — readable prose with a 65–72ch measure.

## Theme

- `--color-paper`: `oklch(14% 0.018 285)`
- `--color-paper-2`: `oklch(18% 0.022 285)`
- `--color-paper-3`: `oklch(22% 0.025 285)`
- `--color-ink`: `oklch(94% 0.012 285)`
- `--color-ink-2`: `oklch(82% 0.014 285)`
- `--color-muted`: `oklch(70% 0.014 285)`
- `--color-rule`: `oklch(34% 0.025 285)`
- `--color-accent`: `oklch(76% 0.15 72)`
- `--color-focus`: `oklch(82% 0.17 72)`
- `--color-state-never`: `oklch(70% 0.17 295)`

State roles are shared with the Android app:

- Normal = warm / sun / Android's system timeout.
- Never = cool / moon / screen stays awake until Normal is restored.

## Typography

- Display: Sora, weight 650, upright.
- Body: Inter, weight 400.
- Mono: ui-monospace fallback, weight 500.
- Display tracking: `-0.045em`.
- Type scale anchor: `--text-display = clamp(3rem, 10vw, 7rem)`.

## Spacing

A named 4-point scale lives in `apps/landing/src/styles/tokens.css`. Production
styles use named tokens rather than raw spacing values.

## Motion

- Easings: `--ease-out`, `--ease-in`, `--ease-in-out`.
- Page reveal: none.
- State control: transform/opacity only.
- Reduced motion: state changes remain visible; spatial motion is removed.

## Microinteractions stance

- Silent success: changing the Normal/Never label is sufficient feedback.
- Hover is a quiet colour or underline change, paired with keyboard focus.
- Focus has no delay and no animation.

## CTA voice

- Primary CTA: the official Google Play badge, unmodified.
- Secondary CTA: typographic link with a short underline.

## Per-page allowances

- Marketing pages may use the branded Paper shader and interactive CSS/WebGL
  app control.
- The homepage omits the site header because the hero already carries the
  product name and privacy link; legal and error pages retain compact navigation.
- Content pages use typography and rules only.
- No public page uses Play Store screenshot PNGs as landing imagery.

## What pages MUST share

- Wordmark and launcher icon.
- Sora display and Inter body faces.
- The Normal/Never semantic colour roles.
- Focus treatment, spacing rhythm, and official Play badge.

## What pages MAY differ on

- Marketing pages may use the Workbench control and state ledger.
- Legal and error pages remain quiet Long Document surfaces.
- Section rhythm may change to fit content density.

## Exports

### tokens.css

```css
:root {
    --color-paper: oklch(14% 0.018 285);
    --color-paper-2: oklch(18% 0.022 285);
    --color-ink: oklch(94% 0.012 285);
    --color-ink-2: oklch(82% 0.014 285);
    --color-rule: oklch(34% 0.025 285);
    --color-accent: oklch(76% 0.15 72);
    --color-accent-ink: oklch(16% 0.02 285);
    --color-focus: oklch(82% 0.17 72);
    --font-display: "Sora", sans-serif;
    --font-body: "Inter", sans-serif;
    --space-md: 1rem;
    --text-md: 1rem;
    --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
    --dur-short: 180ms;
    --radius-control: 0.5rem;
}
```

### Tailwind v4 `@theme`

```css
@theme {
    --color-paper: oklch(14% 0.018 285);
    --color-ink: oklch(94% 0.012 285);
    --color-accent: oklch(76% 0.15 72);
    --font-display: "Sora", sans-serif;
    --font-body: "Inter", sans-serif;
    --spacing-md: 1rem;
    --text-md: 1rem;
    --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
}
```

### DTCG `tokens.json`

```json
{
    "color": {
        "paper": { "$value": "oklch(14% 0.018 285)", "$type": "color" },
        "ink": { "$value": "oklch(94% 0.012 285)", "$type": "color" },
        "accent": { "$value": "oklch(76% 0.15 72)", "$type": "color" }
    },
    "font": {
        "display": { "$value": "Sora", "$type": "fontFamily" },
        "body": { "$value": "Inter", "$type": "fontFamily" }
    },
    "space": {
        "md": { "$value": "1rem", "$type": "dimension" }
    }
}
```

### shadcn/ui CSS variables

```css
:root {
    --background: 14% 0.018 285;
    --foreground: 94% 0.012 285;
    --primary: 76% 0.15 72;
    --primary-foreground: 16% 0.02 285;
    --muted: 22% 0.025 285;
    --muted-foreground: 70% 0.014 285;
    --border: 34% 0.025 285;
    --input: 34% 0.025 285;
    --ring: 82% 0.17 72;
    --radius: 0.5rem;
}
```
