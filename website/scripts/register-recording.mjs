#!/usr/bin/env node
/**
 * Register a real phone recording and its first-frame WebP poster.
 * Files must already exist at <scene>/<theme>/phone-<state>.{mp4,webp}
 * under docs-out. Pixel dimensions come from ffprobe, never CLI guesses.
 */
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { DOCS_OUT, THEME_IDS, parseKotlinScenes } from './lib/docs.mjs';
import { mergeDocsOut } from './merge-capture-out.mjs';

const [sceneId, state, theme = 'light', ...extra] = process.argv.slice(2);
if (!sceneId || !state || extra.length) {
  throw new Error('usage: node scripts/register-recording.mjs <scene> <recorded-state> [theme]');
}
if (![sceneId, state].every((value) => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(value)) ||
    !THEME_IDS.includes(theme)) {
  throw new Error('Invalid scene, state, or theme');
}
const scene = parseKotlinScenes().get(sceneId);
if (!scene || !scene.states.includes(state) || !scene.recordedStates?.includes(state)) {
  throw new Error(`${sceneId}/${state} must be declared as a recorded state in Kotlin`);
}

function probe(file) {
  if (!existsSync(file)) throw new Error(`Recording asset missing: ${file}`);
  const result = JSON.parse(execFileSync('ffprobe', [
    '-v', 'error', '-select_streams', 'v:0',
    '-show_entries', 'stream=width,height,codec_name:format=duration',
    '-of', 'json', file,
  ], { encoding: 'utf8' }));
  const stream = result.streams?.[0];
  if (!Number.isInteger(stream?.width) || stream.width <= 0 ||
      !Number.isInteger(stream?.height) || stream.height <= 0) {
    throw new Error(`No valid image dimensions in ${file}`);
  }
  return { ...stream, duration: Number(result.format?.duration) };
}

const prefix = `${sceneId}/${theme}/phone-${state}`;
const file = `${prefix}.mp4`;
const poster = `${prefix}.webp`;
const videoInfo = probe(join(DOCS_OUT, file));
const posterInfo = probe(join(DOCS_OUT, poster));
const widthDp = scene.widthDp ?? 412;
const heightDp = scene.heightDp ?? 915;
if (videoInfo.codec_name !== 'h264' || !(videoInfo.duration > 0)) {
  throw new Error('The recording must contain a nonempty H.264 video stream');
}
if (posterInfo.codec_name !== 'webp' ||
    posterInfo.width !== videoInfo.width || posterInfo.height !== videoInfo.height) {
  throw new Error('The WebP poster must have the same pixel dimensions as the recording');
}
if (Math.abs(videoInfo.width - Math.round(videoInfo.height * widthDp / heightDp)) > 2) {
  throw new Error(`Recording aspect does not match the scene's ${widthDp}×${heightDp} frame`);
}

const fragment = {
  id: scene.id,
  title: scene.title,
  pathTrace: scene.path,
  states: scene.states,
  presentation: scene.presentation ?? 'screen',
  widthDp,
  heightDp,
  captures: [{
    device: 'phone',
    state,
    theme,
    file,
    poster,
    width: videoInfo.width,
    height: videoInfo.height,
    widthDp,
    heightDp,
  }],
};
// Use the same union operation as device captures: other states, themes and
// devices survive, and generated manifests receive one coherent timestamp.
const incoming = mkdtempSync(join(tmpdir(), 'refra-recording-'));
try {
  mkdirSync(join(incoming, 'manifest'));
  writeFileSync(join(incoming, 'manifest', `${sceneId}.json`), JSON.stringify(fragment));
  mergeDocsOut(incoming, DOCS_OUT);
} finally {
  rmSync(incoming, { recursive: true, force: true });
}
console.log(`Registered ${sceneId}/${state}/${theme}: ${videoInfo.width}×${videoInfo.height}`);
