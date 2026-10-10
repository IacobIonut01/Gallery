---
name: ReFra docs
description: Documentation site for ReFra, the open-source Android gallery — the app's own brand applied to the web.
colors:
  brand-blue: "#488DE9"
  brand-orange: "#FE9921"
  brand-sky: "#B2CFF7"
  brand-apricot: "#FFD4A2"
  brand-icon-background: "#E5E5E8"
  brand-seed: "#247EE0"
  brand-ink: "#001C3B"
  primary: "#005EB1"
  on-primary: "#FFFFFF"
  primary-container: "#D5E3FF"
  on-primary-container: "#001C3B"
  surface: "#F9F9FF"
  on-surface: "#191C20"
  on-surface-variant: "#43474E"
  outline-variant: "#C4C6CF"
typography:
  display:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "6rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.03em"
  display-compact:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "3.8rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.03em"
  wordmark:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.15rem"
    fontWeight: 700
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 700
    letterSpacing: "-0.01em"
  body:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.6
  headline-tagline:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.625rem"
    fontWeight: 500
  headline-tagline-compact:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 500
  lede:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 400
  card-title:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.05rem"
    fontWeight: 700
  title:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "1.3rem"
    fontWeight: 700
  label:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "0.92rem"
    fontWeight: 600
  caption:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "0.88rem"
    fontWeight: 400
  fine:
    fontFamily: "'Google Sans', 'Google Sans Text', 'Roboto', 'Noto Sans', system-ui, sans-serif"
    fontSize: "0.8rem"
    fontWeight: 400
rounded:
  s: "8px"
  control: "10px"
  m: "14px"
  card: "16px"
  l: "24px"
  xl: "32px"
  pill: "999px"
spacing:
  gutter: "24px"
  section: "3.5rem"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "999px"
    padding: "10px 22px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    rounded: "999px"
    padding: "10px 22px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.m}"
    padding: "20px 22px"
---

# Design System: ReFra docs

## Overview

The docs site wears the app's own identity: the canonical blue/orange
refraction mark, the README/store banner palette read as soft floating
blurred light, the app's static default theme colors, and Google Sans — the
same family the app bundles. The site is for **reading** the product (guides,
feature pages that render the app live) and **operating** it (downloading
real APKs, choosing variants). Chrome stays quiet; brand color is earned by
the mark, drifting blurred bulbs in the brand palette, and primary actions —
never decoration on arbitrary cards.

## Colors

Two layers, both sourced from the repo:

- **Brand artwork palette** (`src/styles/brand.css`, `--brand-*`): the mark's
  four fills — blue `#488DE9`, orange `#FE9921`, sky `#B2CFF7`, apricot
  `#FFD4A2` — plus icon background `#E5E5E8`, theme seed `#247EE0`, and ink
  `#001C3B`. These are for brand surfaces only; never small text, never
  white-on-orange. The banner's literal diagonal band is *not* reproduced on
  the site — its colors appear as soft blurred bulbs (`BrandGlow` in the hero,
  a small trailing wash in `CommunityLinks`).
- **Semantic roles** (`src/styles/tokens.css`, generated from seed `#247EE0`,
  with primary roles overridden to the app's static defaults in `brand.css`):
  light primary `#005EB1` / dark and AMOLED `#A7C8FF`, with paired
  `on-primary`, `primary-container`, `on-primary-container`, and
  `surface-tint`. All UI text and controls use these paired roles, which keep
  contrast across light, dark, and AMOLED.

## Typography

Google Sans (400/500/600/700, self-hosted woff2 in `public/fonts/`, converted
from `app/core/ui` compose resources) is the only display and body voice.
Display is the `ReFra` wordmark size (6rem / 3.8rem compact, weight 700,
tracking -0.03em). Tracking never exceeds -0.03em; measure stays ≤68ch.

## Layout

Breakpoints: <720 compact, ≥720 medium, ≥1280 expanded; content max 84rem,
reading column 68ch, gutter 24px. The hero pairs the wordmark + README
tagline with a live app capture, lit by floating blurred brand bulbs
(slow transform-only drift; parked under reduced motion, removed under
reduced transparency). Download uses a two-column work area (form / receipt)
collapsing to one column under 960px.

## Elevation & Depth

Flat, tonal layering. Elevation is declared once — a 1px `--md-outline-variant`
border or a soft shadow, never both stacked. The sticky header is translucent
glass (`backdrop-filter`, opaque fallback under reduced transparency).

## Shapes

Radius scale 8/14/16/24/32 plus `pill` (999px). Cards and grouped controls
use 16px; pills are reserved for buttons, chips, and small controls. The app
icon keeps its own rounded square (16px corners at site sizes).

## Components

- `BrandMark` — the canonical SVG mark; decorative by default, sized per
  context. Not the alternate `mono`/`gallery` launcher variants.
- Buttons — pill, 44px minimum height, primary/outline/tonal on paired roles.
- `CommunityLinks` — surface-container-low cards with a monochrome
  `ServiceIcon` (vendored Simple Icons glyphs), a small blurred blue/orange
  wash at the trailing edge, and a quiet arrow nudge on hover.
- `ServiceIcon` — canonical Telegram/Crowdin paths, `currentColor`, tinted
  from `--md-on-surface-variant` to `--md-primary` on hover.
- `VariantPicker` — direct-download form + receipt; primary-container receipt,
  honest unavailable/error states, no synthesized URLs.

## Motion & Atmosphere

User-pinned preference: blur, smooth animation and motion, subtle gradients,
and restrained brand colors. Decorative floating motion lives only in the
hero (`BrandGlow`, three slow transform-only drifts); controls answer with
short transitions (~140–200ms) on color and translate; everything respects
reduced motion (glow parks, transitions collapse) and reduced transparency
(glow removed). No page-load choreography, no scroll hijacking.

## Do's and Don'ts

- Do use the brand palette for artwork surfaces; use `--md-*` roles for text
  and controls.
- Do keep the brand light motif to the hero glow and the community-card wash —
  it is ambient light, not page background or hard bands.
- Don't put orange or sky behind small text; brand-color surfaces are
  decorative only and text stays on md roles.
- Don't redraw or recolor the mark; derive assets from the launcher sources
  (see `public/brand/README.md`).
