#!/usr/bin/env node
/**
 * gen-theme.mjs — generate src/styles/tokens.css from the ReFra app seed color.
 *
 * The app builds its Material 3 theme from the HCT tonal-spot scheme seeded with
 * #247EE0 (see DOCS_SITE_PLAN.md §2, Theme row). The site uses the same seed and
 * the same tonal-spot formula so docs chrome matches the app.
 *
 * Implementation note: @material/material-color-utilities 0.4.0 ships ESM files
 * with a broken extensionless import inside dynamiccolor/color_spec_2025.js, so
 * this script builds the tonal palettes directly (HCT hue/chroma per the
 * tonal-spot formula) and assigns the standard Material 3 tone steps, which is
 * what SchemeTonalSpot produces.
 *
 * Run: npm run theme
 * The emitted CSS is checked in; `astro build` does not need the generator.
 */
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

// The package's exports map only exposes "." (whose index pulls in a broken
// extensionless import), so load the leaf modules by file URL — subpath
// resolution rules do not apply to direct file URLs.
const PKG = join(
  dirname(fileURLToPath(import.meta.url)),
  '..',
  'node_modules',
  '@material',
  'material-color-utilities',
);
const load = (rel) => import(pathToFileURL(join(PKG, rel)).href);
const { TonalPalette } = await load('palettes/tonal_palette.js');
const { Hct } = await load('hct/hct.js');
const { argbFromHex, hexFromArgb } = await load('utils/string_utils.js');

const SEED = '#247EE0';
const out = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'styles', 'tokens.css');

// Tonal-spot palette recipe (Material 3 spec, matches SchemeTonalSpot):
//   primary = seed hue/chroma 36 · secondary = seed hue/16
//   tertiary = seed hue+60/24 · neutral = seed hue/6 · neutralVariant = seed hue/8
//   error = hue 25 / chroma 84
function palettes() {
  const seed = Hct.fromInt(argbFromHex(SEED));
  return {
    primary: TonalPalette.fromHueAndChroma(seed.internalHue, 36),
    secondary: TonalPalette.fromHueAndChroma(seed.internalHue, 16),
    tertiary: TonalPalette.fromHueAndChroma(seed.internalHue + 60, 24),
    neutral: TonalPalette.fromHueAndChroma(seed.internalHue, 6),
    neutralVariant: TonalPalette.fromHueAndChroma(seed.internalHue, 8),
    error: TonalPalette.fromHueAndChroma(25, 84),
  };
}

// Material 3 role -> [palette, lightTone, darkTone]
const ROLE_TONES = {
  'primary': ['primary', 40, 80],
  'on-primary': ['primary', 100, 20],
  'primary-container': ['primary', 90, 30],
  'on-primary-container': ['primary', 10, 90],
  'secondary': ['secondary', 40, 80],
  'on-secondary': ['secondary', 100, 20],
  'secondary-container': ['secondary', 90, 30],
  'on-secondary-container': ['secondary', 10, 90],
  'tertiary': ['tertiary', 40, 80],
  'on-tertiary': ['tertiary', 100, 20],
  'tertiary-container': ['tertiary', 90, 30],
  'on-tertiary-container': ['tertiary', 10, 90],
  'error': ['error', 40, 80],
  'on-error': ['error', 100, 20],
  'error-container': ['error', 90, 30],
  'on-error-container': ['error', 10, 90],
  'surface': ['neutral', 98, 6],
  'surface-dim': ['neutral', 87, 6],
  'surface-bright': ['neutral', 98, 24],
  'surface-container-lowest': ['neutral', 100, 4],
  'surface-container-low': ['neutral', 96, 10],
  'surface-container': ['neutral', 94, 12],
  'surface-container-high': ['neutral', 92, 17],
  'surface-container-highest': ['neutral', 90, 22],
  'surface-variant': ['neutralVariant', 90, 30],
  'on-surface': ['neutral', 10, 90],
  'on-surface-variant': ['neutralVariant', 30, 80],
  'outline': ['neutralVariant', 50, 60],
  'outline-variant': ['neutralVariant', 80, 30],
  'inverse-surface': ['neutral', 20, 90],
  'inverse-on-surface': ['neutral', 95, 20],
  'inverse-primary': ['primary', 80, 40],
  'scrim': ['neutral', 0, 0],
  'shadow': ['neutral', 0, 0],
  'surface-tint': ['primary', 40, 80],
};

function roleVars(isDark, indent = '  ') {
  const p = palettes();
  return Object.entries(ROLE_TONES)
    .map(([name, [pal, lt, dt]]) => {
      const argb = p[pal].tone(isDark ? dt : lt);
      return `${indent}--md-${name}: ${hexFromArgb(argb)};`;
    })
    .join('\n');
}

const prism = (isDark, indent = '  ') => {
  const p = palettes();
  const t = isDark ? [40, 50, 45] : [70, 75, 65];
  return [
    `${indent}--prism-primary: ${hexFromArgb(p.primary.tone(t[0]))};`,
    `${indent}--prism-secondary: ${hexFromArgb(p.secondary.tone(t[1]))};`,
    `${indent}--prism-tertiary: ${hexFromArgb(p.tertiary.tone(t[2]))};`,
  ].join('\n');
};

// Amoled: the dark scheme with true-black surfaces, matching the app's
// AMOLED preference (black background variant of the dark theme).
const amoledOverrides = [
  '  --md-surface: #000000;',
  '  --md-surface-dim: #000000;',
  '  --md-surface-bright: #000000;',
  '  --md-surface-container-lowest: #000000;',
  '  --md-surface-container-low: #0a0a0c;',
  '  --md-surface-container: #101014;',
  '  --md-surface-container-high: #16161b;',
  '  --md-surface-container-highest: #1d1d23;',
  '  --md-surface-variant: #101014;',
].join('\n');

const css = `/*
 * GENERATED by scripts/gen-theme.mjs — do not edit by hand.
 * Source of truth: ReFra theme seed ${SEED} through the Material tonal-spot
 * scheme (same algorithm the app applies via material-kolor).
 */

:root {
  color-scheme: light;
${roleVars(false)}
${prism(false)}
}

/* Explicit dark selection */
:root[data-theme='dark'] {
  color-scheme: dark;
${roleVars(true)}
${prism(true)}
}

/* AMOLED: dark roles over true-black surfaces */
:root[data-theme='amoled'] {
  color-scheme: dark;
${roleVars(true)}
${amoledOverrides}
${prism(true)}
}

/* auto / unset: follow the OS */
@media (prefers-color-scheme: dark) {
  :root:not([data-theme='light']):not([data-theme='dark']):not([data-theme='amoled']) {
    color-scheme: dark;
${roleVars(true, '    ')}
${prism(true, '    ')}
  }
}
`;

mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, css);
console.log(`wrote ${out} from seed ${SEED}`);
