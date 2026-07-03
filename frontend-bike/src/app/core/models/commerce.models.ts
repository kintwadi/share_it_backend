export interface SubscriptionConfig {
  enabled: boolean;
  starter: boolean;
  plus: boolean;
  pro: boolean;
}

export interface CheckoutSessionResponse {
  sessionId?: string;
  url?: string;
  error?: string;
}

export interface PartnerListingForm {
  title: string;
  description: string;
  category: string;
  type: 'LEND' | 'SELL';
  hourlyRate: number;
  imageUrl: string;
  gallery: string[];
  autoApprove: boolean;
  insuranceRequired: boolean;
  x: number;
  y: number;
  streetAddress: string;
  city: string;
  postalCode: string;
  country: string;
  availableUnlimited: boolean;
}

export interface PartnerListingSummary {
  id: string;
  title: string;
  description: string;
  imageUrl: string | null;
  category: string | null;
  type: string | null;
  hourlyRate: number | null;
  status: string | null;
  partnerName: string | null;
}
