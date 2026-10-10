#!/usr/bin/env node
/**
 * check-download.mjs — focused tests for the download surface.
 *
 * Part 1 (node): parser contract for src/lib/releases.mjs — variant/abi
 * mapping from the frozen snapshot, rejection rules, nightly selection.
 *
 * Part 2 (playwright): VariantPicker behaviour on the built site — SSR link,
 * control wiring, honest unavailable/error states, live-refresh semantics.
 * GitHub API calls are stubbed with fixtures rebuilt from registry/releases.json;
 * no APK is ever downloaded (the download URL is routed and aborted).
 *
 * Usage:
 *   node scripts/check-download.mjs                      # parser tests only
 *   node scripts/check-download.mjs --browser            # + browser tests
 *   CHECK_UI_BASE=http://127.0.0.1:4322 node scripts/check-download.mjs --browser
 *
 * Playwright resolution: CHECK_UI_PLAYWRIGHT scratch root, else 'playwright'
 * or 'playwright-core' resolved from this directory.
 */
import assert from 'node:assert/strict';
import { mkdirSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import {
  ARCHITECTURES,
  VARIANTS,
  findAsset,
  formatDate,
  formatSize,
  normalizeRelease,
  selectNightly,
} from '../src/lib/releases.mjs';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const snapshot = JSON.parse(readFileSync(join(ROOT, 'registry/releases.json'), 'utf8'));
const BASE = (process.env.CHECK_UI_BASE ?? 'http://127.0.0.1:4322').replace(/\/+$/, '');
const SITE = `${BASE}/ReFra`;
const OUT = process.env.CHECK_UI_OUT ?? join(tmpdir(), 'refra-download-check');

const failures = [];
const results = [];
const check = (name, ok, detail = '') => {
  results.push(`${ok ? 'PASS' : 'FAIL'} ${name}${detail ? ` — ${detail}` : ''}`);
  if (!ok) failures.push(name);
};
const t = (name, fn) => {
  try {
    fn();
    check(name, true);
  } catch (e) {
    check(name, false, e.message);
  }
};

/* ============================ part 1: parser ============================= */

t('snapshot maps 3 variants x 5 ABIs on stable', () => {
  const stable = snapshot.stable;
  for (const variant of ['standard', 'offline', 'offline-ml'])
    for (const abi of ARCHITECTURES.map((a) => a.id)) {
      const asset = findAsset(stable, variant, abi);
      assert.ok(asset, `${variant}/${abi} missing`);
      assert.equal(asset.variant, variant);
      assert.equal(asset.abi, abi);
      assert.ok(asset.url.includes('/releases/download/'));
    }
});
t('findAsset resolves standard-ml only when the snapshot has such assets', () => {
  const hasMl = snapshot.stable.assets.some((a) => a.variant === 'standard-ml');
  const found = findAsset(snapshot.stable, 'standard-ml', 'universal');
  assert.equal(found !== null, hasMl, 'standard-ml availability must match the snapshot');
  if (hasMl) assert.equal(found.abi, 'universal');
  assert.equal(findAsset(null, 'standard', 'universal'), null);
});
t('findAsset returns null when the match is ambiguous', () => {
  const dup = { assets: [snapshot.stable.assets[0], { ...snapshot.stable.assets[0] }] };
  assert.equal(findAsset(dup, 'standard', 'universal'), null);
});
t('normalizeRelease rejects malformed releases', () => {
  assert.equal(normalizeRelease(null), null);
  assert.equal(normalizeRelease({ draft: true, tag_name: 'x', published_at: '2026-01-01', assets: [{}] }), null);
  assert.equal(normalizeRelease({ tag_name: 'x', published_at: '2026-01-01', assets: [] }), null);
  assert.equal(normalizeRelease({ tag_name: 'x', published_at: 'not-a-date', assets: [{}] }), null);
});
const goodAsset = (over = {}) => ({
  name: 'ReFra-9.9.9-9999-NoML-universal-release.apk',
  state: 'uploaded',
  size: 1048576,
  browser_download_url:
    'https://github.com/IacobIonut01/ReFra/releases/download/9.9.9/ReFra-9.9.9-9999-NoML-universal-release.apk',
  digest: `sha256:${'a'.repeat(64)}`,
  ...over,
});
const goodRaw = (over = {}, assets = [goodAsset()]) => ({
  tag_name: '9.9.9',
  name: '9.9.9',
  draft: false,
  prerelease: false,
  published_at: '2026-10-01T00:00:00Z',
  assets,
  ...over,
});
t('normalizeRelease rejects foreign host and bad download path', () => {
  assert.equal(
    normalizeRelease(goodRaw({}, [goodAsset({ browser_download_url: 'https://evil.example.com/x.apk' })])),
    null,
  );
  assert.equal(
    normalizeRelease(
      goodRaw({}, [
        goodAsset({
          browser_download_url:
            'https://github.com/IacobIonut01/ReFra/releases/download/1.0.0/ReFra-9.9.9-9999-NoML-universal-release.apk',
        }),
      ]),
    ),
    null,
  );
});
t('normalizeRelease rejects URLs with query, hash or credentials', () => {
  const base =
    'https://github.com/IacobIonut01/ReFra/releases/download/9.9.9/ReFra-9.9.9-9999-NoML-universal-release.apk';
  for (const url of [`${base}?raw=1`, `${base}#frag`, 'https://u:p@github.com/IacobIonut01/ReFra/releases/download/9.9.9/ReFra-9.9.9-9999-NoML-universal-release.apk'])
    assert.equal(normalizeRelease(goodRaw({}, [goodAsset({ browser_download_url: url })])), null, url);
});
t('non-APK assets and non-uploaded assets are skipped', () => {
  const rel = normalizeRelease(
    goodRaw({}, [goodAsset(), { name: 'source.zip', state: 'uploaded', size: 5, browser_download_url: 'https://github.com/x' }, goodAsset({ name: 'ReFra-9.9.9-9999-NoML-x86-release.apk', state: 'starter' })]),
  );
  assert.equal(rel.assets.length, 1);
});
t('invalid digests become sha256 null', () => {
  const rel = normalizeRelease(goodRaw({}, [goodAsset({ digest: 'md5:abc' })]));
  assert.equal(rel.assets[0].sha256, null);
  const rel2 = normalizeRelease(goodRaw({}, [goodAsset({ digest: undefined })]));
  assert.equal(rel2.assets[0].sha256, null);
});
t('variant and abi map from the filename grammar', () => {
  const mk = (name) =>
    normalizeRelease(
      goodRaw({}, [goodAsset({ name, browser_download_url: `https://github.com/IacobIonut01/ReFra/releases/download/9.9.9/${name}` })]),
    ).assets[0];
  assert.equal(mk('ReFra-9.9.9-9999-NoML-arm64-v8a-release.apk').variant, 'standard');
  assert.equal(mk('ReFra-9.9.9-9999-offline-NoML-x86-release.apk').variant, 'offline');
  assert.equal(mk('ReFra-9.9.9-9999-offline-WithML-x86_64-release.apk').variant, 'offline-ml');
  assert.equal(mk('ReFra-9.9.9-9999-WithML-universal-release.apk').variant, 'standard-ml');
  assert.equal(mk('ReFra-9.9.9-9999-NoML-armeabi-v7a-release.apk').abi, 'armeabi-v7a');
});
t('selectNightly picks newest nightly, ignores stable and drafts', () => {
  // Asset URLs must carry the release's own tag to survive the path check.
  const rawForTag = (tag, over = {}) => {
    const name = 'ReFra-9.9.9-9999-NoML-universal-release.apk';
    return goodRaw(
      { tag_name: tag, ...over },
      [
        goodAsset({
          name,
          browser_download_url: `https://github.com/IacobIonut01/ReFra/releases/download/${tag}/${name}`,
        }),
      ],
    );
  };
  const rawStable = rawForTag('9.9.9', { published_at: '2026-10-04T00:00:00Z' });
  const rawNightlyOld = rawForTag('9.0.0-9000-nightly', { prerelease: true, published_at: '2026-01-01T00:00:00Z' });
  const rawNightlyNew = rawForTag('9.9.0-9900-nightly', { prerelease: true, published_at: '2026-09-01T00:00:00Z' });
  const rawDraft = rawForTag('9.9.9-9999-nightly', { prerelease: true, draft: true, published_at: '2026-10-05T00:00:00Z' });
  const rawPrereleaseOther = rawForTag('9.9.9-rc1', { prerelease: true, published_at: '2026-10-06T00:00:00Z' });
  const picked = selectNightly([rawStable, rawNightlyOld, rawNightlyNew, rawDraft, rawPrereleaseOther]);
  assert.equal(picked.tag, '9.9.0-9900-nightly');
  assert.equal(selectNightly([rawStable]), null);
  assert.equal(selectNightly('nope'), null);
});
t('formatSize and formatDate produce the expected text', () => {
  assert.equal(formatSize(1048576 * 100), '100.0 MiB');
  assert.equal(formatSize(393738443), '375.5 MiB');
  assert.equal(formatDate('2026-10-04T16:54:56Z'), 'October 4, 2026');
});

/* ============================ part 2: browser ============================ */
const runBrowser = process.argv.includes('--browser');
if (runBrowser) {
  function resolvePlaywright() {
    const roots = [];
    if (process.env.CHECK_UI_PLAYWRIGHT) roots.push(process.env.CHECK_UI_PLAYWRIGHT);
    roots.push(process.cwd());
    for (const dir of roots) {
      const req = createRequire(join(dir, 'check-download.mjs'));
      for (const name of ['playwright', 'playwright-core']) {
        try {
          const pw = req(name);
          if (pw?.chromium) return pw;
        } catch {
          /* next */
        }
      }
    }
    return null;
  }
  const pw = resolvePlaywright();
  if (!pw) {
    check('playwright resolved', false, 'set CHECK_UI_PLAYWRIGHT to a scratch install root');
  } else {
    let browser;
    try {
      browser = await pw.chromium.launch();
    } catch (err) {
      if (!/executable/i.test(String(err))) throw err;
      browser = await pw.chromium.launch({ channel: 'chrome' });
    }

    /** Raw GitHub payload rebuilt from the frozen snapshot (same truth). */
    const rawFromRelease = (rel, over = {}) => ({
      tag_name: rel.tag,
      name: rel.name,
      draft: false,
      prerelease: rel.prerelease,
      published_at: rel.publishedAt,
      assets: rel.assets.map((a) => ({
        name: a.name,
        state: 'uploaded',
        size: a.size,
        browser_download_url: a.url,
        digest: a.sha256 ? `sha256:${a.sha256}` : null,
      })),
      ...over,
    });
    const stableRaw = rawFromRelease(snapshot.stable);
    const nightlyRaw = rawFromRelease(snapshot.nightly);
    const listRaw = [stableRaw, nightlyRaw];

    const DEFAULT_URL = findAsset(snapshot.stable, 'standard', 'universal').url;
    const errors = [];
    const newPage = async (w = 1440, h = 1000, extra = {}) => {
      const p = await browser.newPage({ viewport: { width: w, height: h }, ...extra });
      p.on('pageerror', (e) => errors.push(`pageerror ${p.url()}: ${e.message}`));
      return p;
    };
    const stubAll = async (p, { stable = stableRaw, list = listRaw } = {}) =>
      p.route('https://api.github.com/**', (route) => {
        const url = route.request().url();
        if (url.endsWith('/latest')) return route.fulfill({ json: stable });
        return route.fulfill({ json: list });
      });

    const receipt = (p) => p.locator('variant-picker .picker-receipt');
    const downloadBtn = (p) => p.locator('variant-picker [data-download]');

    // ---- route ------------------------------------------------------------
    {
      const p = await newPage();
      const res = await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      check('download route serves 200', res.status() === 200);
      await p.close();
    }

    // ---- default render: real link, size, filename -------------------------
    {
      const p = await newPage();
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      const btn = downloadBtn(p);
      check('default CTA points at the standard/universal asset', (await btn.getAttribute('href')) === DEFAULT_URL, await btn.getAttribute('href'));
      check('download hint carries the asset filename', (await btn.getAttribute('download')) === findAsset(snapshot.stable, 'standard', 'universal').name);
      const sizeText = await p.locator('[data-receipt-file]').innerText();
      const expected = findAsset(snapshot.stable, 'standard', 'universal').size;
      check('file size matches formatSize of the asset', sizeText.includes(formatSize(expected)), sizeText);
      check('version heading uses the release tag', (await p.locator('[data-version]').innerText()) === `ReFra ${snapshot.stable.tag}`);
      check('filename is rendered', (await p.locator('[data-filename]').innerText()).endsWith('-release.apk'));
      await p.close();
    }

    // ---- no-JS: SSR link works, controls stay disabled ------------------------
    {
      const ctx = await browser.newContext({ javaScriptEnabled: false });
      const p = await ctx.newPage();
      await p.setViewportSize({ width: 1440, height: 1000 });
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      const href = await p.locator('variant-picker [data-download]').getAttribute('href');
      check('no-JS renders a working direct link', href === DEFAULT_URL, href ?? 'none');
      const disabledCount = await p.locator('variant-picker fieldset[disabled]').count();
      check('no-JS keeps channel/build fieldsets disabled', disabledCount === 2, `${disabledCount}`);
      check('no-JS keeps architecture select disabled', await p.locator('[data-abi]').isDisabled());
      await ctx.close();
    }

    // ---- selection wiring ----------------------------------------------------
    {
      const p = await newPage();
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      // Offline with AI + arm64 → exact matched asset.
      await p.locator('.variant-row[data-variant="offline-ml"]').click();
      await p.locator('[data-abi]').selectOption('arm64-v8a');
      const expected = findAsset(snapshot.stable, 'offline-ml', 'arm64-v8a');
      check('offline-ml + arm64 selects the exact asset', (await downloadBtn(p).getAttribute('href')) === expected.url, await downloadBtn(p).getAttribute('href'));
      check('receipt choice label updates', /Offline with AI · ARM64/.test(await p.locator('[data-receipt-choice]').innerText()));
      // Every ABI resolvable for standard.
      for (const abi of ARCHITECTURES.map((a) => a.id)) {
        await p.locator('[data-abi]').selectOption(abi);
        const asset = findAsset(snapshot.stable, 'offline-ml', abi);
        const href = await downloadBtn(p).getAttribute('href');
        check(`abi ${abi} resolves`, href === asset?.url, `${abi} -> ${href ?? 'null'}`);
      }
      // Keyboard: focus channel group, arrow key moves stable -> nightly.
      await p.locator('input[value="stable"]').focus();
      await p.keyboard.press('ArrowRight');
      check('arrow key moves channel to nightly', await p.locator('input[value="nightly"]').isChecked());
      await p.close();
    }

    // ---- nightly channel ------------------------------------------------------
    {
      const p = await newPage();
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      await p.locator('input[value="nightly"]').click();
      await p.waitForFunction(() => document.querySelector('[data-version]')?.textContent.includes('nightly'));
      check('nightly channel shows nightly tag', (await p.locator('[data-version]').innerText()).includes(snapshot.nightly.tag));
      const warn = await p.locator('[data-channel-warning]').innerText();
      check('nightly warning shown', /for testing and may be less stable/.test(warn));
      check('older-than-stable notice shown', /published before the current stable release/.test(warn), warn);
      check('nightly receipt marks Nightly build', (await p.locator('[data-receipt-channel]').innerText()) === 'Nightly build');
      const href = await downloadBtn(p).getAttribute('href');
      check('nightly CTA targets a nightly asset', href === findAsset(snapshot.nightly, 'standard', 'universal').url, href);
      await p.close();
    }

    // ---- delayed stable cannot overwrite nightly view --------------------------
    {
      const p = await newPage();
      let releaseStable;
      const stableGate = new Promise((r) => (releaseStable = r));
      await p.route('https://api.github.com/**', (route) => {
        const url = route.request().url();
        if (url.endsWith('/latest')) return stableGate.then(() => route.fulfill({ json: stableRaw }));
        return route.fulfill({ json: listRaw });
      });
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      await p.locator('input[value="nightly"]').click();
      await p.waitForFunction(() => document.querySelector('[data-version]')?.textContent.includes('nightly'));
      releaseStable(); // stable answer lands while viewing nightly
      await p.waitForFunction(
        () => !document.querySelector('[data-freshness]') || document.querySelector('[data-freshness]').hidden,
        { timeout: 5000 },
      ).catch(() => {});
      await p.waitForFunction(
        () => document.querySelector('[data-version]')?.textContent.includes('nightly'),
        { timeout: 5000 },
      );
      check('stale stable response never overwrites nightly view', (await p.locator('[data-version]').innerText()).includes(snapshot.nightly.tag));
      await p.close();
    }

    // ---- fresh release missing the selected asset disables the CTA -------------
    {
      const p = await newPage();
      // A valid fresh stable that lacks the standard/universal asset.
      const slimRaw = rawFromRelease(snapshot.stable, {
        assets: rawFromRelease(snapshot.stable).assets.filter(
          (a) => !(a.name === findAsset(snapshot.stable, 'standard', 'universal').name),
        ),
      });
      await stubAll(p, { stable: slimRaw });
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      // Wait for the refresh to land (freshness line clears).
      await p.waitForFunction(
        () => document.querySelector('[data-freshness]')?.hidden === true,
        { timeout: 8000 },
      );
      check('missing selected asset disables the CTA', (await downloadBtn(p).getAttribute('href')) === null);
      check('disabled CTA is marked', (await downloadBtn(p).getAttribute('aria-disabled')) === 'true');
      check('unavailable copy explains the gap', /not published for the selected architecture/.test(await p.locator('[data-unavailable]').innerText()));
      await p.locator('[data-abi]').selectOption('arm64-v8a');
      const arm = findAsset(snapshot.stable, 'standard', 'arm64-v8a');
      check('choosing arm64 restores the exact link', (await downloadBtn(p).getAttribute('href')) === arm.url, await downloadBtn(p).getAttribute('href'));
      await p.close();
    }

    // ---- fresh nightly list without a nightly hides stale notes -----------------
    {
      const p = await newPage();
      await p.route('https://api.github.com/**', (route) => {
        if (route.request().url().endsWith('/latest')) return route.fulfill({ json: stableRaw });
        return route.fulfill({ json: [stableRaw] }); // no nightly in the list
      });
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      await p.locator('input[value="nightly"]').click();
      await p.waitForFunction(
        () => document.querySelector('[data-version]')?.textContent === 'No release published',
        { timeout: 8000 },
      );
      check('empty nightly list marks Nightly build', (await p.locator('[data-receipt-channel]').innerText()) === 'Nightly build');
      check('empty nightly hides stale release notes', !(await p.locator('[data-notes]').isVisible()));
      check('empty nightly disables the CTA', (await downloadBtn(p).getAttribute('href')) === null);
      check('channel status reports no nightly', /No nightly build is published/.test(await p.locator('[data-channel-status]').innerText()));
      await p.close();
    }

    // ---- missing digest shows its own explanation --------------------------------
    {
      const p = await newPage();
      const bareRaw = rawFromRelease(snapshot.stable);
      bareRaw.assets = bareRaw.assets.map((a) =>
        a.name === findAsset(snapshot.stable, 'standard', 'universal').name ? { ...a, digest: null } : a,
      );
      await stubAll(p, { stable: bareRaw });
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      await p.waitForFunction(
        () => document.querySelector('[data-freshness]')?.hidden === true,
        { timeout: 8000 },
      );
      check('missing digest hides the checksum disclosure', !(await p.locator('[data-sha]').isVisible()));
      const note = await p.locator('[data-checksum-missing]').innerText();
      check('missing digest explains itself', /A checksum is not supplied for this file/.test(note), note);
      await p.close();
    }

    // ---- status lifecycle: no stale Checking, copy status resets -----------------
    {
      const p = await newPage(1440, 1000, { permissions: ['clipboard-read', 'clipboard-write'] });
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'load' });
      await p.waitForFunction(
        () => document.querySelector('[data-freshness]')?.hidden === true,
        { timeout: 8000 },
      );
      const settled = await p.locator('[data-status]').innerText();
      check('settled status does not say Checking', !/Checking/.test(settled), settled);
      await p.locator('[data-sha] summary').click();
      await p.locator('[data-copy]').click();
      await p.waitForFunction(() => document.querySelector('[data-status]')?.textContent === 'Checksum copied', { timeout: 5000 });
      await p.locator('.variant-row[data-variant="offline"]').click();
      const after = await p.locator('[data-status]').innerText();
      check('variant change clears the copy status', /Offline for Universal selected\./.test(after), after);
      await p.close();
    }

    // ---- HTTP 403 keeps the saved snapshot; retry recovers ---------------------
    {
      const p = await newPage();
      let fail = true;
      await p.route('https://api.github.com/**', (route) => {
        if (fail) return route.fulfill({ status: 403, json: { message: 'rate limited' } });
        if (route.request().url().endsWith('/latest')) return route.fulfill({ json: stableRaw });
        return route.fulfill({ json: listRaw });
      });
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      await p.waitForFunction(() => document.querySelector('[data-error]')?.hidden === false, { timeout: 8000 });
      const errText = await p.locator('[data-error-text]').innerText();
      check('403 keeps saved snapshot with clear status', /Showing saved release information/.test(errText), errText);
      check('CTA still works on saved snapshot', (await downloadBtn(p).getAttribute('href')) === DEFAULT_URL);
      fail = false;
      await p.locator('[data-retry]').click();
      await p.waitForFunction(() => document.querySelector('[data-error]')?.hidden === true, { timeout: 8000 });
      check('retry after failure recovers', (await downloadBtn(p).getAttribute('href')) === DEFAULT_URL);
      await p.close();
    }

    // ---- empty-assets response must not enable a wrong download -----------------
    {
      const p = await newPage();
      await stubAll(p, { stable: rawFromRelease(snapshot.stable, { assets: [] }) });
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      await p.waitForFunction(() => document.querySelector('[data-error]')?.hidden === false, { timeout: 8000 });
      check('empty fresh release keeps snapshot link', (await downloadBtn(p).getAttribute('href')) === DEFAULT_URL);
      await p.close();
    }

    // ---- network failure --------------------------------------------------------
    {
      const p = await newPage();
      await p.route('https://api.github.com/**', (route) => route.abort('failed'));
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      await p.waitForFunction(() => document.querySelector('[data-error]')?.hidden === false, { timeout: 8000 });
      const errText = await p.locator('[data-error-text]').innerText();
      check('network failure reports clear status', /Could not check for/.test(errText), errText);
      check('snapshot still downloadable after failure', (await downloadBtn(p).getAttribute('href')) === DEFAULT_URL);
      await p.close();
    }

    // ---- checksum disclosure + copy ---------------------------------------------
    {
      const ctx = await browser.newContext({ permissions: ['clipboard-read', 'clipboard-write'] });
      const p = await ctx.newPage();
      await p.setViewportSize({ width: 1440, height: 1000 });
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      await p.locator('[data-sha] summary').click();
      await p.locator('[data-copy]').click();
      await p.waitForFunction(() => document.querySelector('[data-status]')?.textContent === 'Checksum copied', { timeout: 5000 });
      check('copy reports success', true);
      const clip = await p.evaluate(() => navigator.clipboard.readText());
      check('clipboard holds the asset sha256', clip === findAsset(snapshot.stable, 'standard', 'universal').sha256, clip?.slice(0, 16));
      // Failure path: clipboard throws.
      await p.evaluate(() => { navigator.clipboard.writeText = () => Promise.reject(new Error('denied')); });
      await p.locator('[data-copy]').click();
      await p.waitForFunction(() => document.querySelector('[data-status]')?.textContent.includes('Could not copy'), { timeout: 5000 });
      check('copy failure reports fallback message', true);
      await ctx.close();
    }

    // ---- CTA click target + announcement, no APK saved ---------------------------
    {
      const p = await newPage();
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      // Intercept the navigation in-page so the tab stays alive to inspect.
      await downloadBtn(p).evaluate((el) =>
        el.addEventListener('click', (e) => e.preventDefault()),
      );
      await downloadBtn(p).click();
      check('CTA click targets the exact asset URL', (await downloadBtn(p).getAttribute('href')) === DEFAULT_URL);
      check('status announces browser handoff', /browser will handle the APK download/.test(await p.locator('[data-status]').innerText()));
      await p.close();
    }

    // ---- HEAD the real asset (headers only; opt-in via CHECK_DOWNLOAD_HEAD=1) ----
    if (process.env.CHECK_DOWNLOAD_HEAD === '1') {
      const head = await fetch(DEFAULT_URL, { method: 'HEAD', redirect: 'manual' }).catch(() => null);
      const ok = head && [200, 301, 302].includes(head.status);
      check('asset URL answers a HEAD request (redirect/200)', !!ok, head ? `${head.status} -> ${head.headers.get('location')?.slice(0, 60)}` : 'network unreachable');
    }

    // ---- overflow + dark --------------------------------------------------------
    const settleForShot = async (p) => {
      await p.evaluate(() => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, 0);
      });
      await p.waitForFunction(() => window.scrollY === 0, { timeout: 3000 });
      // Wait for the release refresh to finish so no 'Checking…' leaks in.
      await p
        .waitForFunction(() => document.querySelector('[data-freshness]')?.hidden === true, {
          timeout: 8000,
        })
        .catch(() => {});
    };
    {
      const p = await newPage(390, 844);
      await p.route('https://api.github.com/**', (route) => route.fulfill({ json: stableRaw }));
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      check('download 390px no overflow', (await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)) <= 0);
      // Expand the checksum disclosure and select offline-ml for the shot.
      await p.locator('.variant-row[data-variant="offline-ml"]').click();
      await p.locator('[data-sha] summary').click();
      await settleForShot(p);
      await shot(p, 'download-mobile-offline-ai');
      await p.close();
    }
    {
      const p = await newPage(1440, 1000);
      await stubAll(p);
      await p.goto(`${SITE}/download`, { waitUntil: 'networkidle' });
      check('download 1440px no overflow', (await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)) <= 0);
      await settleForShot(p);
      await shot(p, 'download-desktop');
      await p.locator('.theme-select').selectOption('dark');
      // Two animation frames + a beat for theme transitions to settle.
      await p.evaluate(
        () => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(r, 200)))),
      );
      await shot(p, 'download-dark');
      await p.close();
    }

    const jsErrors = [...new Set(errors)];
    if (jsErrors.length) {
      console.log(' JS errors:');
      for (const e of jsErrors) console.log(`   ${e}`);
      jsErrors.forEach((e) => failures.push(`js: ${e.slice(0, 80)}`));
    }
    await browser.close();
  }
}

async function shot(p, name) {
  mkdirSync(OUT, { recursive: true });
  await p.screenshot({ path: join(OUT, `${name}.png`), fullPage: true });
}

console.log(`check-download ${runBrowser ? `(browser via ${SITE})` : '(parser only)'}`);
for (const r of results) console.log(` ${r}`);
console.log(`\ncheck-download: ${failures.length ? `${failures.length} FAILURE(S)` : 'all green'}`);
process.exit(failures.length ? 1 : 0);
