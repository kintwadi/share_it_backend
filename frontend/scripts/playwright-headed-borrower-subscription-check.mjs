import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const baseUrl = 'http://127.0.0.1:4200';
const listingId = '3a815a72-3399-4a0c-8bb8-f0d57731a906';
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const frontendDir = path.resolve(scriptDir, '..');
const artifactsDir = path.resolve(frontendDir, 'playwright-artifacts');

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function run() {
  await ensureDir(artifactsDir);

  const browser = await chromium.launch({
    headless: false,
    slowMo: 250
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 }
  });
  const page = await context.newPage();
  const apiFailures = [];
  const reportPath = path.join(artifactsDir, 'borrower-subscription-continue-report.json');

  page.on('response', async (response) => {
    const status = response.status();
    if (status === 401 || status === 403) {
      apiFailures.push({
        status,
        url: response.url()
      });
    }
  });

  try {
    await page.goto(`${baseUrl}/#/connect`, { waitUntil: 'domcontentloaded' });
    await page.fill('input[name="email"]', 'bob.borrower@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.locator('input[name="password"]').press('Enter');
    await page.waitForURL(/#\/dashboard$/, { timeout: 20000 });

    await page.goto(`${baseUrl}/#/listing/${listingId}/book`, { waitUntil: 'networkidle' });
    await page.waitForSelector('text=Borrowing Options', { timeout: 20000 });
    await page.waitForSelector('text=Start Verified Trial', { timeout: 20000 });

    const beforeContinueUrl = page.url();
    await page.getByRole('button', { name: 'Continue' }).click();
    await page.waitForTimeout(5000);
    const afterContinueUrl = page.url();

    const screenshotPath = path.join(artifactsDir, 'borrower-subscription-continue-result.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });

    const report = {
      listingId,
      beforeContinueUrl,
      afterContinueUrl,
      apiFailures,
      screenshotPath
    };
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2), 'utf8');
    console.log(JSON.stringify(report, null, 2));
  } finally {
    await context.close();
    await browser.close();
  }
}

run().catch((error) => {
  const reportPath = path.resolve('playwright-artifacts', 'borrower-subscription-continue-report.json');
  fs.writeFile(reportPath, JSON.stringify({
    error: String(error?.stack || error?.message || error)
  }, null, 2), 'utf8').catch(() => {});
  console.error(error);
  process.exitCode = 1;
});
