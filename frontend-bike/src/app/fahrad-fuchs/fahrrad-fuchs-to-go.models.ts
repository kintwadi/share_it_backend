export interface FahrradFuchsToGoStore {
  storeName: string;
  addressLine1: string;
  cityLine: string;
  phone: string;
  email: string;
  mapUrl: string;
  openingHours: string[];
}

export interface FahrradFuchsToGoCatalogItem {
  listingId: string;
  slug: string;
  title: string;
  category: string;
  teaser: string;
  availabilityBadge: string;
  dailyRate: number;
  retailPrice: number;
  imageUrl: string;
  highlights: string[];
}

export interface FahrradFuchsToGoTechnicalSpec {
  label: string;
  value: string;
}

export interface FahrradFuchsToGoFrameOption {
  value: string;
  label: string;
}

export interface FahrradFuchsToGoListingDetail {
  listingId: string;
  slug: string;
  title: string;
  category: string;
  availabilityBadge: string;
  description: string;
  dailyRate: number;
  retailPrice: number;
  imageUrl: string;
  gallery: string[];
  valuePoints: string[];
  technicalSpecs: FahrradFuchsToGoTechnicalSpec[];
  frameOptions: FahrradFuchsToGoFrameOption[];
  store: FahrradFuchsToGoStore;
}

export interface FahrradFuchsToGoStorefrontResponse {
  store: FahrradFuchsToGoStore;
  bikes: FahrradFuchsToGoCatalogItem[];
}

export interface FahrradFuchsToGoCheckoutRequest {
  startDate: string;
  endDate: string;
  frameSizeOption: string;
  paymentMethod: string;
  paymentToken: string;
}

export interface FahrradFuchsToGoCheckoutResponse {
  bookingId: string;
  bookingReference: string;
  listingId: string;
  bikeSlug: string;
  bikeTitle: string;
  startDate: string;
  endDate: string;
  frameSizeOption: string;
  totalAmount: number;
  currency: string;
  paymentMethod: string;
  status: string;
  store: FahrradFuchsToGoStore;
}

export interface FahrradFuchsToGoBooking {
  bookingId: string;
  bookingReference: string;
  listingId: string;
  bikeSlug: string;
  bikeTitle: string;
  startDate: string;
  endDate: string;
  frameSizeOption: string;
  totalAmount: number;
  currency: string;
  status: string;
  imageUrl: string;
  createdAt: string;
}
