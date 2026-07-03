const { test, expect } = require('@playwright/test');

const sessionStorageKey = 'frontend-bike.session';
const account = {
  email: 'linda.lender@example.com',
  password: 'password123'
};

async function authenticate(request) {
  const response = await request.post('http://localhost:8081/api/auth/login', {
    data: account,
    headers: {
      'User-Agent': 'Playwright Fahrad-Fuchs Test'
    }
  });

  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  expect(payload.mfaRequired).toBeFalsy();

  return {
    token: payload.token,
    user: {
      id: payload.user.id,
      name: payload.user.displayName || payload.user.name,
      email: payload.user.email,
      role: payload.user.role,
      emailVerified: payload.user.emailVerified
    }
  };
}

function toAbsoluteHashUrl(baseURL, href) {
  return new URL(href, baseURL.endsWith('/') ? baseURL : baseURL + '/').toString();
}

test.describe('Fahrad-Fuchs headed flow', () => {
  test('loads storefront, detail, checkout, and reserves a ride', async ({ page, request, baseURL }) => {
    const session = await authenticate(request);

    await page.addInitScript(
      ([storageKey, authSession]) => {
        window.localStorage.setItem(storageKey, JSON.stringify(authSession));
      },
      [sessionStorageKey, session]
    );

    await page.goto('/#/fahrad-fuchs', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.storefront-shell')).toBeVisible();
    await expect(page.locator('.bike-card').first()).toBeVisible();
    await expect(page.locator('.store-header')).toContainText('Fahrad-Fuchs');

    const bikeCards = page.locator('.bike-card');
    await expect(bikeCards).toHaveCount(4);

    const firstCardLink = page.locator('.bike-card a').first();
    const firstCardTitle = ((await page.locator('.bike-card h3').first().textContent()) || '').trim();
    const detailHref = await firstCardLink.getAttribute('href');
    expect(detailHref).toBeTruthy();

    await page.goto(toAbsoluteHashUrl(baseURL || 'http://localhost:4300', detailHref), {
      waitUntil: 'domcontentloaded'
    });
    await expect(page.locator('.detail-shell')).toBeVisible();
    await expect(page.locator('.detail-shell h1')).toBeVisible();
    await expect(page.locator('.reserve-button')).toBeVisible();

    const detailTitle = ((await page.locator('.detail-shell h1').textContent()) || '').trim();
    expect(detailTitle.length).toBeGreaterThan(0);

    await page.locator('.reserve-button').click();
    await expect(page).toHaveURL(/#\/fahrad-fuchs\/checkout\//);
    await expect(page.locator('.checkout-shell')).toBeVisible();
    await expect(page.locator('.checkout-shell h1')).toContainText('Probefahrt');

    await page.locator('.checkout-form button').click();
    await expect(page.locator('.confirmation')).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.confirmation')).toContainText('Bestaetigt');

    const bookingLink = page.locator('.confirmation a');
    await bookingLink.click();
    await expect(page).toHaveURL(/#\/fahrad-fuchs\/bookings/);
    await expect(page.locator('.bookings-shell')).toBeVisible();
    await expect(page.locator('.booking-card').first()).toBeVisible();

    const bookingReference = ((await page.locator('.booking-card .eyebrow').first().textContent()) || '').trim();

    console.log(JSON.stringify({
      storefront: 'ok',
      firstCardTitle,
      detailTitle,
      bookingReference
    }, null, 2));
  });
});
