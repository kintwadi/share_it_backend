import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://localhost:4200';
const artifactsDir = path.resolve('playwright-artifacts');

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function run() {
  await ensureDir(artifactsDir);

  const browser = await chromium.launch({
    headless: false,
    slowMo: 200
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 }
  });
  const page = await context.newPage();

  try {
    await page.goto(`${baseUrl}/#/connect`, { waitUntil: 'domcontentloaded' });
    await page.fill('input[name="email"]', 'linda.lender@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.locator('input[name="password"]').press('Enter');
    await page.waitForURL(/#\/dashboard$/, { timeout: 20000 });

    await page.goto(`${baseUrl}/#/bikes`, { waitUntil: 'networkidle' });
    await page.waitForSelector('.card', { timeout: 15000 });

    const cardCount = await page.locator('.card').count();
    if (cardCount < 3) {
      throw new Error(`Expected at least 3 bike cards but found ${cardCount}`);
    }

    const firstTitle = (await page.locator('.card h2').first().textContent())?.trim() || '';
    await page.locator('.card').first().click();
    await page.waitForSelector('.bike-detail', { timeout: 15000 });
    await page.waitForSelector('app-handover-checklist, .checklist', { timeout: 15000 });
    await page.waitForSelector('app-rent-to-own-calculator, .calculator', { timeout: 15000 });

    const screenshotPath = path.join(artifactsDir, 'bike-headed-smoke.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });

    console.log(JSON.stringify({
      login: 'ok',
      bikesPage: 'ok',
      firstBikeTitle: firstTitle,
      cardCount,
      screenshotPath
    }, null, 2));
  } finally {
    await context.close();
    await browser.close();
  }
}

run().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
