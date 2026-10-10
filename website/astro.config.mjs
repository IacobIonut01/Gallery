// @ts-check
import { defineConfig } from 'astro/config';
import mdx from '@astrojs/mdx';
import sitemap from '@astrojs/sitemap';

// GitHub Pages project site: https://iacobionut01.github.io/ReFra/
// Override for a custom domain or a fork: DOCS_BASE=/ DOCS_SITE=https://example.com
const site = process.env.DOCS_SITE ?? 'https://iacobionut01.github.io';
const base = process.env.DOCS_BASE ?? '/ReFra';

export default defineConfig({
  site,
  base,
  output: 'static',
  trailingSlash: 'ignore',
  integrations: [mdx(), sitemap()],
  markdown: {
    shikiConfig: {
      themes: { light: 'github-light', dark: 'github-dark' },
    },
  },
});
