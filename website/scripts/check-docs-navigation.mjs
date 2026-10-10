#!/usr/bin/env node
/**
 * check-docs-navigation.mjs — docs UI regression: icon theme menu, grouped
 * sidebar disclosure tree, dark-mode Shiki code blocks.
 *
 *   node scripts/check-docs-navigation.mjs
 *   BASE_URL=http://127.0.0.1:4398/ReFra node scripts/check-docs-navigation.mjs
 *
 * Playwright resolution follows scripts/check-ui.mjs.
 */
import { mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const BASE = (process.env.BASE_URL ?? 'http://127.0.0.1:4398/ReFra').replace(/\/+$/, '');
const OUT = join(tmpdir(), 'refra-docs-nav-check');
const TOL = 1;

function resolvePlaywright() {
  const roots = [];
  if (process.env.CHECK_UI_PLAYWRIGHT) roots.push(process.env.CHECK_UI_PLAYWRIGHT);
  roots.push(process.cwd());
  for (const dir of roots) {
    const req = createRequire(join(dir, 'check-docs-navigation.mjs'));
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
  console.error('check-docs-navigation: playwright not found (see check-ui.mjs header)');
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

const overflow = (p) =>
  p.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);

const pickTheme = async (p, theme) => {
  await p.locator('.theme-trigger').click();
  await p.locator(`[data-theme-choice="${theme}"]`).click();
};

// --------------------------- theme menu ---------------------------
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${BASE}/`, { waitUntil: 'load' });

  const trigger = page.locator('.theme-trigger');
  if ((await trigger.count()) === 0) {
    check('theme trigger is a 44px icon-only control', false, 'no .theme-trigger (old select UI)');
    check('menu opens with expanded state', false, 'skipped — no trigger');
    await page.close();
  } else {
  const triggerBox = await trigger.boundingBox();
  check(
    'theme trigger is a 44px icon-only control',
    triggerBox && Math.abs(triggerBox.width - 44) <= TOL && Math.abs(triggerBox.height - 44) <= TOL &&
      (await trigger.textContent()).trim() === '' &&
      (await trigger.getAttribute('aria-label')) === 'Color theme' &&
      (await trigger.getAttribute('aria-haspopup')) === 'menu',
    triggerBox ? `box=${triggerBox.width}x${triggerBox.height}` : 'missing',
  );
  check('no visible theme label in header', (await trigger.textContent()).trim() === '');

  await trigger.click();
  const panel = page.locator('.theme-panel');
  await panel.waitFor({ state: 'visible' });
  check('menu opens with expanded state', (await trigger.getAttribute('aria-expanded')) === 'true');
  check(
    'all four choices are menuitemradio',
    (await page.locator('[data-theme-choice][role="menuitemradio"]').count()) === 4,
  );
  // Open focuses the currently-selected option.
  const focused = await page.evaluate(() => document.activeElement?.dataset?.themeChoice);
  check('open focuses current option', focused === 'auto', `focused=${focused}`);

  // Roving tabindex: only the selected option is in the tab order.
  const roving = await page.evaluate(() =>
    [...document.querySelectorAll('.theme-option')].map((o) => o.tabIndex).join(','),
  );
  check('roving tabindex: selected=0 rest=-1', roving === '0,-1,-1,-1', `tabindex=${roving}`);

  // Touch-friendly rows.
  const heights = await page
    .locator('.theme-option')
    .evaluateAll((els) => els.map((e) => Math.round(e.getBoundingClientRect().height)));
  check('options are 44px rows', heights.every((h) => h >= 44 - TOL), `heights=${heights.join(',')}`);

  // Keyboard focus carries a real outline, not just a tonal background.
  await page.keyboard.press('ArrowDown');
  const outline = await page.evaluate(() => {
    const cs = getComputedStyle(document.activeElement);
    return { style: cs.outlineStyle, width: cs.outlineWidth, cls: document.activeElement?.className };
  });
  check(
    'focus-visible option has a nonzero outline',
    outline.style !== 'none' && parseFloat(outline.width) > 0,
    JSON.stringify(outline),
  );
  // Roving tabindex tracks the focused option.
  const rovingMoved = await page.evaluate(() =>
    [...document.querySelectorAll('.theme-option')].map((o) => o.tabIndex).join(','),
  );
  check('roving tabindex follows arrow focus', rovingMoved === '-1,0,-1,-1', `tabindex=${rovingMoved}`);
  await page.keyboard.press('Enter');
  await page.waitForTimeout(150);
  check(
    'ArrowDown+Enter applies light',
    (await page.evaluate(() => document.documentElement.dataset.theme)) === 'light' &&
      (await page.evaluate(() => localStorage.getItem('refra-theme'))) === 'light',
  );
  check(
    'choice closes menu and refocuses trigger',
    (await trigger.getAttribute('aria-expanded')) === 'false' &&
      (await page.evaluate(() => document.activeElement?.classList?.contains('theme-trigger'))),
  );

  // Tab inside the open menu closes it and lands on the next focusable
  // control in native order (never body or a hidden menu option);
  // Shift+Tab lands on the previous one.
  const focusInfo = () =>
    page.evaluate(() => {
      const el = document.activeElement;
      const trig = document.querySelector('.theme-trigger');
      if (!el || el === document.body) return { tag: 'body', focusable: false, inPanel: false, rel: 0 };
      return {
        tag: `${el.tagName.toLowerCase()}.${String(el.className).split(' ')[0]}`,
        focusable: el.matches('a[href],button,summary,input,select,textarea,[tabindex]:not([tabindex="-1"])'),
        inPanel: !!el.closest('.theme-panel'),
        follows: (trig.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0,
        precedes: (trig.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_PRECEDING) !== 0,
      };
    });
  await trigger.click();
  await page.keyboard.press('Tab');
  await page.waitForTimeout(100);
  const tabInfo = await focusInfo();
  check(
    'Tab exits menu to next control in order',
    tabInfo.focusable && !tabInfo.inPanel && tabInfo.follows,
    JSON.stringify(tabInfo),
  );
  await trigger.click();
  await page.keyboard.press('Shift+Tab');
  await page.waitForTimeout(100);
  const shiftTabInfo = await focusInfo();
  check(
    'Shift+Tab exits menu to previous control in order',
    shiftTabInfo.focusable && !shiftTabInfo.inPanel && shiftTabInfo.precedes,
    JSON.stringify(shiftTabInfo),
  );
  // Selection + aria-checked + localStorage survive a route change.
  await pickTheme(page, 'amoled');
  await page.goto(`${BASE}/features/`, { waitUntil: 'load' });
  check(
    'theme persists across routes',
    (await page.evaluate(() => document.documentElement.dataset.theme)) === 'amoled' &&
      (await page.evaluate(() => localStorage.getItem('refra-theme'))) === 'amoled',
  );
  check(
    'aria-checked follows persisted theme',
    (await page.locator('[data-theme-choice="amoled"]').getAttribute('aria-checked')) === 'true',
  );

  // Escape closes and refocuses the trigger.
  await page.locator('.theme-trigger').click();
  await page.keyboard.press('Escape');
  check(
    'Escape closes menu + refocuses trigger',
    (await page.locator('.theme-trigger').getAttribute('aria-expanded')) === 'false' &&
      (await page.evaluate(() => document.activeElement?.classList?.contains('theme-trigger'))),
  );

  // Outside pointerdown closes without stealing focus.
  await page.locator('.theme-trigger').click();
  await page.mouse.click(200, 300);
  check(
    'outside click closes menu',
    (await page.locator('.theme-trigger').getAttribute('aria-expanded')) === 'false',
  );

  // Storage denied: data-theme still applies on choice.
  const denied = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    storageState: undefined,
  });
  const dp = await denied.newPage();
  await dp.addInitScript(() => {
    const orig = Storage.prototype.setItem;
    Storage.prototype.setItem = function (k, ...rest) {
      if (k === 'refra-theme') throw new DOMException('denied');
      return orig.call(this, k, ...rest);
    };
  });
  await dp.goto(`${BASE}/`, { waitUntil: 'load' });
  await dp.locator('.theme-trigger').click();
  await dp.locator('[data-theme-choice="dark"]').click();
  check(
    'storage denied still applies theme',
    (await dp.evaluate(() => document.documentElement.dataset.theme)) === 'dark',
  );
  await denied.close();

  // Auto tracks an emulated OS dark scheme — compare against the resolved
  // explicit-dark surface, not just "not white" (the light body isn't white
  // either, so a "!=" check is vacuous).
  const auto = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    colorScheme: 'dark',
  });
  const ap = await auto.newPage();
  await ap.goto(`${BASE}/`, { waitUntil: 'load' });
  const autoBg = await ap.evaluate(() => getComputedStyle(document.body).backgroundColor);
  await auto.close();
  const explicitDark = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const edp = await explicitDark.newPage();
  await edp.addInitScript(() => localStorage.setItem('refra-theme', 'dark'));
  await edp.goto(`${BASE}/`, { waitUntil: 'load' });
  const darkBg = await edp.evaluate(() => getComputedStyle(document.body).backgroundColor);
  await explicitDark.close();
  check(
    'auto + dark OS matches explicit dark surface',
    autoBg === darkBg && darkBg !== 'rgb(255, 255, 255)',
    `auto=${autoBg} dark=${darkBg}`,
  );

  // prefers-reduced-transparency collapses the translucent/blurred panel
  // to a solid surface container. The reducedTransparency context option is
  // silently ignored by this Chromium build — emulate via CDP instead.
  const rt = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const rp = await rt.newPage();
  const cdp = await rt.newCDPSession(rp);
  await cdp.send('Emulation.setEmulatedMedia', {
    features: [{ name: 'prefers-reduced-transparency', value: 'reduce' }],
  });
  await rp.goto(`${BASE}/`, { waitUntil: 'load' });
  await rp.locator('.theme-trigger').click();
  const rtStyle = await rp.locator('.theme-panel').evaluate((el) => {
    const cs = getComputedStyle(el);
    return { backdrop: cs.backdropFilter, bg: cs.backgroundColor };
  });
  check(
    'reduced transparency: solid opaque panel',
    (rtStyle.backdrop === 'none' || rtStyle.backdrop === '') && /^rgb\(/.test(rtStyle.bg),
    JSON.stringify(rtStyle),
  );
  await rt.close();

  await page.close();
  }
}

// --------------------------- sidebar grouping ---------------------------
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${BASE}/features/first-launch`, { waitUntil: 'load' });

  const nav = page.locator('nav.docs-nav');
  const hrefs = await nav.locator('a.nav-item').evaluateAll((els) => els.map((a) => a.getAttribute('href')));
  const dupes = hrefs.filter((h, i) => hrefs.indexOf(h) !== i);
  check('sidebar link hrefs all unique', dupes.length === 0, `dupes=${[...new Set(dupes)].join(',')}`);

  const firstLaunch = nav.locator('a[href$="/features/first-launch"]');
  check(
    'First launch appears exactly once',
    (await firstLaunch.count()) === 1,
    `count=${await firstLaunch.count()}`,
  );
  const mainNav = nav.locator('a[href$="/features/main-navigation"]');
  check(
    'Main navigation appears exactly once',
    (await mainNav.count()) === 1,
    `count=${await mainNav.count()}`,
  );
  check(
    'first-launch link is current',
    (await firstLaunch.first().getAttribute('aria-current')) === 'page',
  );
  const groupCount = await page.locator('.nav-group').count();
  check('sidebar renders grouped disclosures', groupCount >= 5, `groups=${groupCount}`);
  if (groupCount > 0) {
    check(
      'active nav-group is open',
      await page
        .locator('.nav-group[data-nav-group="getting-started"]')
        .evaluate((el) => el.open),
    );

    // Other groups exist closed; toggling works via summary click + keyboard.
    const guides = page.locator('.nav-group[data-nav-group="guides"]');
    check('inactive group starts closed', !(await guides.evaluate((el) => el.open)));
    await guides.locator('summary').click();
    check('summary click toggles group', await guides.evaluate((el) => el.open));
    await guides.locator('summary').press('Enter'); // Enter on focused summary toggles too
    check('keyboard Enter toggles group', !(await guides.evaluate((el) => el.open)));
  }

  // Every feature directory entry still has a sidebar link (pins moved to
  // Getting started count once; total links = directory rows + non-feature items).
  const dirPage = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await dirPage.goto(`${BASE}/features/`, { waitUntil: 'load' });
  const featureRows = await dirPage.locator('.feature-row').count();
  const sidebarFeatureLinks = hrefs.filter((h) => h.includes('/features/'));
  check(
    'all feature pages remain linked once each',
    sidebarFeatureLinks.length === featureRows,
    `sidebar=${sidebarFeatureLinks.length} directory=${featureRows}`,
  );
  await dirPage.close();

  check('desktop: no document h-scroll', (await overflow(page)) <= 0);

  mkdirSync(OUT, { recursive: true });
  await page.locator('.docs-browse').screenshot({ path: `${OUT}/firstlaunch-sidebar.png` });
  await page.close();
}

// --------------------------- mobile disclosure ---------------------------
{
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await page.goto(`${BASE}/features/first-launch`, { waitUntil: 'load' });
  const browse = page.locator('.docs-browse');
  const summary = browse.locator('summary.docs-browse-summary');
  check('mobile: outer disclosure visible', await summary.isVisible());
  await summary.click();
  check(
    'mobile: disclosure opens groups',
    await browse.evaluate((el) => el.open) &&
      (await page.locator('.nav-group').count()) > 0,
  );
  check('mobile: no document h-scroll', (await overflow(page)) <= 0);
  await page.close();
}

// --------------------------- dark syntax highlighting ---------------------------
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${BASE}/build`, { waitUntil: 'load' });
  const pre = page.locator('pre.astro-code').first();
  check('build page has shiki code block', (await pre.count()) === 1);

  const sample = async () => {
    const preColor = await pre.evaluate((el) => getComputedStyle(el).backgroundColor);
    // First span carrying the dual --shiki-dark var — resolves that var in a
    // probe so the token color is checked against its own declared value
    // (an arbitrary first .line span only proves inherited <pre> text color).
    const token = await pre.locator('span[style*="--shiki-dark"]').first().evaluate((el) => {
      const cs = getComputedStyle(el);
      const probe = el.ownerDocument.createElement('span');
      probe.style.color = cs.getPropertyValue('--shiki-dark').trim() || 'rgb(0,0,0)';
      el.appendChild(probe);
      const expected = getComputedStyle(probe).color;
      probe.remove();
      return { color: cs.color, expected };
    });
    return { preColor, token };
  };

  const light = await sample();
  check(
    'light theme: code block is light',
    light.preColor === 'rgb(255, 255, 255)' || light.preColor === 'rgb(250, 253, 255)',
    `pre=${light.preColor}`,
  );

  // Drive data-theme directly — this section tests the CSS contract, not the
  // menu, and must still repro a white code block on older builds.
  const setTheme = (t) =>
    page.evaluate((v) => { document.documentElement.dataset.theme = v; }, t);

  await setTheme('dark');
  await page.waitForTimeout(150);
  const dark = await sample();
  check(
    'dark theme: code block recolors without reload',
    dark.preColor !== light.preColor && dark.preColor === 'rgb(36, 41, 46)',
    `pre=${dark.preColor} token=${dark.token.color}`,
  );
  check(
    'dark theme: tokens use resolved shiki-dark values',
    dark.token.color === dark.token.expected && dark.token.color !== light.token.color,
    `token=${dark.token.color} var=${dark.token.expected} light=${light.token.color}`,
  );

  await setTheme('amoled');
  await page.waitForTimeout(150);
  const amoled = await sample();
  check('amoled keeps dark code palette', amoled.preColor === dark.preColor, `pre=${amoled.preColor}`);

  // Explicit light must stay light even under a dark OS scheme.
  const osDark = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    colorScheme: 'dark',
  });
  const lp = await osDark.newPage();
  await lp.addInitScript(() => localStorage.setItem('refra-theme', 'light'));
  await lp.goto(`${BASE}/build`, { waitUntil: 'load' });
  const explicitLight = await lp
    .locator('pre.astro-code')
    .first()
    .evaluate((el) => getComputedStyle(el).backgroundColor);
  check(
    'explicit light stays light under dark OS',
    explicitLight === 'rgb(255, 255, 255)' || explicitLight === light.preColor,
    `pre=${explicitLight}`,
  );
  await osDark.close();

  // auto + dark OS picks the dark palette.
  const autoDark = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    colorScheme: 'dark',
  });
  const ad = await autoDark.newPage();
  await ad.addInitScript(() => localStorage.removeItem('refra-theme'));
  await ad.goto(`${BASE}/build`, { waitUntil: 'load' });
  const autoPre = await ad
    .locator('pre.astro-code')
    .first()
    .evaluate((el) => getComputedStyle(el).backgroundColor);
  check('auto + dark OS uses dark code palette', autoPre === 'rgb(36, 41, 46)', `pre=${autoPre}`);
  await autoDark.close();

  // Build-doc factual content checks (lead-authored page) — scan the whole
  // article, not just the first code block.
  const pageText = await page.locator('article').first().textContent();
  check('build doc names the app module task', /assembleArm64-v8aNoMLDebug|:app:androidApp/.test(pageText));
  check('build doc documents CARTO_BASEMAP_KEY', /CARTO_BASEMAP_KEY/.test(pageText));
  check('build doc drops obsolete :app:assemble', !/:app:assemble[^a-zA-Z]/.test(pageText));

  // Long code lines stay inside the block — no page-level h-scroll.
  check('build page no document h-scroll', (await overflow(page)) <= 0);
  const preOverflows = await pre.evaluate((el) => el.scrollWidth <= el.clientWidth + 1 || getComputedStyle(el).overflowX !== 'visible');
  check('code block contains horizontal overflow', preOverflows === true);

  mkdirSync(OUT, { recursive: true });
  await page.evaluate(() => { document.documentElement.dataset.theme = 'amoled'; });
  await page.screenshot({ path: `${OUT}/build-amoled.png` });
  await page.close();
}

// --------------------------- screenshots for review ---------------------------
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${BASE}/`, { waitUntil: 'load' });
  mkdirSync(OUT, { recursive: true });
  if ((await page.locator('.theme-trigger').count()) > 0) {
    await page.locator('.theme-trigger').click();
    await page.locator('header').screenshot({ path: `${OUT}/theme-open-desktop.png` });
  } else {
    await page.locator('header').screenshot({ path: `${OUT}/theme-open-desktop.png` });
  }
  await page.close();

  const mp = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await mp.goto(`${BASE}/`, { waitUntil: 'load' });
  let panel = null;
  if ((await mp.locator('.theme-trigger').count()) > 0) {
    await mp.locator('.theme-trigger').click();
    await mp.screenshot({ path: `${OUT}/theme-open-mobile.png` });
    panel = await mp.locator('.theme-panel').boundingBox();
  }
  // Menu must stay inside the viewport on a narrow screen.
  check(
    'mobile: theme menu stays inside viewport',
    !!panel && panel.x >= 0 && panel.x + panel.width <= 390 + TOL,
    panel ? `x=${panel.x} w=${panel.width}` : 'no panel',
  );
  check('mobile: no document h-scroll', (await overflow(mp)) <= 0);
  await mp.close();
}

await browser.close();
results.forEach((r) => console.log(r));
console.log(`\n${results.length - failures.length} passed, ${failures.length} failed — shots in ${OUT}`);
process.exit(failures.length ? 1 : 0);
