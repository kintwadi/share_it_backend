export enum ListingType {
  GOODS = 'GOODS',
  SKILL = 'SKILL',
  GIVE = 'GIVE',
  SELL = 'SELL',
  LEND = 'LEND'
}

export interface SubscriptionUpgradePreview {
  currentPlan: string;
  newPlan: string;
  cycleEndDate: string;
  remainingDays: number;
  creditCents: number;
  chargeCents: number;
  netImmediateChargeCents: number;
  nextFullChargeCents: number;
  nextFullChargeDate: string;
}

export enum AvailabilityStatus {
  AVAILABLE = 'AVAILABLE',
  BORROWED = 'BORROWED',
  PENDING = 'PENDING',
  SCHEDULED = 'SCHEDULED',
  BLOCKED = 'BLOCKED',
  HIDDEN = 'HIDDEN',
  GIFTED = 'GIFTED',
  SOLD = 'SOLD',
  APPROVED = 'APPROVED',
  DISPUTED = 'DISPUTED',
  PARTNER_INACTIVE = 'PARTNER_INACTIVE',
  PARTNER_ACTIVE = 'PARTNER_ACTIVE',
  PARTNER_BORROW_REQUESTED = 'PARTNER_BORROW_REQUESTED'
}

export enum UserRole {
  ADMIN = 'ADMIN',
  LENDER = 'LENDER',
  BORROWER = 'BORROWER',
  MEMBER = 'MEMBER'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  BLOCKED = 'BLOCKED'
}

export enum VerificationStatus {
  UNVERIFIED = 'UNVERIFIED',
  PENDING = 'PENDING',
  VERIFIED = 'VERIFIED'
}

export enum ReturnStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  DISPUTED = 'DISPUTED'
}

export interface ReturnSessionResponse {
  id: string;
  listingId: string;
  borrowerCode: string;
  lenderCode: string;
  borrowerScanned: boolean;
  lenderScanned: boolean;
  manualBorrowerConfirmed: boolean;
  manualLenderConfirmed: boolean;
  status: ReturnStatus;
  expiresAt: string;
}

export interface User {
  id: string;
  name: string;
  displayName?: string;
  email: string;
  phone: string;
  address: string;
  avatarUrl: string;
  trustScore: number;
  vouchCount: number;
  verificationStatus: VerificationStatus;
  location: {
    lat: number;
    lng: number;
  };
  joinedDate: string;
  role: UserRole;
  status: UserStatus;
  twoFactorEnabled?: boolean;
  profileVisible?: boolean;
  showRatings?: boolean;
  adminScope?: 'FULL' | 'PARTNER' | string;
}

export interface Listing {
  id: string;
  ownerId?: string | null;
  partnerId?: string | null;
  partnerName?: string | null;
  borrowerId?: string;
  borrower?: User;
  title: string;
  description: string;
  type: ListingType;
  category: string;
  imageUrl: string;
  gallery?: string[];
  distanceMiles: number;
  status: AvailabilityStatus;
  hourlyRate?: number;
  autoApprove?: boolean;
  insuranceRequired?: boolean;
  location: {
    x: number;
    y: number;
  };
  owner?: User;
  pickupLocation?: PickupLocation;
  pickupLocationCustom?: string;
  pickupLocationStreet?: string;
  pickupLocationHouseNumber?: string;
  pickupLocationCity?: string;
  pickupLocationZip?: string;

  availableUnlimited?: boolean;
  availableFrom?: string | null;
  availableTo?: string | null;
}

export interface InsuranceTypeInfo {
  insuranceType: string;
  percent: number;
  min: number;
  max: number;
}

export interface InsuranceQuoteResponse {
  quoteId: string;
  productId: string;
  productBasePrice: number;
  insuranceType: string;
  insuranceCost: number;
  totalCost: number;
  currency: string;
  validUntil: string;
}

export interface InsurancePurchaseResponse {
  policyNumber: string;
  status: string;
  message: string;
}

export interface ListingRecommendationRequest {
  title: string;
  category: string;
  description?: string;
  estimatedValue?: number;
}

export interface SimilarItem {
  id: string;
  title: string;
  transactionType: string;
  price?: number;
}

export interface ListingRecommendationResult {
  recommendedAction: 'SELL' | 'LEND' | 'GIVE';
  suggestedPrice?: number;
  confidenceScore: number;
  reasoning: string;
  similarItems: SimilarItem[];
}

export interface PickupLocation {
  id: string;
  name: string;
  address: string;
  location: {
    x: number;
    y: number;
  };
}

export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  senderEmail?: string;
  receiverEmail?: string;
  content: string;
  imageUrl?: string;
  timestamp: string;
  isRead: boolean;
}

export interface Review {
  id: string;
  authorId: string;
  targetUserId: string;
  listingId: string;
  rating: number;
  comment: string;
  timestamp: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  icon?: string;
}

export enum NotificationType {
  PENDING_REQUEST = 'PENDING_REQUEST',
  PRICE_SUGGESTION = 'PRICE_SUGGESTION',
  REQUEST_APPROVED = 'REQUEST_APPROVED'
}

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  link?: string;
  metadata?: any;
}

export interface Activity {
  id: string;
  type: string;
  userId: string;
  listingId?: string;
  timestamp: string;
  details?: string;
}

export interface BorrowHistoryItem {
    id: string;
    listingId: string;
    listingTitle: string;
    lenderId: string;
    lenderName: string;
    borrowerId: string;
    borrowerName: string;
    startDate: string;
    endDate: string;
    status: AvailabilityStatus;
    listing?: Listing; // Added to match mockApi usage
    borrowedDate?: string;
    returnedDate?: string;
}

export interface Device {
  id: string;
  name: string;
  userAgent: string;
  ipAddress: string;
  lastActive: string;
  trusted: boolean;
}

export type PartnerStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED';

export interface Partner {
  id: string;
  name: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  contactPerson?: string;
  status?: PartnerStatus;
}

export interface PartnerBorrowRequest {
  listingId: string;
  listingTitle?: string;
  partnerId?: string;
  partnerName?: string;
  borrowerId?: string | null;
  borrowerName?: string | null;
  borrowerEmail?: string | null;
  status?: AvailabilityStatus;
}

export interface PartnerSettings {
  partnerId: string;
  maxLendingDays?: number | null;
  depositCents?: number | null;
  autoApproval?: boolean | null;
}
