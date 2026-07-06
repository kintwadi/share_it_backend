export interface BikeCatalogItem {
  id: string;
  title: string;
  description: string;
  category: string;
  imageUrl: string | null;
  hourlyRate: number | null;
  city: string | null;
  country: string | null;
  frameSize: string | null;
  bikeType: string | null;
  assemblyBufferMinutes: number | null;
  rentToOwnEligible: boolean;
  retailPurchasePrice: number | null;
  inventoryStatus: string | null;
  createdAt: string | null;
}

export interface BikeDetail extends BikeCatalogItem {
  gallery: string[];
  ownerName: string | null;
  partnerName: string | null;
}

export interface RentToOwnQuote {
  listingId: string;
  borrowerId: string | null;
  retailPurchasePrice: number;
  rentalCreditApplied: number;
  settlementAmount: number;
  currency: string;
  rentToOwnEligible: boolean;
}

export interface BikePage {
  content: BikeCatalogItem[];
  pagination: BikePagination;
}

export interface BikePagination {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  from: number;
  to: number;
  hasNext: boolean;
  hasPrevious: boolean;
  pages: number[];
}

export interface BikeShopFilterSelection {
  key: string;
  values: string[];
}

export interface BikeShopSearchRequest {
  query: string | null;
  saleType: string | null;
  categories: string[];
  minPrice: number | null;
  maxPrice: number | null;
  sort: string;
  page: number;
  size: number;
  filters: BikeShopFilterSelection[];
}

export interface BikeShopFilterOption {
  value: string;
  label: string;
  displayLabel: string;
  count: number;
  selected: boolean;
  disabled: boolean;
}

export interface BikeShopFilterSection {
  key: string;
  label: string;
  displayLabel: string;
  uiControl: string;
  isCustom: boolean;
  multiSelect: boolean;
  componentSpecific: boolean;
  options: BikeShopFilterOption[];
}

export interface BikeShopSidebar {
  minAvailablePrice: number | null;
  maxAvailablePrice: number | null;
  selectedMinPrice: number | null;
  selectedMaxPrice: number | null;
  framesetSelected: boolean;
  sections: BikeShopFilterSection[];
}

export interface BikeShopPagination {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  from: number;
  to: number;
  hasNext: boolean;
  hasPrevious: boolean;
  pages: number[];
}

export interface BikeShopProductPreview {
  id: number;
  brandName: string;
  modelName: string;
  displayName: string;
  modelYear: number;
  category: string;
  saleType: string;
  basePrice: number;
  description: string | null;
  imageUrl: string;
  active: boolean;
  inStock: boolean;
  totalStock: number;
  skuCount: number;
  defaultColor: string;
}

export interface BikeShopSearchResponse {
  products: BikeShopProductPreview[];
  pagination: BikeShopPagination;
  sidebar: BikeShopSidebar;
}

export interface BikeShopSku {
  id: number;
  skuCode: string;
  colorName: string;
  sizeValue: string;
  riderHeightMinCm: number | null;
  riderHeightMaxCm: number | null;
  stackMm: number | null;
  reachMm: number | null;
  stockQuantity: number;
  priceModifier: number | null;
}

export interface BikeShopSpecGroup {
  attributeName: string;
  custom: boolean;
  values: string[];
}

export interface BikeShopDetail {
  id: number;
  brandName: string;
  modelName: string;
  displayName: string;
  modelYear: number;
  category: string;
  saleType: string;
  basePrice: number;
  description: string | null;
  imageUrl: string;
  active: boolean;
  skus: BikeShopSku[];
  specs: BikeShopSpecGroup[];
}

export interface BikeAdminSpecSelectionRequest {
  attributeName: string;
  values: string[];
}

export interface BikeAdminSkuRequest {
  id?: number | null;
  skuCode: string;
  colorName: string;
  sizeValue: string;
  riderHeightMinCm: number | null;
  riderHeightMaxCm: number | null;
  stackMm: number | null;
  reachMm: number | null;
  stockQuantity: number;
  priceModifier: number | null;
}

export interface BikeAdminUpsertBikeRequest {
  brandName: string;
  modelName: string;
  modelYear: number;
  category: string;
  saleType: string;
  basePrice: number;
  description: string | null;
  imageUrl: string | null;
  isActive: boolean;
  specs: BikeAdminSpecSelectionRequest[];
  skus: BikeAdminSkuRequest[];
}
