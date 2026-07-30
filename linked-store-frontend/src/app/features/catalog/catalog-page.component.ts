import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { LinkedStoreApiService } from '../../core/services/linked-store-api.service';
import { StoreCategory, StoreProduct, StoreProductVariant, StoreSummary } from '../../core/models/catalog.models';

@Component({
  selector: 'app-catalog-page',
  imports: [CommonModule, FormsModule, CurrencyPipe],
  templateUrl: './catalog-page.component.html',
  styleUrl: './catalog-page.component.css'
})
export class CatalogPageComponent {
  readonly sortOptions = [
    { value: 'featured', label: 'Featured' },
    { value: 'name-asc', label: 'Name A-Z' },
    { value: 'name-desc', label: 'Name Z-A' },
    { value: 'price-asc', label: 'Price Low To High' },
    { value: 'price-desc', label: 'Price High To Low' },
    { value: 'newest', label: 'Newest' }
  ] as const;
  readonly pageSizeOptions = [6, 9, 12, 24] as const;

  private readonly api = inject(LinkedStoreApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly stores = signal<StoreSummary[]>([]);
  readonly categories = signal<StoreCategory[]>([]);
  readonly products = signal<StoreProduct[]>([]);
  readonly variants = signal<StoreProductVariant[]>([]);
  readonly selectedStoreId = signal<number | null>(null);
  readonly selectedCategoryId = signal<number | null>(null);
  readonly selectedProductId = signal<number | null>(null);
  readonly selectedVariantId = signal<number | null>(null);
  readonly selectedImageIndex = signal(0);
  readonly searchTerm = signal('');
  readonly selectedSort = signal<(typeof this.sortOptions)[number]['value']>('featured');
  readonly viewMode = signal<'grid' | 'list'>('grid');
  readonly page = signal(1);
  readonly pageSize = signal(9);
  readonly loadingCatalog = signal(false);
  readonly loadingVariants = signal(false);
  readonly errorMessage = signal('');
  readonly quoteCopied = signal(false);
  readonly skuCopied = signal(false);
  readonly loadedStoreId = signal<number | null>(null);
  readonly routeCategoryId = signal<number | null>(null);
  readonly routeSearch = signal('');
  readonly routeSort = signal<(typeof this.sortOptions)[number]['value']>('featured');
  readonly routePage = signal(1);
  readonly routePageSize = signal(9);

  readonly categoryOptions = computed(() => this.flattenCategories(this.categories()));

  readonly filteredProducts = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoryId = this.selectedCategoryId();

    return this.products().filter((product) => {
      const matchesCategory = !categoryId || product.categoryId === categoryId;
      if (!matchesCategory) {
        return false;
      }
      if (!term) {
        return true;
      }

      const searchableText = [
        product.name,
        product.sku,
        product.description || '',
        ...Object.values(product.properties || {}).map((value) => String(value))
      ]
        .join(' ')
        .toLowerCase();

      return searchableText.includes(term);
    });
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredProducts().length / this.pageSize())));
  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));
  readonly activeFilters = computed(() => {
    const filters: Array<{ key: 'category' | 'search' | 'sort' | 'size'; label: string }> = [];
    const categoryId = this.selectedCategoryId();
    const category = this.categoryOptions().find((entry) => entry.id === categoryId);

    if (category) {
      filters.push({ key: 'category', label: category.name });
    }

    if (this.searchTerm().trim()) {
      filters.push({ key: 'search', label: `Search: ${this.searchTerm().trim()}` });
    }

    if (this.selectedSort() !== 'featured') {
      const sortLabel = this.sortOptions.find((option) => option.value === this.selectedSort())?.label ?? this.selectedSort();
      filters.push({ key: 'sort', label: `Sort: ${sortLabel}` });
    }

    if (this.pageSize() !== 9) {
      filters.push({ key: 'size', label: `Page size: ${this.pageSize()}` });
    }

    return filters;
  });
  readonly sortedProducts = computed(() => {
    const sort = this.selectedSort();
    const products = [...this.filteredProducts()];

    switch (sort) {
      case 'name-asc':
        return products.sort((a, b) => a.name.localeCompare(b.name));
      case 'name-desc':
        return products.sort((a, b) => b.name.localeCompare(a.name));
      case 'price-asc':
        return products.sort((a, b) => Number(a.basePrice) - Number(b.basePrice));
      case 'price-desc':
        return products.sort((a, b) => Number(b.basePrice) - Number(a.basePrice));
      case 'newest':
        return products.sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt));
      default:
        return products.sort((a, b) => a.name.localeCompare(b.name));
    }
  });

  readonly pagedProducts = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.sortedProducts().slice(start, start + this.pageSize());
  });

  readonly selectedProduct = computed(() => {
    const productId = this.selectedProductId();
    return this.products().find((product) => product.id === productId) ?? null;
  });
  readonly selectedProductImages = computed(() => this.productImages(this.selectedProduct()));
  readonly selectedVariant = computed(() => {
    const variantId = this.selectedVariantId();
    return this.variants().find((variant) => variant.id === variantId) ?? null;
  });
  readonly selectedVariantImages = computed(() => this.variantImages(this.selectedVariant()));
  readonly displayImages = computed(() => {
    const variantImages = this.selectedVariantImages();
    return variantImages.length ? variantImages : this.selectedProductImages();
  });
  readonly selectedProductImage = computed(() => {
    const images = this.displayImages();
    const imageIndex = this.selectedImageIndex();
    return images[imageIndex] ?? images[0] ?? this.placeholderImage(this.selectedProduct()?.name ?? 'Product');
  });
  readonly selectedProductDetails = computed(() => this.productDetails(this.selectedProduct()));
  readonly selectedVariantDetails = computed(() => this.variantDetails(this.selectedVariant()));
  readonly selectedVariantStock = computed(() => this.selectedVariant()?.stock ?? null);
  readonly selectedInventoryState = computed(() => this.inventoryState(this.selectedVariantStock()));
  readonly lowStockVariantCount = computed(() => this.variants().filter((variant) => variant.stock > 0 && variant.stock <= 5).length);
  readonly selectedCategoryName = computed(() => {
    const categoryId = this.selectedProduct()?.categoryId;
    return this.categoryOptions().find((category) => category.id === categoryId)?.name ?? 'Uncategorized';
  });

  readonly pages = computed(() => Array.from({ length: this.totalPages() }, (_, index) => index + 1));
  readonly selectedStoreName = computed(() => {
    const storeId = this.selectedStoreId();
    return this.stores().find((store) => store.id === storeId)?.name ?? 'Unknown store';
  });
  readonly selectedStore = computed(() => {
    const storeId = this.selectedStoreId();
    return this.stores().find((store) => store.id === storeId) ?? null;
  });
  readonly quoteSummary = computed(() => {
    const product = this.selectedProduct();
    const variant = this.selectedVariant();
    if (!product) {
      return '';
    }

    const price = variant?.price ?? product.basePrice;
    const stock = variant?.stock ?? null;
    const detailText = (variant ? this.variantDetails(variant) : this.productDetails(product))
      .slice(0, 4)
      .map((entry) => `${entry.key}: ${entry.value}`)
      .join(' | ');

    return [
      `Store: ${this.selectedStoreName()}`,
      `Category: ${this.selectedCategoryName()}`,
      `Product: ${product.name}`,
      `SKU: ${variant?.sku ?? product.sku}`,
      `Price: ${this.currencyLabel(price, product.currency)}`,
      `Stock: ${this.stockLabel(stock)}`,
      detailText ? `Options: ${detailText}` : null
    ]
      .filter((entry): entry is string => Boolean(entry))
      .join('\n');
  });

  private quoteFeedbackTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.route.paramMap.subscribe(() => this.handleRouteState());
    this.route.queryParamMap.subscribe(() => this.handleQueryState());
    this.loadStores();
  }

  onStoreChange(storeId: number | string): void {
    const parsed = Number(storeId);
    if (!Number.isInteger(parsed) || parsed <= 0) {
      return;
    }

    this.selectStore(parsed, true, null);
  }

  onCategoryChange(categoryId: number | string | null): void {
    const parsed = Number(categoryId);
    this.selectedCategoryId.set(Number.isInteger(parsed) && parsed > 0 ? parsed : null);
    this.selectedProductId.set(null);
    this.variants.set([]);
    this.page.set(1);
    this.syncRoute(null);
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.page.set(1);
    this.syncRoute(this.selectedProductId());
  }

  onSortChange(value: string): void {
    this.selectedSort.set(this.normalizeSort(value));
    this.page.set(1);
    this.syncRoute(this.selectedProductId());
  }

  onPageSizeChange(value: number | string): void {
    this.pageSize.set(this.normalizePageSize(Number(value)));
    this.page.set(1);
    this.syncRoute(this.selectedProductId());
  }

  setView(mode: 'grid' | 'list'): void {
    this.viewMode.set(mode);
  }

  clearFilters(): void {
    this.selectedCategoryId.set(null);
    this.searchTerm.set('');
    this.selectedSort.set('featured');
    this.pageSize.set(9);
    this.page.set(1);
    this.syncRoute(this.selectedProductId());
  }

  openProduct(product: StoreProduct, updateRoute = true): void {
    const storeId = this.selectedStoreId();
    if (!storeId) {
      return;
    }

    this.selectedProductId.set(product.id);
    this.selectedVariantId.set(null);
    this.selectedImageIndex.set(0);
    if (updateRoute) {
      this.syncRoute(product.id);
    }
    this.loadingVariants.set(true);
    this.errorMessage.set('');
    this.api.getVariants(storeId, product.id).subscribe({
      next: (variants) => {
        this.variants.set(variants);
        this.selectedVariantId.set(variants[0]?.id ?? null);
        this.loadingVariants.set(false);
      },
      error: () => {
        this.selectedVariantId.set(null);
        this.loadingVariants.set(false);
        this.errorMessage.set('Unable to load product variants.');
      }
    });
  }

  setPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }
    this.page.set(page);
    this.syncRoute(this.selectedProductId());
  }

  previousPage(): void {
    this.setPage(this.page() - 1);
  }

  nextPage(): void {
    this.setPage(this.page() + 1);
  }

  closeProduct(): void {
    this.selectedProductId.set(null);
    this.selectedVariantId.set(null);
    this.selectedImageIndex.set(0);
    this.variants.set([]);
    this.syncRoute(null);
  }

  selectImage(index: number): void {
    if (index < 0 || index >= this.displayImages().length) {
      return;
    }
    this.selectedImageIndex.set(index);
  }

  selectVariant(variantId: number): void {
    if (!this.variants().some((variant) => variant.id === variantId)) {
      return;
    }
    this.selectedVariantId.set(variantId);
    this.selectedImageIndex.set(0);
    this.quoteCopied.set(false);
  }

  copyQuoteSummary(): void {
    const text = this.quoteSummary().trim();
    if (!text) {
      return;
    }
    this.copyText(text, () => this.quoteCopied.set(true), () => this.quoteCopied.set(false));
  }

  copySelectedSku(): void {
    const sku = this.selectedVariant()?.sku ?? this.selectedProduct()?.sku ?? '';
    if (!sku) {
      return;
    }
    this.copyText(sku, () => this.skuCopied.set(true), () => this.skuCopied.set(false));
  }

  stockLabel(stock: number | null): string {
    if (stock == null) {
      return 'Stock unavailable';
    }
    if (stock <= 0) {
      return 'Out of stock';
    }
    if (stock <= 5) {
      return `Low stock: ${stock}`;
    }
    return `In stock: ${stock}`;
  }

  inventoryClass(stock: number | null): string {
    return this.inventoryState(stock);
  }

  categoryOptionLabel(category: StoreCategory & { depth: number }): string {
    return `${'  '.repeat(category.depth)}${category.name}`;
  }

  productImage(product: StoreProduct | null): string {
    return this.productImages(product)[0] ?? this.placeholderImage(product?.name ?? 'Product');
  }

  categoryBanner(category: (StoreCategory & { depth: number }) | StoreCategory | null): string | null {
    const banner = category?.attributeSchema?.['bannerImageUrl'];
    return typeof banner === 'string' && banner.trim().length > 0 ? banner : null;
  }

  removeFilter(key: 'category' | 'search' | 'sort' | 'size'): void {
    switch (key) {
      case 'category':
        this.selectedCategoryId.set(null);
        break;
      case 'search':
        this.searchTerm.set('');
        break;
      case 'sort':
        this.selectedSort.set('featured');
        break;
      case 'size':
        this.pageSize.set(9);
        break;
    }
    this.page.set(1);
    this.syncRoute(this.selectedProductId());
  }

  productDetails(product: StoreProduct | null): Array<{ key: string; value: string }> {
    if (!product) {
      return [];
    }

    return Object.entries(product.properties || {})
      .filter(([key]) => key !== 'images')
      .map(([key, value]) => ({
        key,
        value: this.formatPropertyValue(value)
      }));
  }

  variantDetails(variant: StoreProductVariant | null): Array<{ key: string; value: string }> {
    if (!variant) {
      return [];
    }

    return Object.entries(variant.options || {})
      .filter(([key]) => key !== 'images')
      .map(([key, value]) => ({
        key,
        value: this.formatPropertyValue(value)
      }));
  }

  private loadStores(): void {
    this.loadingCatalog.set(true);
    this.api.getStores().subscribe({
      next: (stores) => {
        this.stores.set(stores);
        const defaultStoreId = this.api.getDefaultStoreId();
        const initialStore = stores.find((store) => store.id === defaultStoreId) ?? stores[0] ?? null;
        if (initialStore) {
          this.selectedStoreId.set(initialStore.id);
          this.handleRouteState();
        } else {
          this.loadingCatalog.set(false);
        }
      },
      error: () => {
        this.loadingCatalog.set(false);
        this.errorMessage.set('Unable to load stores. Check the linked-store API and runtime configuration.');
      }
    });
  }

  private loadCatalog(storeId: number, focusProductId: number | null = null): void {
    this.loadingCatalog.set(true);
    this.errorMessage.set('');
    this.loadedStoreId.set(null);

    forkJoin({
      categories: this.api.getCategories(storeId),
      products: this.api.getProducts(storeId)
    }).subscribe({
      next: ({ categories, products }) => {
        this.categories.set(categories);
        this.products.set(products);
        this.loadedStoreId.set(storeId);
        this.applyQueryState();
        this.loadingCatalog.set(false);
        if (focusProductId != null) {
          const targetProduct = products.find((product) => product.id === focusProductId);
          if (targetProduct) {
            this.openProduct(targetProduct, false);
          } else {
            this.selectedProductId.set(null);
            this.selectedVariantId.set(null);
            this.selectedImageIndex.set(0);
            this.variants.set([]);
            this.errorMessage.set('Product not found for the selected store.');
          }
        } else {
          this.selectedProductId.set(null);
          this.selectedVariantId.set(null);
          this.selectedImageIndex.set(0);
          this.variants.set([]);
        }
      },
      error: () => {
        this.loadingCatalog.set(false);
        this.loadedStoreId.set(null);
        this.errorMessage.set('Unable to load catalog data for the selected store.');
      }
    });
  }

  private selectStore(storeId: number, updateRoute: boolean, focusProductId: number | null): void {
    const store = this.stores().find((entry) => entry.id === storeId);
    if (!store) {
      return;
    }

    const shouldReload = this.selectedStoreId() !== storeId || this.loadedStoreId() !== storeId;
    this.selectedStoreId.set(storeId);
    this.selectedCategoryId.set(null);
    this.selectedVariantId.set(null);
    this.page.set(1);
    if (updateRoute) {
      this.syncRoute(focusProductId, store.slug);
    }
    if (shouldReload || focusProductId != null) {
      this.loadCatalog(storeId, focusProductId);
    } else if (focusProductId == null) {
      this.selectedProductId.set(null);
      this.selectedVariantId.set(null);
      this.selectedImageIndex.set(0);
      this.variants.set([]);
    }
  }

  private handleRouteState(): void {
    if (!this.stores().length) {
      return;
    }

    const storeSlug = this.route.snapshot.paramMap.get('storeSlug');
    const productId = this.parsePositiveInt(this.route.snapshot.paramMap.get('id'));

    if (storeSlug) {
      const matchingStore = this.stores().find((store) => store.slug === storeSlug);
      if (!matchingStore) {
        this.errorMessage.set(`Store "${storeSlug}" was not found.`);
        this.loadingCatalog.set(false);
        return;
      }

      if (this.selectedStoreId() !== matchingStore.id || this.loadedStoreId() !== matchingStore.id) {
        this.selectStore(matchingStore.id, false, productId);
      } else if (productId != null) {
        const product = this.products().find((entry) => entry.id === productId);
        if (product && this.selectedProductId() !== productId) {
          this.openProduct(product, false);
        } else if (!product) {
          this.errorMessage.set('Product not found for the selected store.');
        }
      } else {
        this.selectedProductId.set(null);
        this.selectedVariantId.set(null);
        this.selectedImageIndex.set(0);
        this.variants.set([]);
      }
      return;
    }

    if (productId != null) {
      this.resolveStoreForProduct(productId);
      return;
    }

    const defaultStoreId = this.selectedStoreId() ?? this.api.getDefaultStoreId() ?? this.stores()[0]?.id ?? null;
    if (defaultStoreId != null && (this.selectedStoreId() !== defaultStoreId || this.loadedStoreId() !== defaultStoreId)) {
      this.selectStore(defaultStoreId, false, null);
    }
  }

  private handleQueryState(): void {
    const queryParamMap = this.route.snapshot.queryParamMap;
    this.routeCategoryId.set(this.parsePositiveInt(queryParamMap.get('category')));
    this.routeSearch.set((queryParamMap.get('q') ?? '').trim());
    this.routeSort.set(this.normalizeSort(queryParamMap.get('sort')));
    this.routePage.set(this.parsePositiveInt(queryParamMap.get('page')) ?? 1);
    this.routePageSize.set(this.normalizePageSize(this.parsePositiveInt(queryParamMap.get('size'))));
    this.applyQueryState();
  }

  private applyQueryState(): void {
    this.searchTerm.set(this.routeSearch());
    this.selectedCategoryId.set(this.normalizeCategoryId(this.routeCategoryId()));
    this.selectedSort.set(this.routeSort());
    this.pageSize.set(this.routePageSize());
    this.page.set(Math.max(1, this.routePage()));
  }

  private resolveStoreForProduct(productId: number): void {
    if (!this.stores().length) {
      return;
    }

    this.loadingCatalog.set(true);
    this.errorMessage.set('');
    forkJoin(
      this.stores().map((store) =>
        this.api.getProducts(store.id).pipe(
          map((products) => ({ store, product: products.find((entry) => entry.id === productId) ?? null })),
          catchError(() => of({ store, product: null as StoreProduct | null }))
        )
      )
    ).subscribe({
      next: (results) => {
        const match = results.find((entry) => entry.product);
        if (!match || !match.product) {
          this.loadingCatalog.set(false);
          this.errorMessage.set('Product route could not be resolved to any store.');
          return;
        }
        this.selectStore(match.store.id, false, match.product.id);
      },
      error: () => {
        this.loadingCatalog.set(false);
        this.errorMessage.set('Unable to resolve product route.');
      }
    });
  }

  private syncRoute(productId: number | null, explicitStoreSlug?: string): void {
    const storeSlug = explicitStoreSlug ?? this.selectedStore()?.slug ?? null;
    const queryParams = this.buildQueryParams();
    if (storeSlug && productId != null) {
      void this.router.navigate(['/stores', storeSlug, 'products', productId], { queryParams });
      return;
    }
    if (storeSlug) {
      void this.router.navigate(['/stores', storeSlug], { queryParams });
      return;
    }
    if (productId != null) {
      void this.router.navigate(['/products', productId], { queryParams });
      return;
    }
    void this.router.navigate(['/'], { queryParams });
  }

  private parsePositiveInt(value: string | null): number | null {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private normalizeCategoryId(categoryId: number | null): number | null {
    if (categoryId == null) {
      return null;
    }
    return this.categoryOptions().some((category) => category.id === categoryId) ? categoryId : null;
  }

  private normalizeSort(value: string | null): (typeof this.sortOptions)[number]['value'] {
    return this.sortOptions.some((option) => option.value === value)
      ? (value as (typeof this.sortOptions)[number]['value'])
      : 'featured';
  }

  private normalizePageSize(value: number | null): number {
    return this.pageSizeOptions.includes((value ?? 0) as (typeof this.pageSizeOptions)[number]) ? Number(value) : 9;
  }

  private buildQueryParams(): Record<string, string> {
    const queryParams: Record<string, string> = {};
    const categoryId = this.selectedCategoryId();
    const searchTerm = this.searchTerm().trim();
    const currentPage = this.currentPage();
    const sort = this.selectedSort();
    const pageSize = this.pageSize();

    if (categoryId != null) {
      queryParams['category'] = String(categoryId);
    }
    if (searchTerm) {
      queryParams['q'] = searchTerm;
    }
    if (sort !== 'featured') {
      queryParams['sort'] = sort;
    }
    if (currentPage > 1) {
      queryParams['page'] = String(currentPage);
    }
    if (pageSize !== 9) {
      queryParams['size'] = String(pageSize);
    }

    return queryParams;
  }

  private flattenCategories(categories: StoreCategory[], depth = 0): Array<StoreCategory & { depth: number }> {
    return categories.flatMap((category) => [
      { ...category, depth },
      ...this.flattenCategories(category.children || [], depth + 1)
    ]);
  }

  private productImages(product: StoreProduct | null): string[] {
    const parsed = this.parseImageList(product?.properties?.['images']);
    return parsed.length ? parsed : [this.placeholderImage(product?.name ?? 'Product')];
  }

  private variantImages(variant: StoreProductVariant | null): string[] {
    return this.parseImageList(variant?.options?.['images']);
  }

  private parseImageList(value: unknown): string[] {
    if (Array.isArray(value)) {
      return value.filter((image): image is string => typeof image === 'string' && image.trim().length > 0);
    }
    if (typeof value === 'string') {
      return value
        .split(',')
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);
    }
    return [];
  }

  private formatPropertyValue(value: unknown): string {
    if (Array.isArray(value)) {
      return value.map((entry) => String(entry)).join(', ');
    }
    if (value && typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value ?? '');
  }

  private placeholderImage(label: string): string {
    return `https://placehold.co/1200x900/E8EEF7/30425A?text=${encodeURIComponent(label)}`;
  }

  private copyText(text: string, onSuccess: () => void, onFailure: () => void): void {
    const clipboard = globalThis.navigator?.clipboard;
    if (!clipboard?.writeText) {
      onFailure();
      return;
    }

    void clipboard.writeText(text).then(() => {
      onSuccess();
      if (this.quoteFeedbackTimer) {
        clearTimeout(this.quoteFeedbackTimer);
      }
      this.quoteFeedbackTimer = setTimeout(() => {
        this.quoteCopied.set(false);
        this.skuCopied.set(false);
      }, 1800);
    }).catch(() => onFailure());
  }

  private inventoryState(stock: number | null): 'unknown' | 'out' | 'low' | 'healthy' {
    if (stock == null) {
      return 'unknown';
    }
    if (stock <= 0) {
      return 'out';
    }
    if (stock <= 5) {
      return 'low';
    }
    return 'healthy';
  }

  private currencyLabel(value: unknown, currency: string | null | undefined): string {
    const amount = Number(value);
    if (Number.isNaN(amount)) {
      return String(value ?? '');
    }
    const formatter = new Intl.NumberFormat('en', {
      style: 'currency',
      currency: currency || 'EUR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    return formatter.format(amount);
  }
}
