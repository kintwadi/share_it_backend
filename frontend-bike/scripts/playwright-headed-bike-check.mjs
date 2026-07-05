import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://localhost:4300';
const artifactsDir = path.resolve('playwright-artifacts');

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function run() {
  await ensureDir(artifactsDir);

  const browser = await chromium.launch({
    headless: false,
    channel: 'chrome'
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 }
  });
  const page = await context.newPage();

  try {
    await page.goto(baseUrl + '/#/login', { waitUntil: 'networkidle' });
    await page.fill('input[name="email"]', 'linda.lender@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL(/#\/bikes/, { timeout: 15000 });

    await page.goto(baseUrl + '/#/fahrad-fuchs', { waitUntil: 'networkidle' });
    await page.waitForSelector('.storefront-shell', { timeout: 30000 });
    const cards = page.locator('.bike-card');
    await cards.first().waitFor({ timeout: 30000 });
    const cardCount = await cards.count();
    const firstTitle = (await page.locator('.bike-card h3').first().textContent())?.trim() || '';

    const detailHref = await page.locator('.bike-card a').first().getAttribute('href');
    if (!detailHref) {
      throw new Error('Missing Fahrad-Fuchs detail link.');
    }
    await page.goto(baseUrl + detailHref, { waitUntil: 'networkidle' });
    await page.waitForSelector('.detail-shell h1', { timeout: 30000 });
    const detailTitle = (await page.locator('.detail-shell h1').textContent())?.trim() || '';

    const checkoutHref = await page.locator('.reserve-button').getAttribute('href');
    if (!checkoutHref) {
      throw new Error('Missing Fahrad-Fuchs checkout link.');
    }
    await page.locator('.reserve-button').click();
    await page.waitForURL(/#\/fahrad-fuchs\/checkout\//, { timeout: 30000 });
    await page.waitForSelector('.checkout-shell', { timeout: 30000 });
    const checkoutTitle = (await page.locator('.checkout-shell h1').textContent())?.trim() || '';

    await page.screenshot({
      path: path.join(artifactsDir, 'frontend-bike-headed-smoke.png'),
      fullPage: true
    });

    console.log(JSON.stringify({
      login: 'ok',
      fahradFuchsPage: 'ok',
      cardCount,
      firstTitle,
      detailTitle,
      checkoutTitle
    }, null, 2));
  } finally {
    await context.close();
    await browser.close();
  }
}

run().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
