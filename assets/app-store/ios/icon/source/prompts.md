# Image generation provenance

The icon artwork was generated with OpenAI's built-in image generation tool,
then resized and prepared deterministically by
`scripts/generate-ios-store-assets.py`.

## Default appearance

> Use case: logo-brand. Asset type: iOS and iPadOS App Store app icon master,
> 1024 × 1024 square. Create a refined, memorable Never Sleep app icon based
> on the supplied brand references. Preserve the core identity of a luminous
> crescent moon with cyan-to-violet illumination, but simplify it into one
> bold, centered celestial mark that remains recognizable at very small icon
> sizes. Do not reproduce the circular in-app button or a screenshot; turn the
> crescent into an independent brand symbol. Use a full-bleed opaque midnight
> navy to deep violet atmospheric background, subtle soft fog depth, and only
> a few deliberately placed star points. Make the crescent's outer edges crisp
> with gentle internal luminosity. Keep the canvas square and unmasked, with a
> generous safe area and the mark occupying about 58 percent of the canvas.
> The mood is calm, nocturnal, awake, and quietly alive. Use midnight, indigo,
> violet, and a small cyan highlight. No alpha, rounded-corner mask, text,
> border, device frame, UI, excessive glow, photorealism, or watermark.

References: the shipping Android icon and the verified iOS active screen.

## Dark appearance

> Edit the supplied Never Sleep icon into its iOS dark-appearance variant.
> Preserve the exact crescent silhouette, scale, optical position, star layout,
> square composition, and full-bleed unmasked canvas. Make the backdrop deeper
> near-black midnight navy with restrained violet atmosphere. Reduce the
> overall glow and star brightness by roughly 25 percent while keeping the
> cyan inner rim and violet-magenta outer rim crisp enough to read at 32 px.
> Keep it premium and calm. The image must remain fully opaque, with no
> transparency, rounded-corner mask, border, text, letters, UI, new objects,
> or watermark.

Reference: the generated default appearance.

## Tinted appearance

The tinted appearance is a deterministic monochrome rendering of the default
master. This keeps the silhouette identical and lets iOS apply the user's
chosen tint predictably.
