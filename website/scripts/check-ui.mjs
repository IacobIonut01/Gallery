#!/usr/bin/env node
/**
 * check-ui.mjs — browser smoke test for the docs site UI.
 *
 * Drives a running dev or preview server with Playwright and asserts the
 * structural contract: hero capture, header nav + theme persistence, the
 * feature-directory filter, DeviceStage mount hygiene (one media child, one
 * state control), manifest-mocked fidelity regressions, TOC anchor
 * integrity, horizontal overflow, and renders screenshots for review.
 *
 * Usage:
 *   node scripts/check-ui.mjs                  # http://localhost:4321
 *   CHECK_UI_BASE=http://localhost:4322 node scripts/check-ui.mjs
 *
 * Playwright is not a project dependency. Resolution order:
 *   1. CHECK_UI_PLAYWRIGHT — path to a scratch install root whose
 *      node_modules holds `playwright` or `playwright-core`
 *      (e.g. `npm i --prefix /tmp/pw playwright-core`)
 *   2. `playwright` / `playwright-core` resolved from this directory
 * The bundled Chromium is tried first; an installed Chrome channel is used
 * only if the Chromium executable is absent.
 *
 * Env: CHECK_UI_OUT — screenshot output dir (default: system tmp).
 */
import { mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const BASE = (process.env.CHECK_UI_BASE ?? 'http://localhost:4321').replace(/\/+$/, '');
const SITE = `${BASE}/ReFra`;
const OUT = process.env.CHECK_UI_OUT ?? join(tmpdir(), 'refra-ui-check');

function resolvePlaywright() {
  const roots = [];
  if (process.env.CHECK_UI_PLAYWRIGHT) roots.push(process.env.CHECK_UI_PLAYWRIGHT);
  roots.push(process.cwd());
  for (const dir of roots) {
    const req = createRequire(join(dir, 'check-ui.mjs'));
    for (const name of ['playwright', 'playwright-core']) {
      try {
        const pw = req(name);
        if (pw?.chromium) return pw;
      } catch {
        /* try the next candidate */
      }
    }
  }
  return null;
}

const pw = resolvePlaywright();
if (!pw) {
  console.error(
    'check-ui: playwright not found. Install it in a scratch dir and point\n' +
      'CHECK_UI_PLAYWRIGHT at it, e.g.:\n' +
      '  npm i --prefix /tmp/refra-ui-deps playwright-core && \\\n' +
      '    CHECK_UI_PLAYWRIGHT=/tmp/refra-ui-deps node scripts/check-ui.mjs',
  );
  process.exit(2);
}
const { chromium } = pw;

let browser;
try {
  browser = await chromium.launch();
} catch (err) {
  if (!/executable/i.test(String(err))) throw err;
  browser = await chromium.launch({ channel: 'chrome' });
}

const failures = [];
const results = [];
const check = (name, ok, detail = '') => {
  results.push(`${ok ? 'PASS' : 'FAIL'} ${name}${detail ? ` — ${detail}` : ''}`);
  if (!ok) failures.push(name);
};

const shot = async (page, name) => {
  mkdirSync(OUT, { recursive: true });
  await page.screenshot({ path: join(OUT, `${name}.png`), fullPage: true });
};

const errors = [];
async function page(width, height, extra = {}) {
  const p = await browser.newPage({ viewport: { width, height }, ...extra });
  p.on('pageerror', (e) => errors.push(`pageerror ${p.url()}: ${e.message}`));
  p.on('console', (m) => {
    // Pagefind assets only exist after a production build; the search page
    // renders an explicit fallback note in dev, so its 404 is expected.
    if (m.type() === 'error' && !(m.location()?.url ?? '').includes('/pagefind/'))
      errors.push(`console.error ${p.url()}: ${m.text()}`);
  });
  return p;
}
const overflow = (p) =>
  p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);

/** True when every img/video/iframe inside the stage is a loaded media node. */
const mediaState = (p, stageSel) =>
  p.evaluate((sel) => {
    const stage = document.querySelector(sel);
    if (!stage) return { count: -1, loaded: false };
    const media = [...stage.querySelectorAll('.stage-frame img, .stage-frame video, .stage-frame iframe')];
    const pending = stage.querySelectorAll('.embed-pending').length;
    return {
      count: media.length,
      pending,
      loaded: media.every((el) => el.tagName === 'IFRAME' || (el.complete && el.naturalWidth > 0)),
    };
  }, stageSel);

const CAPTURE_SRC = 'captures/settings-list/phone.webp';
const captureFixture = (scene, overrides = {}) => ({
  scenes: {
    [scene]: {
      id: scene,
      title: 'Fixture scene',
      path: ['Settings'],
      states: [],
      fidelity: 'capture',
      assets: {},
      ...overrides,
    },
  },
});

/* ------------------------------ key routes ------------------------------- */
for (const route of ['/', '/features/', '/features/appearance', '/features/timeline', '/build', '/download', '/search']) {
  const p = await page(1440, 1000);
  const res = await p.goto(`${SITE}${route}`, { waitUntil: 'networkidle' });
  const text = await p.evaluate(() => document.body?.innerText?.trim().length ?? 0);
  check(`route ${route}`, res?.status() === 200 && text > 50, `status ${res?.status()}, ${text} chars`);
  await p.close();
}

/* ------------------------------- home hero ------------------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/`, { waitUntil: 'networkidle' });
  const img = p.locator('.hero-preview live-embed img').first();
  await img.waitFor({ timeout: 10000 });
  const ok = await img.evaluate((el) => el.complete && el.naturalWidth > 0);
  const src = (await img.getAttribute('src')) ?? '';
  check('hero renders a real capture', ok && src.includes('captures/settings-list'), src || 'no img');
  await shot(p, 'home-desktop');
  await p.close();
}
{
  const p = await page(390, 844);
  await p.goto(`${SITE}/`, { waitUntil: 'networkidle' });
  await shot(p, 'home-mobile');
  check('home mobile no overflow', (await overflow(p)) <= 0, `overflow ${await overflow(p)}px`);
  await p.close();
}

/* ------------------------- mobile nav + aria-current --------------------- */
{
  const p = await page(390, 844);
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'load' });
  const nav = p.locator('.mobile-nav');
  const summary = nav.locator('summary');
  check('mobile nav starts closed', (await nav.getAttribute('open')) === null);
  await summary.click();
  check('mobile nav opens', (await nav.getAttribute('open')) !== null);
  const current = await nav.locator('[aria-current="page"]').innerText();
  check('mobile nav marks current page', current === 'Features', current);
  await p.keyboard.press('Escape');
  check('Escape closes mobile nav', (await nav.getAttribute('open')) === null);
  check('Escape returns focus to summary', (await p.evaluate(() => document.activeElement?.tagName)) === 'SUMMARY');
  await p.close();
}

/* -------------------------- desktop nav active --------------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'load' });
  const label = await p.locator('.main-nav [aria-current="page"]').innerText().catch(() => '');
  check('desktop nav marks Features active', label === 'Features', label);
  await p.close();
}

/* ------------------------- theme select persistence ---------------------- */

// Opens the icon-trigger menu and picks a theme; mirrors the keyboard/mouse
// paths users take on the real control.
const pickTheme = async (p, theme) => {
  await p.locator('.theme-trigger').click();
  await p.locator(`[data-theme-choice="${theme}"]`).click();
};

{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/`, { waitUntil: 'load' });
  const trigger = p.locator('.theme-trigger');
  check(
    'theme control is an icon trigger',
    (await trigger.count()) === 1 &&
      (await trigger.getAttribute('aria-label')) === 'Color theme' &&
      (await trigger.textContent()).trim() === '',
    `count=${await trigger.count()} label=${await trigger.getAttribute('aria-label')}`,
  );
  await pickTheme(p, 'amoled');
  await p.goto(`${SITE}/features/`, { waitUntil: 'load' });
  const theme = await p.evaluate(() => document.documentElement.dataset.theme);
  check('theme persists across routes', theme === 'amoled', theme);
  const checked = await p
    .locator('[data-theme-choice="amoled"]')
    .getAttribute('aria-checked');
  check('menu option reflects stored theme', checked === 'true', `aria-checked=${checked}`);
  await p.evaluate(() => localStorage.setItem('refra-theme', 'bogus'));
  await p.goto(`${SITE}/`, { waitUntil: 'load' });
  const bogus = await p.evaluate(() => document.documentElement.dataset.theme);
  check('invalid stored theme falls back to auto', bogus === 'auto', bogus);
  await pickTheme(p, 'dark');
  await shot(p, 'home-dark');
  await p.evaluate(() => localStorage.removeItem('refra-theme'));
  await p.close();
}

/* --------------------------- directory filter ---------------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/features/`, { waitUntil: 'load' });
  const total = await p.locator('.feature-row').count();
  const sectionCount = await p.locator('.area-section').count();
  await p.locator('#feature-filter').fill('appearance');
  const shown = await p.locator('.feature-row:not([hidden])').count();
  const visibleSections = await p.locator('.area-section:not([hidden])').count();
  const foundTitle = await p.locator('.feature-row:not([hidden]) .feature-title').first().innerText();
  check('filter finds Appearance', foundTitle === 'Appearance' && shown === 1, `${foundTitle} (${shown} shown)`);
  check('filter hides unmatched groups', visibleSections === 1, `${visibleSections} sections`);
  await p.locator('#feature-filter').fill('zzz-no-such-thing');
  check('no-results state appears', await p.locator('.filter-empty').isVisible());
  check('all groups hidden on no match', (await p.locator('.area-section:not([hidden])').count()) === 0);
  await p.locator('.filter-clear').click();
  const restored = await p.locator('.feature-row:not([hidden])').count();
  check('clear restores all entries', restored === total && (await p.locator('.area-section:not([hidden])').count()) === sectionCount, `${restored}/${total}`);
  check('directory no overflow', (await overflow(p)) <= 0);
  await shot(p, 'directory-desktop');
  await p.close();
}

/* --------------------- device switch mount hygiene ----------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/features/settings-overview`, { waitUntil: 'networkidle' });
  // settings-list is an extra scene with real captures; open its details.
  await p.locator('.scene-details summary', { hasText: 'Settings list' }).click();
  const stageSel = '.scene-details[open] device-stage';
  const stage = p.locator(stageSel);
  await p.waitForFunction(
    (sel) => document.querySelectorAll(`${sel} .stage-frame img`).length === 1,
    stageSel,
    { timeout: 5000 },
  );
  for (const d of ['tablet', 'foldable', 'phone']) {
    await stage.locator(`.stage-preset[data-device="${d}"]`).click();
    await p.waitForFunction(
      (sel) => {
        const media = document.querySelectorAll(`${sel} .stage-frame img, ${sel} .stage-frame video, ${sel} .stage-frame iframe`);
        const pending = document.querySelectorAll(`${sel} .embed-pending`).length;
        return media.length + pending === 1;
      },
      stageSel,
      { timeout: 5000 },
    );
  }
  const media = await mediaState(p, `${stageSel}`);
  check('device switching leaves one media element', media.count === 1 && media.loaded, JSON.stringify(media));
  const stateControls = await stage.locator('.stage-state, .embed-states').count();
  check('at most one state control', stateControls <= 1, `${stateControls}`);
  check('status reports captured preview', /Captured preview/.test(await stage.locator('.stage-status').innerText()));
  // The scaled frame must sit inside the stage viewport bounds.
  const fits = await p.evaluate((sel) => {
    const vp = document.querySelector(`${sel} .stage-viewport`).getBoundingClientRect();
    const fr = document.querySelector(`${sel} .stage-frame`).getBoundingClientRect();
    return fr.bottom <= vp.bottom + 0.5;
  }, stageSel);
  check('frame settles inside viewport bounds', fits);
  check('feature page no overflow', (await overflow(p)) <= 0);
  await p.close();
}

/* --------------- fixture (a): missing device recovers -------------------- */
{
  const p = await page(1440, 1000);
  await p.route('**/docs-manifest.json', (route) =>
    route.fulfill({
      json: captureFixture('settings-hub', {
        assets: { phone: { default: { src: CAPTURE_SRC, type: 'image' } } },
      }),
    }),
  );
  await p.goto(`${SITE}/features/settings-overview`, { waitUntil: 'networkidle' });
  const stageSel = '.docs-article > device-stage';
  const stage = p.locator(stageSel).first();
  await p.waitForFunction(
    (sel) => document.querySelectorAll(`${sel} .stage-frame img`).length === 1,
    stageSel,
    { timeout: 5000 },
  );
  await stage.locator('.stage-preset[data-device="tablet"]').click();
  await p.waitForFunction(
    (sel) => document.querySelectorAll(`${sel} .embed-pending`).length === 1,
    stageSel,
    { timeout: 5000 },
  );
  // Controls stay reachable so the reader can recover the working device.
  check('missing device keeps toolbar controls', await stage.locator('.stage-preset[data-device="phone"]').isVisible());
  await stage.locator('.stage-preset[data-device="phone"]').click();
  await p.waitForFunction(
    (sel) => {
      const imgs = [...document.querySelectorAll(`${sel} .stage-frame img`)];
      return imgs.length === 1 && imgs[0].complete && imgs[0].naturalWidth > 0;
    },
    stageSel,
    { timeout: 5000 },
  );
  const media = await mediaState(p, stageSel);
  check('recovering to phone restores one real capture', media.count === 1 && media.loaded, JSON.stringify(media));
  check('status reports captured preview after recovery', /Captured preview/.test(await stage.locator('.stage-status').innerText()));
  await p.close();
}

/* ------------- fixture (b): state switch keeps one image ----------------- */
{
  const p = await page(1440, 1000);
  await p.route('**/docs-manifest.json', (route) =>
    route.fulfill({
      json: captureFixture('settings-appearance', {
        states: ['default', 'busy'],
        assets: {
          phone: {
            default: { src: CAPTURE_SRC, type: 'image' },
            busy: { src: CAPTURE_SRC, type: 'image' },
          },
        },
      }),
    }),
  );
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'networkidle' });
  const stageSel = '.docs-article > device-stage';
  const stage = p.locator(stageSel).first();
  await p.waitForFunction(
    (sel) => document.querySelectorAll(`${sel} .stage-frame img`).length === 1,
    stageSel,
    { timeout: 5000 },
  );
  const sel = stage.locator('select.stage-state');
  check('state select appears for multi-state scene', await sel.isVisible());
  for (const value of ['busy', 'default', 'busy']) {
    await sel.selectOption(value);
    await p.waitForFunction(
      ({ sel: s, v }) => {
        const stage = document.querySelector(s);
        const imgs = stage.querySelectorAll('.stage-frame img');
        const picker = stage.querySelector('select.stage-state');
        return imgs.length === 1 && picker?.value === v;
      },
      { sel: stageSel, v: value },
      { timeout: 5000 },
    );
  }
  const media = await mediaState(p, stageSel);
  check('repeated state switches keep one loaded image', media.count === 1 && media.loaded, JSON.stringify(media));
  check('select value tracks last state', (await sel.inputValue()) === 'busy');
  check('still a single state control', (await stage.locator('.stage-state, .embed-states').count()) <= 1);
  await p.close();
}

/* --------- fixture (c): live error falls back to capture ----------------- */
{
  const p = await page(1440, 1000);
  await p.route('**/docs-manifest.json', (route) =>
    route.fulfill({
      json: captureFixture('settings-appearance', {
        fidelity: 'live',
        assets: { phone: { default: { src: CAPTURE_SRC, type: 'image' } } },
      }),
    }),
  );
  await p.route('**/live/index.html*', (route) =>
    route.fulfill({
      contentType: 'text/html',
      body: `<!doctype html><html><body><script>
        parent.postMessage(JSON.stringify({ type: 'docs:error' }), '*');
      </script></body></html>`,
    }),
  );
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'networkidle' });
  const stageSel = '.docs-article > device-stage';
  const stage = p.locator(stageSel).first();
  await p.waitForFunction(
    (sel) => {
      const imgs = [...document.querySelectorAll(`${sel} .stage-frame img`)];
      return imgs.length === 1 && imgs[0].complete && imgs[0].naturalWidth > 0;
    },
    stageSel,
    { timeout: 8000 },
  );
  const detached = await p.evaluate((sel) => {
    const embed = document.querySelector(`${sel} live-embed`);
    return embed.iframe === null && embed.ready === false;
  }, stageSel);
  check('docs:error swaps to capture and drops the iframe', detached);
  // Controls still work on the fallback: tablet has no asset -> pending.
  await stage.locator('.stage-preset[data-device="tablet"]').click();
  await p.waitForFunction(
    (sel) => document.querySelectorAll(`${sel} .embed-pending`).length === 1,
    stageSel,
    { timeout: 5000 },
  );
  check('controls work after live fallback', await stage.locator('.stage-preset[data-device="phone"]').isVisible());
  await p.close();
}

/* ----------- fixture (d): unavailable scene collapses chrome ------------- */
{
  const p = await page(1440, 1000);
  await p.route('**/docs-manifest.json', (route) =>
    route.fulfill({
      json: captureFixture('settings-appearance', { fidelity: 'pending', assets: {}, states: [] }),
    }),
  );
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'networkidle' });
  const stageSel = '.docs-article > device-stage';
  const stage = p.locator(stageSel).first();
  await p.waitForFunction(
    (sel) => document.querySelector(`${sel} .stage-empty`)?.hidden === false,
    stageSel,
    { timeout: 5000 },
  );
  check('unavailable scene hides toolbar', !(await stage.locator('.stage-toolbar').isVisible()));
  check('unavailable scene hides state select', !(await stage.locator('select.stage-state').isVisible()));
  check('unavailable scene hides status row', !(await stage.locator('.stage-status-row').isVisible()));
  const body = await stage.locator('.stage-empty-body').innerText();
  check('empty notice carries the embed status', body.length > 10, body);
  await p.close();
}

/* ----------------------- pending scene state select ---------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'networkidle' });
  const stage = p.locator('device-stage').first();
  // settings-appearance registers states but has no captures yet: the stage
  // must collapse to a compact notice instead of an empty 480px frame.
  check('pending scene collapses the frame', await p.locator('.stage-empty').first().isVisible());
  const sel = stage.locator('.stage-state');
  if (await sel.isVisible()) {
    const opts = await sel.locator('option').allInnerTexts();
    await sel.selectOption({ index: Math.min(1, opts.length - 1) });
    await p.waitForFunction(
      () => document.querySelectorAll('device-stage .embed-pending').length === 1,
      { timeout: 5000 },
    );
    check('state change never appends duplicate content', true);
    check('still one state control', (await stage.locator('.stage-state, .embed-states').count()) <= 1);
  }
  await shot(p, 'feature-appearance-desktop');
  await p.close();
}
{
  const p = await page(390, 844);
  await p.goto(`${SITE}/features/appearance`, { waitUntil: 'networkidle' });
  check('feature mobile no overflow', (await overflow(p)) <= 0);
  await shot(p, 'feature-appearance-mobile');
  await p.close();
}

/* --------------------- docs sidebar collapse on resize ------------------- */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/overview`, { waitUntil: 'load' });
  const openDesktop = await p.evaluate(() => document.querySelector('[data-docs-nav]')?.open);
  check('docs nav open on desktop', openDesktop === true);
  await p.setViewportSize({ width: 500, height: 800 });
  await p.waitForFunction(() => document.querySelector('[data-docs-nav]')?.open === false, { timeout: 3000 });
  check('docs nav collapses to disclosure below 720', true);
  await p.setViewportSize({ width: 1440, height: 1000 });
  await p.waitForFunction(() => document.querySelector('[data-docs-nav]')?.open === true, { timeout: 3000 });
  check('docs nav reopens on desktop', true);
  await p.close();
}

/* ------------------------------ TOC anchors ------------------------------ */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/overview`, { waitUntil: 'load' });
  const hrefs = await p.locator('.docs-toc a').evaluateAll((els) => els.map((a) => a.getAttribute('href')));
  const missing = [];
  for (const h of hrefs) {
    const id = h?.startsWith('#') ? h.slice(1) : null;
    if (id && !(await p.evaluate((i) => !!document.getElementById(i), id))) missing.push(h);
  }
  check('TOC links resolve to heading ids', hrefs.length > 0 && missing.length === 0, `${hrefs.length} links${missing.length ? `, missing ${missing}` : ''}`);
  await p.close();
}

/* ------------------------------- search page ------------------------------ */
{
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/search`, { waitUntil: 'networkidle' });
  const hasUI = await p
    .waitForSelector('.pagefind-ui__form, .search-mount .card', { timeout: 10000 })
    .then((el) => el.evaluate((e) => e.classList.contains('pagefind-ui__form')).catch(() => false))
    .catch(() => false);
  if (hasUI) {
    // Production build: run a real query and follow a result.
    await p.locator('.pagefind-ui__search-input').fill('appearance');
    const result = p.locator('.pagefind-ui__result a.pagefind-ui__result-link[href*="features/appearance"]').first();
    await result.waitFor({ timeout: 10000 });
    const nav = p.waitForURL('**/features/appearance*', { timeout: 10000 }).catch(() => null);
    await result.click();
    await nav;
    check('search result navigates to the feature page', p.url().includes('/features/appearance'), p.url());
  } else {
    const note = await p.locator('.search-mount .card').innerText().catch(() => '');
    check('search renders UI or build fallback', /unavailable|generated/i.test(note), note.slice(0, 60));
  }
  await p.close();
}

/* ------------------- widths + themes: no overflow ------------------------ */
for (const w of [320, 390, 834]) {
  const p = await page(w, 800);
  await p.goto(`${SITE}/`, { waitUntil: 'networkidle' });
  check(`home ${w}px no overflow`, (await overflow(p)) <= 0, `${await overflow(p)}px`);
  await p.goto(`${SITE}/features/`, { waitUntil: 'load' });
  check(`directory ${w}px no overflow`, (await overflow(p)) <= 0, `${await overflow(p)}px`);
  await p.close();
}
for (const t of ['light', 'dark', 'amoled']) {
  const p = await page(1440, 1000);
  await p.goto(`${SITE}/`, { waitUntil: 'load' });
  await pickTheme(p, t);
  const applied = await p.evaluate(() => document.documentElement.dataset.theme);
  check(`theme ${t} applies`, applied === t, applied);
  check(`theme ${t} no overflow`, (await overflow(p)) <= 0);
  await p.close();
}

/* -------------------------------- report --------------------------------- */
console.log(`check-ui against ${SITE}`);
console.log(`screenshots -> ${OUT}`);
for (const r of results) console.log(` ${r}`);
const jsErrors = [...new Set(errors)];
if (jsErrors.length) {
  console.log(' JS errors:');
  for (const e of jsErrors) console.log(`   ${e}`);
}
await browser.close();
console.log(`\ncheck-ui: ${failures.length || jsErrors.length ? `${failures.length + jsErrors.length} FAILURE(S)` : 'all green'}`);
process.exit(failures.length || jsErrors.length ? 1 : 0);
