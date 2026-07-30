export interface StoreSummary {
  id: number;
  name: string;
  slug: string;
  bannerImageUrl: string | null;
  createdAt: string;
}

export interface StoreCategory {
  id: number;
  storeId: number;
  parentId: number | null;
  name: string;
  slug: string;
  attributeSchema: Record<string, unknown>;
  children: StoreCategory[];
}

export interface StoreProduct {
  id: number;
  storeId: number;
  sku: string;
  name: string;
  description: string | null;
  basePrice: number;
  currency: string;
  categoryId: number | null;
  properties: Record<string, unknown>;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StoreProductVariant {
  id: number;
  storeId: number;
  productId: number;
  sku: string;
  price: number | null;
  stock: number;
  options: Record<string, unknown>;
  isActive: boolean;
  createdAt: string;
}
