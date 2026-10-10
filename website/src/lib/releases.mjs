/**
 * Release data contract for the download page.
 *
 * `sync-releases.mjs` writes a normalized snapshot to registry/releases.json;
 * VariantPicker renders that snapshot server-side and refreshes it live
 * against the GitHub Releases API at runtime.
 *
 * APK names follow the release grammar
 *   ReFra-<version>-<versionCode>[-offline]-(NoML|WithML)-<abi>-release.apk
 * and a download URL is only ever taken from the asset's own
 * `browser_download_url` after the expected path check — never synthesized.
 */
export const RELEASE_API = 'https://api.github.com/repos/IacobIonut01/ReFra/releases';
export const RELEASES_URL = 'https://github.com/IacobIonut01/ReFra/releases';
export const ARCHITECTURES = [
  { id: 'universal', label: 'Universal', description: 'Not sure? Includes every supported architecture.' },
  { id: 'arm64-v8a', label: 'ARM64', description: 'Most recent Android phones and tablets.' },
  { id: 'armeabi-v7a', label: 'ARM 32-bit', description: 'Older Android devices.' },
  { id: 'x86_64', label: 'x86 64-bit', description: 'Some emulators and Intel devices.' },
  { id: 'x86', label: 'x86 32-bit', description: 'Older Intel devices and emulators.' },
];
export const VARIANTS = [
  { id: 'standard', title: 'Standard', description: 'Maps, cloud providers, and online features. Download AI models when you need them.', detail: 'No models bundled' },
  { id: 'offline', title: 'Offline', description: 'No network access, maps, or cloud providers. AI models cannot be downloaded after installation.', detail: 'No models bundled' },
  { id: 'offline-ml', title: 'Offline with AI', description: 'The offline build with bundled models for on-device AI features.', detail: 'Models included' },
  { id: 'standard-ml', title: 'Standard with AI', description: 'Online features with bundled AI models.', detail: 'Models included' },
];
export function normalizeRelease(raw) {
  if (!raw || raw.draft || typeof raw.tag_name !== 'string' || !Array.isArray(raw.assets) || !Number.isFinite(Date.parse(raw.published_at))) return null;
  const releasePath = `/IacobIonut01/ReFra/releases/tag/${encodeURIComponent(raw.tag_name)}`;
  const assets = [];
  for (const a of raw.assets) {
    if (!a || a.state !== 'uploaded' || typeof a.name !== 'string' || !Number.isSafeInteger(a.size) || a.size <= 0) continue;
    const match = /^ReFra-(.+)-(\d+)(-offline)?-(NoML|WithML)-(universal|arm64-v8a|armeabi-v7a|x86_64|x86)-release\.apk$/.exec(a.name);
    if (!match) continue;
    let url; try { url = new URL(a.browser_download_url); } catch { continue; }
    const expected = `/IacobIonut01/ReFra/releases/download/${encodeURIComponent(raw.tag_name)}/${encodeURIComponent(a.name)}`;
    if (url.origin !== 'https://github.com' || url.pathname !== expected || url.search || url.hash || url.username || url.password) continue;
    const variant = match[3] ? (match[4] === 'WithML' ? 'offline-ml' : 'offline') : (match[4] === 'WithML' ? 'standard-ml' : 'standard');
    assets.push({ name: a.name, url: url.href, size: a.size, variant, abi: match[5], sha256: typeof a.digest === 'string' && /^sha256:[a-f0-9]{64}$/i.test(a.digest) ? a.digest.slice(7).toLowerCase() : null });
  }
  if (!assets.length) return null;
  return { tag: raw.tag_name, name: typeof raw.name === 'string' ? raw.name : raw.tag_name, publishedAt: raw.published_at, url: `https://github.com${releasePath}`, prerelease: raw.prerelease === true, assets };
}
export function selectNightly(raw) {
  if (!Array.isArray(raw)) return null;
  return raw.filter((r) => r?.prerelease === true && typeof r.tag_name === 'string' && r.tag_name.endsWith('-nightly')).map(normalizeRelease).filter(Boolean).sort((a, b) => Date.parse(b.publishedAt) - Date.parse(a.publishedAt))[0] || null;
}
export function findAsset(release, variant, abi) {
  const matches = release?.assets?.filter((a) => a.variant === variant && a.abi === abi) || [];
  return matches.length === 1 ? matches[0] : null;
}
export function formatSize(bytes) { return `${(bytes / 1048576).toFixed(1)} MiB`; }
export function formatDate(iso) { return new Intl.DateTimeFormat('en', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(iso)); }
