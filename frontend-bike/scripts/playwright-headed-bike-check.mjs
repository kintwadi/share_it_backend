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

    await page.goto(baseUrl + '/#/bikes', { waitUntil: 'networkidle' });
    const cards = page.locator('.card');
    await cards.first().waitFor({ timeout: 15000 });
    const cardCount = await cards.count();
    const firstTitle = (await page.locator('.card h2, .card h3').first().textContent())?.trim() || '';

    await cards.first().click();
    await page.waitForSelector('.bike-detail h1', { timeout: 15000 });
    const detailTitle = (await page.locator('.bike-detail h1').textContent())?.trim() || '';

    await page.screenshot({
      path: path.join(artifactsDir, 'frontend-bike-headed-smoke.png'),
      fullPage: true
    });

    console.log(JSON.stringify({
      login: 'ok',
      bikesPage: 'ok',
      cardCount,
      firstTitle,
      detailTitle
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
