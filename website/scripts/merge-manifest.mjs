#!/usr/bin/env node
/**
 * merge-manifest.mjs — build-time merge of the docs scene registry.
 *
 * Reads:
 *   ../docs-out/docs-manifest.json (+ docs-out/manifest/*.json) — capture tier
 *   ../app/docs/showcase/.../SceneRegistry.kt                    — registry fallback
 *   public/live/index.html + docs-live.json                    — live tier
 *
 * Writes:
 *   registry/scenes.json        — checked-in, imported by pages (SceneIndex) and
 *                                 validated by scripts/check-registry.mjs
 *   public/docs-manifest.json   — runtime contract fetched by LiveEmbed
 *   public/captures/<scene>/<theme>/… — the referenced WebP/MP4 frames, copied so
 *                                 the static site serves them under the docs base
 *
 * Scenes registered in Kotlin but not yet captured land as fidelity "pending";
 * LiveEmbed renders its placeholder card for those. `assets` remains the
 * light-theme alias of `themes.light` for existing consumers.
 */
import { copyFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { collectScenes, DOCS_OUT, SCENE_REGISTRY_KT, WEBSITE_ROOT } from './lib/docs.mjs';

const { generatedAtMs, scenes } = collectScenes();
const rel = (p) => relative(WEBSITE_ROOT, p);

const slimAsset = ({ src, type, file, width, height, widthDp, heightDp, poster, posterFile }) => ({
  src,
  type,
  file,
  width,
  height,
  widthDp,
  heightDp,
  poster,
  posterFile,
});

const slimThemes = (themes) =>
  Object.fromEntries(
    Object.entries(themes).map(([theme, devices]) => [
      theme,
      Object.fromEntries(
        Object.entries(devices).map(([device, states]) => [
          device,
          Object.fromEntries(
            Object.entries(states).map(([state, a]) => [state, slimAsset(a)]),
          ),
        ]),
      ),
    ]),
  );

const sceneEntry = (s) => ({
  id: s.id,
  title: s.title,
  path: s.path,
  states: s.states,
  presentation: s.presentation,
  widthDp: s.widthDp,
  heightDp: s.heightDp,
  live: s.live,
  fidelity: s.fidelity,
  assets: Object.fromEntries(
    Object.entries(s.assets).map(([device, states]) => [
      device,
      Object.fromEntries(
        Object.entries(states).map(([state, a]) => [state, slimAsset(a)]),
      ),
    ]),
  ),
  themes: slimThemes(s.themes),
});

/* registry/scenes.json — page-facing shape */
const registry = {
  generatedAt: new Date().toISOString(),
  capturedAtMs: generatedAtMs || null,
  sources: [DOCS_OUT, SCENE_REGISTRY_KT].map(rel),
  sceneCount: scenes.size,
  scenes: Object.fromEntries(
    [...scenes.values()]
      .sort((a, b) => a.id.localeCompare(b.id))
      .map((s) => [s.id, sceneEntry(s)]),
  ),
};

/* public/docs-manifest.json — the LiveEmbed runtime contract (plan §7) */
const runtime = {
  generatedAtMs: generatedAtMs || Date.now(),
  scenes: Object.fromEntries([...scenes.values()].map((s) => [s.id, sceneEntry(s)])),
};

const registryPath = join(WEBSITE_ROOT, 'registry', 'scenes.json');
const manifestPath = join(WEBSITE_ROOT, 'public', 'docs-manifest.json');
mkdirSync(dirname(registryPath), { recursive: true });
writeFileSync(registryPath, `${JSON.stringify(registry, null, 2)}\n`);
mkdirSync(dirname(manifestPath), { recursive: true });
writeFileSync(manifestPath, `${JSON.stringify(runtime, null, 2)}\n`);

/* Copy the referenced frames into public/captures/ (gitignored — CI drops the
   docs-capture output here the same way). Dedupe by file across themes. */
let copied = 0;
let missing = 0;
const copiedFiles = new Set();
for (const scene of scenes.values()) {
  for (const devices of Object.values(scene.themes)) {
    for (const states of Object.values(devices)) {
      for (const asset of Object.values(states)) {
        for (const relFile of [asset.file, asset.posterFile].filter(Boolean)) {
          if (copiedFiles.has(relFile)) continue;
          copiedFiles.add(relFile);
          const from = join(DOCS_OUT, relFile);
          const to = join(WEBSITE_ROOT, 'public', 'captures', relFile);
          try {
            mkdirSync(dirname(to), { recursive: true });
            copyFileSync(from, to);
            copied += 1;
          } catch {
            missing += 1;
          }
        }
      }
    }
  }
}

const byFidelity = {};
for (const s of scenes.values()) byFidelity[s.fidelity] = (byFidelity[s.fidelity] ?? 0) + 1;
console.log(
  `merged ${scenes.size} scenes (${Object.entries(byFidelity)
    .map(([k, v]) => `${v} ${k}`)
    .join(', ')})`,
);
console.log(`wrote ${registryPath.replace(`${WEBSITE_ROOT}/`, '')}`);
console.log(`wrote ${manifestPath.replace(`${WEBSITE_ROOT}/`, '')}`);
console.log(`copied ${copied} capture frames to public/captures/`);
if (missing) {
  console.warn(`warning: ${missing} manifest frame(s) missing under ${DOCS_OUT} — run check:registry`);
}
