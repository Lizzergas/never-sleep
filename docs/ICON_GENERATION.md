# Icon & Asset Generation Guide (Never Sleep)

This document captures the process, prompts, tools, and learnings from generating the custom launcher icon, adaptive icon assets, tile icons, widget icons, and Play Store assets using Grok's image tools.

Goal: Produce professional, on-brand icons that match the app's dark cosmic shader theme (deep #0D0D1A / #1A1A2E with purple/cyan accents) quickly.

## Tools Used

- `image_gen` — Primary tool for creating new 1:1 square images from text prompts. Returns high-res output (usually 1024x1024).
- `image_edit` — For refining existing generated images (e.g., isolating foreground/symbol on transparent background).
- Terminal (macOS built-ins):
  - `sips` — Fast image conversion and resizing (no external tools needed).
  - `cp`, `mkdir`, `file`, `ls` for management.
- Android resource structure:
  - `mipmap-*` for launcher icons (legacy + adaptive)
  - `drawable/` for other icons (tile, widget, shortcuts)
  - `mipmap-anydpi-v26/` for adaptive icon XML descriptors.

**Important**: The image tools live in the Grok session environment. Generated files land in a session `images/` folder (e.g. `~/.grok/sessions/.../images/`). Always copy them into the project immediately.

## Recommended Workflow (Fastest Path)

1. **Prepare assets folder**
   ```bash
   mkdir -p assets/icons assets/play-store
   ```

2. **Generate master icon(s) with image_gen**
   - Use 1:1 aspect ratio.
   - Be extremely specific in the prompt (see examples below).
   - Generate 2–4 variants and pick the best.

3. **Copy + convert to PNG**
   ```bash
   cp /path/to/session/images/XX.jpg assets/icons/never-sleep-master.jpg
   sips -s format png assets/icons/never-sleep-master.jpg --out assets/icons/never-sleep-master.png
   ```

4. **Resize for different uses** (using `sips -Z`)
   - Launcher (mipmap):
     - mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
   - Play Store listing: 512
   - Tile / Widget / Shortcuts: 192 or 256 is usually plenty (they render at 24–48dp)

   Example batch:
   ```bash
   for size in 48 72 96 144 192 256 512; do
     # ... create appropriately named outputs
     sips -Z $size assets/icons/never-sleep-master.png --out output-$size.png
   done
   ```

5. **Place in correct resource folders**
   - Launcher icons → `app/androidApp/src/main/res/mipmap-*/ic_launcher*.png`
   - Adaptive descriptor → `mipmap-anydpi-v26/ic_launcher.xml` (and round)
   - Background → `drawable/ic_launcher_background.xml` (simple solid color)
   - Foreground (for adaptive) → `drawable/ic_launcher_foreground.png`
   - State icons (moon/sun) → `drawable/ic_moon.png` + `drawable/ic_sun.png`
   - High-res master → `drawable/ic_app.png`
   - Tile/widget/shortcut masters → keep in `assets/`

6. **Update adaptive icon XML**
   ```xml
   <adaptive-icon ...>
       <background android:drawable="@drawable/ic_launcher_background" />
       <foreground android:drawable="@drawable/ic_launcher_foreground" />
   </adaptive-icon>
   ```

7. **Clean up old defaults**
   - Remove or rename old default vectors (e.g. the green grid + robot) so new assets take precedence.
   - Delete conflicting `.bak` or old XMLs from `res/` folders.

8. **Rebuild and test**
   ```bash
   ./gradlew :app:androidApp:clean :app:androidApp:assembleDebug
   ```
   - Install on emulator + physical device.
   - Check different DPIs, light/dark, round masks, widget, QS tile.
   - Verify Play Store 512px asset looks good at listing size.

## Best Prompt Patterns (What Worked Well)

### Launcher / Main App Icon
```
High quality modern minimalist app icon for "Never Sleep". Centered elegant glowing crescent moon with soft purple-cyan neon glow. Deep dark space background in shades of #0D0D1A to #1A1A2E. Premium flat design with gentle depth, perfectly centered circular composition, high contrast, clean lines, no text, app store quality, 1024x1024 square.
```

Key ingredients that produced good results:
- "High quality modern minimalist app icon"
- Brand colors explicitly (`#0D0D1A`, purple/cyan glow)
- "glowing crescent moon" or "moon + subtle sun"
- "centered", "circular composition"
- "no text"
- "premium", "clean", "high contrast"
- "1024x1024 square"
- Reference to app mood ("cosmic dark", "matching the shader")

### Tile & Widget Icons (Stateful Moon/Sun)
```
High quality minimalist Android icon for Quick Settings tile and home widget, active "Never Sleep" state. Elegant glowing crescent moon, soft purple-cyan neon glow on deep dark cosmic background, centered, high contrast, premium clean design, no text, 1024x1024.
```

For the opposite state:
```
... normal/inactive state. Stylized bright sun with gentle rays, matching cosmic premium style ... centered, high contrast, simple and recognizable at small sizes ...
```

Tips for small icons:
- Emphasize "high contrast", "works at small sizes", "clean".
- Avoid too much fine detail (stars/grain can disappear).
- Generate separate masters for moon vs sun so they feel intentionally paired.

### Foreground Isolation (for better adaptive icons)
Use `image_edit` on a master:
```
Extract the central glowing crescent moon icon and make the background fully transparent. Keep only the moon symbol with its soft glow and subtle stars, centered nicely on transparent. High quality, preserve the purple cyan glow, suitable as Android adaptive icon foreground.
```

Note: Alpha channel support in edits can be inconsistent. You may need to fall back to using the full designed icon as foreground (the system mask + your dark background still looks good).

### Play Store / Promo Assets
Just resize the master to 512:
```bash
sips -Z 512 ... --out assets/play-store/icon-512.png
```

You can also generate wider "feature graphic" (1024x500) with a separate prompt if needed.

## What We Learned / Gotchas

- **Output format**: The generator frequently returns JPEG even when you ask for PNG paths. Always run `sips -s format png` as the first post-processing step.
- **Session paths are long and temporary**: Immediately `cp` generated files into `assets/icons/` inside the project.
- **Adaptive icons are special**:
  - Background should be a simple solid (or very subtle) color/vector matching the theme. Complex backgrounds fight the mask.
  - Foreground benefits from transparency around the main symbol.
  - Providing high-res PNG foreground + solid background works reliably.
  - Legacy `mipmap-*` PNGs are still important for older devices and round icons.
- **Resizing quality**: `sips -Z` (fit within size) produces good results for icons. It preserves aspect and is fast/native on macOS.
- **Stateful icons (tile + widget)**: Using the same visual language as the launcher creates a cohesive brand. Raster PNGs at 192–256px look much richer than the old simple vectors.
- **Widget layout**: The current 1x1 widget uses a 48dp ImageView on a dark background. Icons with their own glow read well; very busy backgrounds inside the icon can fight the widget bg.
- **Tile icons**: Quick Settings tiles are small. High contrast + clear silhouette is more important than fine detail.
- **Shortcuts**: If you later add `ShortcutManager` (static or dynamic app shortcuts on long-press), use the same generation process. Typical size ~96–128px. Name them `ic_shortcut_*.png`.
- **Reusability**: Keep the 1024px masters + the generation prompts in `assets/` or this doc. Future rebrands or density additions become trivial.
- **Testing is mandatory**: Different launchers apply different masks (circle, squircle, rounded square). Always test on a real device.
- **Play policy / icons**: Use the custom icon everywhere (launcher, Play listing, shortcuts). Avoid anything that looks like a stock Android icon.
- **Vector vs Raster**: Vectors (the old `ic_moon.xml`) are tiny and scale perfectly but look flat. Generated raster icons give the "premium" feel the user liked. For tile/widget we switched to PNG successfully.

## File Locations Reference

| Purpose                  | Location                                      | Sizes          |
|--------------------------|-----------------------------------------------|----------------|
| Launcher (legacy)        | `mipmap-*/ic_launcher.png` + `_round.png`     | 48–192         |
| Adaptive descriptor      | `mipmap-anydpi-v26/ic_launcher*.xml`          | -              |
| Adaptive bg              | `drawable/ic_launcher_background.xml`         | -              |
| Adaptive fg              | `drawable/ic_launcher_foreground.png`         | ~1024 or 432+  |
| Master / Play 512        | `assets/icons/`, `assets/play-store/`         | 1024, 512      |
| Tile / Widget / Moon-Sun | `drawable/ic_moon.png`, `drawable/ic_sun.png` | 192–256        |
| Other (ic_app, tile PNGs)| `drawable/ic_app.png`, `ic_tile.png`, etc.    | 1024           |

## Quick Commands Cheatsheet

```bash
# After image_gen / image_edit
cp /long/session/path/9.jpg assets/icons/moon-tile.jpg
sips -s format png ... --out assets/icons/moon-tile.png

# Launcher densities
sips -Z 48  ... --out mipmap-mdpi/ic_launcher.png
# (repeat for 72,96,144,192 + round variants)

# Tile/widget size
sips -Z 192 assets/icons/moon-master.png --out drawable/ic_moon.png
```

## Future Improvements

- Add a small Gradle task or script to automate resizing once masters exist.
- Maintain a "brand kit" folder with all masters + source prompts.
- For very small UI elements, also generate simplified 1-bit or monochrome versions.
- Consider generating a "shortcut" specific variant with tighter framing.

This process let us go from "default robot icon" to cohesive custom cosmic moon branding in one focused session.

Next time you need icons for a new feature or rebrand, start here.

## Assets Generated in This Session (for reference)

- Launcher: `assets/icons/never-sleep-icon-master.png` + all mipmap sizes + adaptive foreground
- Tile active (moon): `assets/icons/never-sleep-moon-tile.png` → `drawable/ic_moon.png`
- Tile/widget inactive (sun): `assets/icons/never-sleep-sun-tile.png` → `drawable/ic_sun.png`
- Shortcut example: `assets/icons/never-sleep-shortcut.png` → `drawable/ic_shortcut_never_sleep.png`
- Play Store: `assets/play-store/icon-512.png`

Legacy vectors were moved to `assets/icons/legacy-vectors/`.
```

Now update AGENTS.md to reference the new doc.