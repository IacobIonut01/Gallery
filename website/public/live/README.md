# live/

The wasmJs live bundle produced by `:app:docs:showcase:wasmJsBrowserDistribution`
is published here. `LiveEmbed` and `PreviewCollection` frame it as
`live/index.html?scene=&theme=&device=&state=`.

## Publishing locally

```bash
cd website
npm run previews:live
```

The publisher builds the Gradle distribution, copies it into this directory
(keeping this README), serves it locally, and probes it with Playwright:
the bundle must post a valid `docs:scenes` catalog and `docs:ready` for
`?scene=settings-appearance`, and render a non-blank canvas. Only then is
`docs-live.json` written — the catalog the manifest transform trusts for
`live` fidelity. A failed probe exits non-zero and leaves the previous
catalog in place.

Prereqs: `npm install` in `website/` plus `npx playwright install chromium`
(a cached chromium build is reused when present).

Build artifacts are gitignored; until the bundle lands, embeds resolve scenes
through `docs-manifest.json` and serve the capture tier or a pending
placeholder.
