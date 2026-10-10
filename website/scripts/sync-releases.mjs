#!/usr/bin/env node
/**
 * sync-releases.mjs — capture the GitHub Releases snapshot the download page
 * renders. Run manually (`npm run releases:sync`); the site build itself never
 * touches the network. Output: registry/releases.json.
 */
import { writeFile } from 'node:fs/promises';
import { RELEASE_API, normalizeRelease, selectNightly } from '../src/lib/releases.mjs';

const options = {
  headers: { Accept: 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28' },
  signal: AbortSignal.timeout(20000),
};
const [stableResponse, listResponse] = await Promise.all([
  fetch(`${RELEASE_API}/latest`, options),
  fetch(`${RELEASE_API}?per_page=100`, options),
]);
if (!stableResponse.ok || !listResponse.ok)
  throw new Error(`Release metadata fetch failed: ${stableResponse.status}/${listResponse.status}`);
const stable = normalizeRelease(await stableResponse.json());
const nightly = selectNightly(await listResponse.json());
if (!stable || stable.prerelease)
  throw new Error('No valid stable APK release; keeping existing snapshot.');
const snapshot = { checkedAt: new Date().toISOString(), stable, nightly };
await writeFile(
  new URL('../registry/releases.json', import.meta.url),
  JSON.stringify(snapshot, null, 2) + '\n',
);
console.log(`Release snapshot: stable ${stable.tag}, nightly ${nightly?.tag || 'not found'}`);
