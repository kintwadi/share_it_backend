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
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
