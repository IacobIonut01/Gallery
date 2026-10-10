#!/usr/bin/env node
/**
 * merge-capture-out.mjs — union-merge a pulled docs-out tree into the host's.
 *
 * Usage: node merge-capture-out.mjs <incomingDir> <hostDir>
 *
 * Both trees may carry a top-level docs-manifest.json plus per-scene fragments
 * under manifest/. Merge rules (one entry map per tree first):
 *
 *   - Scenes union by id. For a scene present in BOTH trees the incoming
 *     metadata wins and host capture keys are pruned when the incoming scene
 *     no longer declares that state ("default" is always kept). Incoming
 *     capture records win the same (theme\0device\0state) key; host-only keys
 *     survive, so a dark-only rerun on another emulator keeps host light and
 *     tablet frames.
 *   - Media files are copied only after the manifests have been parsed, so a
 *     malformed pull never silently widens coverage.
 *   - The merged top manifest and every per-scene fragment are written
 *     atomically; the incoming JSON never lands verbatim on top of the merge.
 */
import {
  cpSync,
  existsSync,
  mkdirSync,
  renameSync,
  writeFileSync,
} from 'node:fs';
import { join } from 'node:path';
import { MEDIA_EXT, parseKotlinScenes, readDocsOutManifest, walk } from './lib/docs.mjs';
import { dirname } from 'node:path';

const META_FIELDS = ['title', 'pathTrace', 'states', 'presentation', 'widthDp', 'heightDp'];

/** Union two entry maps (id -> {meta fields, captures: Map}). */
export function mergeEntryMaps(hostEntries, incomingEntries, registeredScenes = new Map()) {
  const merged = new Map();
  for (const [id, e] of hostEntries) {
    merged.set(id, { ...e, captures: new Map(e.captures) });
  }
  for (const [id, inc] of incomingEntries) {
    const entry = merged.get(id) ?? { id, captures: new Map(), sources: [] };
    for (const field of META_FIELDS) {
      if (inc[field] !== undefined) entry[field] = inc[field];
    }
    // The re-recorded scene declares which states still exist; drop host
    // captures whose state is gone ("default" is always retained). An
    // explicit empty list still prunes — only an absent field means
    // "unknown, keep everything".
    // A device pull also includes untouched fragments from earlier APKs.
    // Those fragments predate host-recorded clips. Preserve recorded states
    // still declared by the current registry, even in an older state list.
    const recorded = registeredScenes.get(id)?.recordedStates ?? [];
    const declared = new Set([...(inc.states ?? []), ...recorded]);
    if (Array.isArray(inc.states)) {
      entry.states = [...declared];
      for (const [key, cap] of [...entry.captures]) {
        const state = cap.state || 'default';
        if (state !== 'default' && !declared.has(state)) entry.captures.delete(key);
      }
    }
    for (const [key, cap] of inc.captures) entry.captures.set(key, cap);
    merged.set(id, entry);
  }
  return merged;
}

function sceneJson(id, entry) {
  const scene = { id };
  for (const field of META_FIELDS) {
    if (entry[field] !== undefined) scene[field] = entry[field];
  }
  scene.captures = [...entry.captures.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([, cap]) => cap);
  return scene;
}

function writeAtomic(file, content) {
  const tmp = `${file}.tmp-${process.pid}`;
  writeFileSync(tmp, content);
  renameSync(tmp, file);
}

export function mergeDocsOut(incomingDir, hostDir, registeredScenes = new Map()) {
  // Parse BOTH trees before touching any media file.
  const hostEntries = readDocsOutManifest(hostDir).entries;
  const incomingEntries = readDocsOutManifest(incomingDir).entries;
  const merged = mergeEntryMaps(hostEntries, incomingEntries, registeredScenes);

  for (const file of walk(incomingDir, MEDIA_EXT)) {
    const dest = join(hostDir, file.slice(incomingDir.length + 1));
    mkdirSync(dirname(dest), { recursive: true });
    cpSync(file, dest);
  }

  mkdirSync(join(hostDir, 'manifest'), { recursive: true });
  const sorted = [...merged.entries()].sort(([a], [b]) => a.localeCompare(b));
  const manifest = {
    schema: 1,
    generatedAtMs: Date.now(),
    source: 'docs-capture',
    scenes: sorted.map(([id, entry]) => sceneJson(id, entry)),
  };
  for (const [id, entry] of sorted) {
    writeAtomic(
      join(hostDir, 'manifest', `${id}.json`),
      `${JSON.stringify(sceneJson(id, entry), null, 2)}\n`,
    );
  }
  writeAtomic(join(hostDir, 'docs-manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
  return merged.size;
}

const isMain = process.argv[1] && process.argv[1].endsWith('merge-capture-out.mjs');
if (isMain) {
  const [incomingDir, hostDir] = process.argv.slice(2);
  if (!incomingDir || !hostDir || !existsSync(incomingDir)) {
    console.error('usage: node merge-capture-out.mjs <incomingDir> <hostDir>');
    process.exit(2);
  }
  mkdirSync(hostDir, { recursive: true });
  const count = mergeDocsOut(incomingDir, hostDir, parseKotlinScenes());
  console.log(`merge-capture-out: merged ${count} scene(s) into ${hostDir}`);
}
