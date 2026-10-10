#!/usr/bin/env node
/**
 * Crop an Android screenrecord to the measured docs frame, preserving aspect.
 * Input JSON is exported by DocsCaptureActivity, including scene identity,
 * on-screen frame edges, and physical screen dimensions.
 */
import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { DOCS_OUT, THEME_IDS, parseKotlinScenes } from './lib/docs.mjs';

const [input, boundsFile, sceneId, state, theme = 'light', ...extra] = process.argv.slice(2);
if (!input || !boundsFile || !sceneId || !state || extra.length) {
  throw new Error('usage: node scripts/encode-recording.mjs <raw.mp4> <bounds.json> <scene> <state> [theme]');
}
const scene = parseKotlinScenes().get(sceneId);
if (!scene || !scene.recordedStates?.includes(state) || !THEME_IDS.includes(theme)) {
  throw new Error('Unknown recorded scene/state/theme');
}
const bounds = JSON.parse(readFileSync(boundsFile, 'utf8'));
if (bounds.scene !== sceneId || bounds.state !== state || bounds.theme !== theme ||
    bounds.widthDp !== 412 || bounds.heightDp !== 915) {
  throw new Error('Measured bounds do not belong to the requested phone recording');
}
for (const field of ['left', 'top', 'right', 'bottom', 'screenWidth', 'screenHeight']) {
  if (!Number.isInteger(bounds[field])) throw new Error(`Invalid bounds field: ${field}`);
}
if (bounds.left < 0 || bounds.top < 0 ||
    bounds.right <= bounds.left || bounds.bottom <= bounds.top ||
    bounds.right > bounds.screenWidth || bounds.bottom > bounds.screenHeight) {
  throw new Error('The docs frame is outside the physical screen');
}
if (Math.abs((bounds.right - bounds.left) -
    Math.round((bounds.bottom - bounds.top) * bounds.widthDp / bounds.heightDp)) > 2) {
  throw new Error('Measured frame aspect differs from the declared phone aspect');
}
const probe = JSON.parse(execFileSync('ffprobe', [
  '-v', 'error', '-select_streams', 'v:0',
  '-show_entries', 'stream=width,height', '-of', 'json', input,
], { encoding: 'utf8' })).streams?.[0];
if (!Number.isInteger(probe?.width) || probe.width <= 0 ||
    !Number.isInteger(probe?.height) || probe.height <= 0 ||
    Math.abs(probe.width - probe.height * bounds.screenWidth / bounds.screenHeight) > 1) {
  throw new Error('The recording is not an aspect-preserving capture of this physical screen');
}
const left = Math.round(bounds.left * probe.width / bounds.screenWidth);
const top = Math.round(bounds.top * probe.height / bounds.screenHeight);
const right = Math.round(bounds.right * probe.width / bounds.screenWidth);
const bottom = Math.round(bounds.bottom * probe.height / bounds.screenHeight);
const width = right - left;
const height = bottom - top;
// Two pixels per logical dp; both output dimensions are even for H.264.
const outputWidth = 824;
const outputHeight = 1830;
const prefix = join(DOCS_OUT, sceneId, theme, `phone-${state}`);
mkdirSync(dirname(prefix), { recursive: true });
execFileSync('ffmpeg', [
  '-y', '-v', 'error', '-i', input, '-an',
  '-vf', `crop=${width}:${height}:${left}:${top}:exact=1,scale=${outputWidth}:${outputHeight}:flags=lanczos,setsar=1`,
  '-r', '30', '-c:v', 'libx264', '-preset', 'medium', '-crf', '20',
  '-pix_fmt', 'yuv420p', '-movflags', '+faststart', `${prefix}.mp4`,
], { stdio: 'inherit' });
// cwebp is also used for still assets; the installed ffmpeg need not have
// the optional libwebp encoder. Extract the first decoded video frame once.
const posterDir = mkdtempSync(join(tmpdir(), 'refra-poster-'));
try {
  const png = join(posterDir, 'first-frame.png');
  execFileSync('ffmpeg', [
    '-y', '-v', 'error', '-i', `${prefix}.mp4`, '-frames:v', '1', png,
  ], { stdio: 'inherit' });
  execFileSync('cwebp', ['-quiet', '-q', '90', png, '-o', `${prefix}.webp`], {
    stdio: 'inherit',
  });
} finally {
  rmSync(posterDir, { recursive: true, force: true });
}
console.log(`Encoded ${sceneId}/${state}/${theme} from measured ${width}×${height} crop`);
