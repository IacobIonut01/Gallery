# ReFra brand assets

Derived from the app sources in this repository. Regenerate by copying the
sources again — do not redraw or recolor.

| File | Source | Derivation |
| --- | --- | --- |
| `refra-mark.svg` | `app/commonApp/src/androidMain/res/drawable/ic_launcher_foreground.xml` | Mechanical VectorDrawable → SVG. The outer adaptive-icon group (scale 0.71076924, translate 62.47/61.05) is launcher safe-zone padding and is stripped; the `viewBox="74 74 284 284"` crops around the inner artwork. Clip paths, path data, fills, and even-odd rules are preserved exactly. |
| `app-icon.png` | `app/androidApp/src/main/ic_launcher-playstore.png` | Byte copy of the canonical 512×512 Play Store icon (mark on `#E5E5E8`). |
| `social-preview.png` | `screenshots/preview.png` (README banner) | Banner scaled to 1160px wide and centered on a 1200×630 `#E5E5E8` canvas. |
| `../favicon.svg` | same as `refra-mark.svg` | Same file served as the site icon. |
| `../favicon.png` | `ic_launcher-playstore.png` | 32×32 LANCZOS downscale. |
| `../apple-touch-icon.png` | `ic_launcher-playstore.png` | 180×180 LANCZOS downscale. |

## Palette

Mark fills (from the launcher foreground):

- Blue `#488DE9`
- Orange `#FE9921`
- Sky `#B2CFF7`
- Apricot `#FFD4A2`

Icon background `#E5E5E8` (`ic_launcher_background`). App theme seed
`#247EE0`. See `src/styles/brand.css` and `website/DESIGN.md` for how these map
onto the site's semantic tokens.

The alternate `mono` and `gallery` launcher foregrounds are product variants,
not the default brand — the canonical mark above is the default.

## Service icons

`src/components/ServiceIcon.astro` inlines the canonical monochrome Telegram
and Crowdin paths, vendored from Simple Icons (CC0,
https://github.com/simple-icons/simple-icons/blob/develop/LICENSE;
icon sources https://cdn.simpleicons.org/telegram and
https://cdn.simpleicons.org/crowdin). They render `currentColor` — the site
tints them, never recolors the path data.
