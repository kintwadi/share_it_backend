const { test, expect } = require('@playwright/test');

const stores = [
  { id: 1, name: 'Tech Hub Europe', slug: 'tech-hub-europe', createdAt: '2026-07-17T10:00:00Z' },
  { id: 2, name: 'Urban Home Living', slug: 'urban-home-living', createdAt: '2026-07-17T10:00:00Z' }
];

const categories = [
  {
    id: 11,
    storeId: 1,
    parentId: null,
    name: 'Electronics',
    slug: 'electronics',
    attributeSchema: { department: 'electronics' },
    children: [
      {
        id: 12,
        storeId: 1,
        parentId: 11,
        name: 'Smartphones',
        slug: 'smartphones',
        attributeSchema: { attributes: ['brand', 'storage'] },
        children: []
      },
      {
        id: 13,
        storeId: 1,
        parentId: 11,
        name: 'Laptops',
        slug: 'laptops',
        attributeSchema: { attributes: ['ram', 'storage'] },
        children: []
      }
    ]
  }
];

const productsByStore = {
  1: [
    {
      id: 101,
      storeId: 1,
      sku: 'TECH-PHN-NOVAX',
      name: 'Orion Nova X',
      description: 'Flagship smartphone with all-day battery life.',
      basePrice: 899,
      currency: 'EUR',
      categoryId: 12,
      properties: { brand: 'Orion', screen: '6.7-inch AMOLED' },
      isActive: true,
      createdAt: '2026-07-17T10:00:00Z',
      updatedAt: '2026-07-17T10:00:00Z'
    },
    {
      id: 102,
      storeId: 1,
      sku: 'TECH-LAP-WB14',
      name: 'Vertex WorkBook Pro 14',
      description: 'Professional laptop with creator-grade performance.',
      basePrice: 1499,
      currency: 'EUR',
      categoryId: 13,
      properties: { brand: 'Vertex', ram: '32GB' },
      isActive: true,
      createdAt: '2026-07-17T10:00:00Z',
      updatedAt: '2026-07-18T10:00:00Z'
    },
    {
      id: 103,
      storeId: 1,
      sku: 'TECH-AUD-SPM',
      name: 'Pulse Studio Max',
      description: 'Noise-cancelling headphones tuned for travel.',
      basePrice: 329,
      currency: 'EUR',
      categoryId: 11,
      properties: { brand: 'Pulse', batteryHours: 40 },
      isActive: true,
      createdAt: '2026-07-17T10:00:00Z',
      updatedAt: '2026-07-16T10:00:00Z'
    }
  ],
  2: [
    {
      id: 201,
      storeId: 2,
      sku: 'HOME-SOF-CLOUD',
      name: 'Cloud Modular Sofa',
      description: 'Modular sofa designed for family lounges.',
      basePrice: 1299,
      currency: 'EUR',
      categoryId: null,
      properties: { brand: 'Loft & Line' },
      isActive: true,
      createdAt: '2026-07-17T10:00:00Z',
      updatedAt: '2026-07-17T10:00:00Z'
    }
  ]
};

const variantsByProduct = {
  101: [
    {
      id: 1001,
      storeId: 1,
      productId: 101,
      sku: 'TECH-PHN-NOVAX-BLK-128-5G',
      price: 899,
      stock: 12,
      options: { color: 'Black', storage: '128GB', connectivity: '5G' },
      isActive: true,
      createdAt: '2026-07-17T10:00:00Z'
    }
  ]
};

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/stores', async (route) => {
    await route.fulfill({ json: stores });
  });

  await page.route('**/api/v1/categories', async (route) => {
    await route.fulfill({ json: categories });
  });

  await page.route('**/api/v1/products**', async (route) => {
    const url = new URL(route.request().url());
    const variantMatch = url.pathname.match(/\/products\/(\d+)\/variants$/);
    if (variantMatch) {
      const productId = Number(variantMatch[1] || 0);
      await route.fulfill({ json: variantsByProduct[productId] || [] });
      return;
    }

    const storeId = Number(route.request().headers()['x-store-id'] || 0);
    const categoryId = Number(url.searchParams.get('categoryId') || 0);
    let products = productsByStore[storeId] || [];
    if (categoryId > 0) {
      products = products.filter((product) => product.categoryId === categoryId);
    }
    await route.fulfill({ json: products });
  });
});

test('supports route, sort, size, search, and product detail in headed mode', async ({ page }) => {
  await page.goto('/stores/tech-hub-europe?sort=price-desc&size=6&q=orion');

  const selects = page.locator('select');
  const searchInput = page.locator('input[type="search"]');

  await expect(page.getByText('Linked Store Catalog')).toBeVisible();
  await expect(selects.nth(0)).toContainText('Tech Hub Europe');
  await expect(selects.nth(2)).toContainText('Price High To Low');
  await expect(selects.nth(3)).toContainText('6');
  await expect(searchInput).toHaveValue('orion');
  await expect(page.getByText('Orion Nova X')).toBeVisible();

  await page.getByText('Orion Nova X').click();
  await expect(page).toHaveURL(/\/stores\/tech-hub-europe\/products\/101\?q=orion&sort=price-desc&size=6/);
  await expect(page.getByText('TECH-PHN-NOVAX-BLK-128-5G')).toBeVisible();

  await selects.nth(2).selectOption({ label: 'Name A-Z' });
  await expect(page).toHaveURL(/sort=name-asc/);

  await selects.nth(3).selectOption({ label: '12' });
  await expect(page).toHaveURL(/size=12/);

  await page.getByRole('button', { name: 'Back To Store' }).click();
  await expect(page).toHaveURL(/\/stores\/tech-hub-europe\?q=orion&sort=name-asc&size=12/);
});
