#!/usr/bin/env node
/**
 * check-preview-scale.mjs — LiveEmbed logical-size scaling regression.
 *
 * The homepage hero mounts a wasm scene inside PhoneFrame. The iframe must
 * render at the device preset's logical size (412x915 phone) and be
 * CSS-transformed down to the stage, never resized to the stage itself —
 * a resized iframe shrinks the app's viewport and inflates its UI.
 *
 * Usage:
 *   node scripts/check-preview-scale.mjs
 *   BASE_URL=http://127.0.0.1:4322/ReFra node scripts/check-preview-scale.mjs
 *
 * Playwright resolution follows scripts/check-ui.mjs.
 */
import { mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import { PHONE_STATUS_BAR_HEIGHT } from '../src/lib/constants.ts';

const BASE = (process.env.BASE_URL ?? 'http://127.0.0.1:4398/ReFra').replace(/\/+$/, '');
const OUT = join(tmpdir(), 'refra-scale-check');
const PHONE = { width: 412, height: 915 };
const WIDTHS = [1440, 1024, 390, 320];
const TOL = 1;

function resolvePlaywright() {
  const roots = [];
  if (process.env.CHECK_UI_PLAYWRIGHT) roots.push(process.env.CHECK_UI_PLAYWRIGHT);
  roots.push(process.cwd());
  for (const dir of roots) {
    const req = createRequire(join(dir, 'check-preview-scale.mjs'));
    for (const name of ['playwright', 'playwright-core']) {
      try {
        const pw = req(name);
        if (pw?.chromium) return pw;
      } catch {
        /* next candidate */
      }
    }
  }
  return null;
}

const pw = resolvePlaywright();
if (!pw) {
  console.error('check-preview-scale: playwright not found (see check-ui.mjs header for install hints)');
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

const measure = (page) =>
  page.evaluate(() => {
    const embed = document.querySelector('live-embed');
    const stage = embed?.querySelector('.embed-stage');
    const iframe = stage?.querySelector('iframe.embed-media');
    if (!embed || !stage || !iframe) return null;
    const r = iframe.getBoundingClientRect();
    const s = stage.getBoundingClientRect();
    let iw = 0;
    let ih = 0;
    try {
      iw = iframe.contentWindow.innerWidth;
      ih = iframe.contentWindow.innerHeight;
    } catch {
      /* cross-origin */
    }
    return {
      ready: embed.ready === true,
      iframeW: iframe.offsetWidth,
      iframeH: iframe.offsetHeight,
      iw,
      ih,
      rect: { x: r.x, y: r.y, w: r.width, h: r.height },
      stageRect: { x: s.x, y: s.y, w: s.width, h: s.height },
      stageClient: { w: stage.clientWidth, h: stage.clientHeight },
    };
  });

async function assertFit(page, label) {
  const m = await measure(page);
  check(`${label}: iframe exists`, !!m);
  if (!m) return null;
  check(
    `${label}: iframe viewport 412x915`,
    Math.abs(m.iw - PHONE.width) <= TOL && Math.abs(m.ih - PHONE.height) <= TOL,
    `innerWidth/Height=${m.iw}x${m.ih}, offset=${m.iframeW}x${m.iframeH}`,
  );
  const scale = Math.min(
    m.stageClient.w / PHONE.width,
    m.stageClient.h / PHONE.height,
  );
  const expW = PHONE.width * scale;
  const expH = PHONE.height * scale;
  const scaleX = m.rect.w / PHONE.width;
  const scaleY = m.rect.h / PHONE.height;
  check(
    `${label}: equal X/Y scale`,
    Math.abs(scaleX - scaleY) * PHONE.width <= TOL,
    `scaleX=${scaleX.toFixed(4)} scaleY=${scaleY.toFixed(4)}`,
  );
  check(
    `${label}: visual size = logical*fit`,
    Math.abs(m.rect.w - expW) <= TOL && Math.abs(m.rect.h - expH) <= TOL,
    `rect=${m.rect.w.toFixed(2)}x${m.rect.h.toFixed(2)} want=${expW.toFixed(2)}x${expH.toFixed(2)} scale=${scale.toFixed(4)}`,
  );
  check(
    `${label}: iframe inside stage`,
    m.rect.x >= m.stageRect.x - TOL &&
      m.rect.y >= m.stageRect.y - TOL &&
      m.rect.x + m.rect.w <= m.stageRect.x + m.stageRect.w + TOL &&
      m.rect.y + m.rect.h <= m.stageRect.y + m.stageRect.h + TOL,
    `iframe=${JSON.stringify(m.rect)} stage=${JSON.stringify(m.stageRect)}`,
  );
  const hscroll = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1,
  );
  check(`${label}: no page h-scroll`, !hscroll);
  return m;
}

// ---------- live scaling across widths ----------
{
  const page = await browser.newPage({ viewport: { width: WIDTHS[0], height: 900 } });
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  await page.goto(`${BASE}/`, { waitUntil: 'load' });
  await page.waitForFunction(
    () => document.querySelector('live-embed')?.ready === true,
    undefined,
    { timeout: 90_000 },
  );

  // Tag the iframe node — resizes must refit, not remount.
  await page.evaluate(() => {
    const f = document.querySelector('live-embed .embed-stage iframe.embed-media');
    if (f) f.dataset.scaleProbe = 'same-node';
  });

  const statusbarInfo = () =>
    page.evaluate((statusBarDp) => {
      const bars = document.querySelectorAll('.phone-screen .phone-statusbar');
      const bar = bars[0];
      const screen = document.querySelector('.phone-screen');
      if (!bar || !screen) return { count: bars.length };
      const r = bar.getBoundingClientRect();
      const s = screen.getBoundingClientRect();
      const timeEl = bar.querySelector('.phone-statusbar-time');
      const iconsEl = bar.querySelector('.phone-statusbar-icons');
      const tr = timeEl?.getBoundingClientRect();
      const ir = iconsEl?.getBoundingClientRect();
      const gestures = document.querySelectorAll('.phone-screen .phone-gesture');
      const g = gestures[0]?.getBoundingClientRect();
      const iframe = document.querySelector(
        'live-embed .embed-stage iframe.embed-media',
      );
      const perDp = s.width / 412;
      return {
        count: bars.length,
        ariaHidden: bar.getAttribute('aria-hidden'),
        time: timeEl?.textContent,
        icons: bar.querySelectorAll('svg').length,
        pointerEvents: getComputedStyle(bar).pointerEvents,
        height: r.height,
        expectedH: (s.width * statusBarDp) / 412,
        inside: r.top >= s.top - 1 && r.bottom <= s.bottom + 1,
        clockTopDp: tr ? (tr.top - s.top) / perDp : null,
        clockLeftDp: tr ? (tr.left - s.left) / perDp : null,
        iconsRightDp: ir ? (s.right - ir.right) / perDp : null,
        statusBarQuery: iframe
          ? new URL(iframe.src).searchParams.get('statusBar')
          : null,
        statusBarHeightQuery: iframe
          ? new URL(iframe.src).searchParams.get('statusBarHeight')
          : null,
        gestureCount: gestures.length,
        gesture: g
          ? {
              pointerEvents: getComputedStyle(gestures[0]).pointerEvents,
              centerXDelta: Math.abs(g.left + g.width / 2 - (s.left + s.width / 2)),
              bottomDp: (s.bottom - g.bottom) / perDp,
              inside: g.top >= s.top - 1 && g.bottom <= s.bottom + 1,
            }
          : null,
      };
    }, PHONE_STATUS_BAR_HEIGHT);
  const assertStatusbar = async (label) => {
    const sb = await statusbarInfo();
    check(`${label}: exactly one statusbar`, sb.count === 1, `count=${sb.count}`);
    if (sb.count !== 1) return;
    check(`${label}: statusbar decorative`, sb.ariaHidden === 'true');
    check(
      `${label}: statusbar content`,
      sb.time === '9:41' && sb.icons === 3,
      `time=${sb.time} icons=${sb.icons}`,
    );
    check(`${label}: statusbar pointer-events none`, sb.pointerEvents === 'none');
    check(`${label}: statusbar inside phone`, sb.inside === true);
    check(
      `${label}: statusbar height = ${PHONE_STATUS_BAR_HEIGHT}dp`,
      Math.abs(sb.height - sb.expectedH) <= TOL,
      `h=${sb.height?.toFixed(2)} want=${sb.expectedH?.toFixed(2)}`,
    );
    check(
      `${label}: clock top >= 24dp`,
      sb.clockTopDp != null && sb.clockTopDp >= 24 - TOL,
      `clockTop=${sb.clockTopDp?.toFixed(1)}dp`,
    );
    check(
      `${label}: 32dp horizontal clearance`,
      sb.clockLeftDp != null &&
        sb.iconsRightDp != null &&
        Math.abs(sb.clockLeftDp - 32) <= TOL &&
        Math.abs(sb.iconsRightDp - 32) <= TOL,
      `left=${sb.clockLeftDp?.toFixed(1)} right=${sb.iconsRightDp?.toFixed(1)}`,
    );
    check(
      `${label}: iframe requests statusBar height`,
      sb.statusBarQuery === 'true' &&
        sb.statusBarHeightQuery === String(PHONE_STATUS_BAR_HEIGHT),
      `statusBar=${sb.statusBarQuery} statusBarHeight=${sb.statusBarHeightQuery}`,
    );
    check(`${label}: exactly one gesture indicator`, sb.gestureCount === 1);
    if (sb.gesture) {
      check(
        `${label}: gesture indicator decorative/positioned`,
        sb.gesture.pointerEvents === 'none' &&
          sb.gesture.inside &&
          sb.gesture.centerXDelta <= TOL &&
          Math.abs(sb.gesture.bottomDp - 10) <= TOL,
        JSON.stringify(sb.gesture),
      );
    }
  };

  for (const w of WIDTHS) {
    await page.setViewportSize({ width: w, height: 900 });
    await page.waitForTimeout(400); // ResizeObserver tick
    await assertFit(page, `w${w}`);
    const same = await page.evaluate(
      () =>
        document.querySelector('live-embed .embed-stage iframe.embed-media')
          ?.dataset.scaleProbe === 'same-node',
    );
    check(`w${w}: iframe not remounted`, same);
    await assertStatusbar(`w${w} light`);
  }

  // Dark theme — same overlay must still render inside the phone.
  {
    const darkCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const darkPage = await darkCtx.newPage();
    await darkPage.addInitScript(() => localStorage.setItem('refra-theme', 'dark'));
    await darkPage.goto(`${BASE}/`, { waitUntil: 'load' });
    await darkPage.waitForFunction(
      () => document.querySelector('live-embed')?.ready === true,
      undefined,
      { timeout: 90_000 },
    );
    await darkPage.waitForTimeout(400);
    const dm = await measure(darkPage);
    check(
      'dark: iframe viewport 412x915',
      dm && Math.abs(dm.iw - PHONE.width) <= TOL && Math.abs(dm.ih - PHONE.height) <= TOL,
      dm ? `iw=${dm.iw}x${dm.ih}` : 'no iframe',
    );
    // statusbarInfo is bound to `page` — re-evaluate against the dark page
    const sb = await darkPage.evaluate((statusBarDp) => {
      const bars = document.querySelectorAll('.phone-screen .phone-statusbar');
      const bar = bars[0];
      const screen = document.querySelector('.phone-screen');
      if (!bar || !screen) return { count: bars.length };
      const r = bar.getBoundingClientRect();
      const s = screen.getBoundingClientRect();
      return {
        count: bars.length,
        ariaHidden: bar.getAttribute('aria-hidden'),
        icons: bar.querySelectorAll('svg').length,
        pointerEvents: getComputedStyle(bar).pointerEvents,
        height: r.height,
        expectedH: (s.width * statusBarDp) / 412,
        inside: r.top >= s.top - 1 && r.bottom <= s.bottom + 1,
      };
    }, PHONE_STATUS_BAR_HEIGHT);
    check('dark: exactly one statusbar', sb.count === 1);
    check(
      'dark: statusbar decorative + sized',
      sb.ariaHidden === 'true' &&
        sb.icons === 3 &&
        sb.pointerEvents === 'none' &&
        sb.inside === true &&
        Math.abs(sb.height - sb.expectedH) <= TOL,
      JSON.stringify(sb),
    );
    await darkCtx.close();
  }

  // Transformed ancestor: an extra scale(.75) on phone-screen must show up as
  // exactly ONE extra factor on the visual rect — the logical size and the
  // fit math must not double-count it.
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.waitForTimeout(300);
  const before = await measure(page);
  await page.evaluate(() => {
    const screen = document.querySelector('.phone-screen');
    screen.style.transformOrigin = 'top left';
    screen.style.transform = 'scale(.75)';
  });
  await page.waitForTimeout(300);
  const scaled = await measure(page);
  if (before && scaled) {
    check(
      'transformed ancestor: logical size unchanged',
      Math.abs(scaled.iw - PHONE.width) <= TOL && scaled.iframeW === before.iframeW,
      `iw=${scaled.iw} offsetW=${scaled.iframeW}`,
    );
    check(
      'transformed ancestor: exactly one extra .75 visual factor',
      Math.abs(scaled.rect.w - before.rect.w * 0.75) <= TOL &&
        Math.abs(scaled.rect.h - before.rect.h * 0.75) <= TOL,
      `${before.rect.w.toFixed(2)} -> ${scaled.rect.w.toFixed(2)}`,
    );
  }
  await page.evaluate(() => {
    document.querySelector('.phone-screen').style.transform = '';
  });
  await page.waitForTimeout(300);

  // Pointer mapping: click the middle bottom-nav tab (Albums) at scaled
  // coordinates and compare ONLY the iframe's own pixels — the page-level
  // animated hero glow would make a full-page screenshot differ regardless.
  const m = await measure(page);
  const iframeEl = page.locator('live-embed .embed-stage iframe.embed-media');
  const frameBefore = await iframeEl.screenshot();
  const scale = m.rect.w / PHONE.width;
  await page.mouse.click(
    m.rect.x + (PHONE.width / 2) * scale,
    m.rect.y + (PHONE.height - 88) * scale,
  );
  await page.waitForTimeout(1200);
  const frameAfter = await iframeEl.screenshot();
  check(
    'albums nav click changes iframe pixels',
    !frameBefore.equals(frameAfter),
    `clicked logical (${PHONE.width / 2},${PHONE.height - 88})`,
  );
  mkdirSync(OUT, { recursive: true });
  const { writeFileSync } = await import('node:fs');
  writeFileSync(`${OUT}/nav-before.png`, frameBefore);
  writeFileSync(`${OUT}/nav-after.png`, frameAfter);

  // Seam check: the status-bar safe area must be content padding, not a whole-
  // scene inset — the FloatingTopBarScrim gradient paints continuously behind
  // it. Sample a narrow left-edge column band straddling the 64dp boundary.
  {
    const req = createRequire(join(process.cwd(), 'check-preview-scale.mjs'));
    const { PNG } = req('pngjs');
    const png = PNG.sync.read(await iframeEl.screenshot());
    const pxPerDp = png.width / PHONE.width;
    const x0 = Math.round(8 * pxPerDp);
    const x1 = Math.round(24 * pxPerDp);
    const bandMean = (r0, r1) => {
      let r = 0, g = 0, b = 0, n = 0;
      for (let y = Math.round(r0 * pxPerDp); y < Math.round(r1 * pxPerDp); y++) {
        for (let x = x0; x < x1; x++) {
          const i = (y * png.width + x) << 2;
          r += png.data[i]; g += png.data[i + 1]; b += png.data[i + 2]; n++;
        }
      }
      return [r / n, g / n, b / n];
    };
    const above = bandMean(62, 64);
    const below = bandMean(64, 66);
    const delta = Math.max(...above.map((v, i) => Math.abs(v - below[i])));
    check(
      'scrim: no seam at statusbar boundary',
      delta <= 4,
      `max channel delta ${delta.toFixed(1)} above=[${above.map((v) => v.toFixed(0))}] below=[${below.map((v) => v.toFixed(0))}]`,
    );
  }

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${OUT}/refra-scale-after-desktop.png` });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(300);
  await page.locator('.hero-preview').screenshot({ path: `${OUT}/refra-scale-after-mobile.png` });
  check('desktop: no JS errors', errors.length === 0, errors.slice(0, 2).join(' | '));
  await page.close();
}

// ---------- capture fallback fills the stage ----------
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.route('**/live/index.html*', (r) => r.abort());
  await page.goto(`${BASE}/`, { waitUntil: 'load' });
  const img = page.locator('live-embed .embed-stage img.embed-media');
  await img.waitFor({ state: 'attached', timeout: 30_000 });
  await img.evaluate(
    (el) =>
      el.complete ||
      new Promise((res) => {
        el.addEventListener('load', res, { once: true });
        el.addEventListener('error', res, { once: true });
      }),
  );
  const m = await page.evaluate(() => {
    const stage = document.querySelector('live-embed .embed-stage');
    const el = stage?.querySelector('img.embed-media');
    if (!stage || !el) return null;
    const r = el.getBoundingClientRect();
    const s = stage.getBoundingClientRect();
    return { rw: r.width, rh: r.height, sw: s.width, sh: s.height, complete: el.complete, nw: el.naturalWidth };
  });
  check('fallback: capture media mounted', !!m && m.complete && m.nw > 0, JSON.stringify(m));
  if (m) {
    check(
      'fallback: fills stage without crop/stretch',
      Math.abs(m.rw - m.sw) <= TOL && Math.abs(m.rh - m.sh) <= TOL,
      `media=${m.rw.toFixed(2)}x${m.rh.toFixed(2)} stage=${m.sw.toFixed(2)}x${m.sh.toFixed(2)}`,
    );
  }
  await page.close();
}

await browser.close();
results.forEach((r) => console.log(r));
console.log(`\n${results.length - failures.length} passed, ${failures.length} failed — shots in ${OUT}`);
process.exit(failures.length ? 1 : 0);
