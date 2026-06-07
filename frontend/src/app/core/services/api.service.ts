import { Injectable, inject } from '@angular/core';
import { ApiClientService } from './api-client.service';
import { firstValueFrom } from 'rxjs';
import { Category, Listing, ListingRecommendationRequest, ListingRecommendationResult, ExchangeLocation, User, AvailabilityStatus, Message, InsuranceTypeInfo, InsuranceQuoteResponse, InsurancePurchaseResponse } from '../models/types';
import { AuthStorageService } from './auth-storage.service';
import { withTenantHeader } from '../config/runtime-env';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private api = inject(ApiClientService);
  private authStorage = inject(AuthStorageService);

  private extractApiErrorCode(error: any): string {
    return String(
      error?.error?.error ??
      error?.error?.message ??
      error?.message ??
      'request_failed'
    );
  }

  private normalizeUuid(value: string | null | undefined): string | null {
    const trimmed = String(value ?? '').trim();
    return trimmed ? trimmed : null;
  }

  private normalizeLocalDateTime(value: string | null | undefined): string | null {
    const trimmed = String(value ?? '').trim();
    if (!trimmed) return null;
    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(trimmed) ? trimmed : null;
  }

  async getPublicConfig(): Promise<any> {
    try {
      return await firstValueFrom(this.api.get<any>('/config/public'));
    } catch {
      return {};
    }
  }

  async getSettingsConfig(): Promise<any> {
    return firstValueFrom(this.api.get<any>('/config/settings'));
  }

  async getCurrentUser(): Promise<User | null> {
    if (!this.authStorage.getToken()) return null;
    try {
      return await firstValueFrom(this.api.get<User>('/users/me'));
    } catch {
      return null;
    }
  }

  async getCurrentSubscription(): Promise<any | null> {
    if (!this.authStorage.getToken()) return null;
    try {
      return await firstValueFrom(this.api.get<any>('/subscriptions/me'));
    } catch {
      return null;
    }
  }

  async seedData(): Promise<string> {
    return firstValueFrom(this.api.getText('/seed'));
  }

  async getInsuranceTypes(): Promise<InsuranceTypeInfo[]> {
    return firstValueFrom(this.api.get<InsuranceTypeInfo[]>('/insurance/types'));
  }

  async quoteInsurance(payload: { productId: string; productBasePrice: number; insuranceType: string; customerZipCode?: string | null }): Promise<InsuranceQuoteResponse> {
    const body = {
      productId: payload.productId,
      productBasePrice: payload.productBasePrice,
      insuranceType: payload.insuranceType,
      customerZipCode: payload.customerZipCode ?? null
    };
    return firstValueFrom(this.api.post<InsuranceQuoteResponse>('/insurance/quote', body));
  }

  async purchaseInsurance(quoteId: string): Promise<InsurancePurchaseResponse> {
    return firstValueFrom(this.api.post<InsurancePurchaseResponse>('/insurance/purchase', { quoteId }));
  }

  async getBorrowingHistory(): Promise<any[]> {
    try {
      const user = await this.getCurrentUser();
      if (!user) return [];
      const listings = await this.getListings();
      const borrowed = listings.filter(l => l.borrowerId === user.id);
      const now = new Date();
      return borrowed.map((l, idx) => ({
        id: `hist_${l.id}_${idx}`,
        listing: l,
        borrowedDate: now.toISOString().slice(0, 10),
        returnedDate: l.status === AvailabilityStatus.BORROWED ? '' : now.toISOString().slice(0, 10),
      }));
    } catch {
      return [];
    }
  }

  async getListings(): Promise<Listing[]> {
    try {
      const page = await firstValueFrom(this.api.get<any>('/listings/?page=0&size=100'));
      return page.content || [];
    } catch {
      return [];
    }
  }

  async getNearbyListings(lat: number, lng: number, radiusKm: number = 25, size: number = 100): Promise<Listing[]> {
    try {
      const out = await firstValueFrom(this.api.get<Listing[]>(`/listings/nearby?lat=${encodeURIComponent(String(lat))}&lng=${encodeURIComponent(String(lng))}&radiusKm=${encodeURIComponent(String(radiusKm))}&size=${encodeURIComponent(String(size))}`));
      return Array.isArray(out) ? out : [];
    } catch {
      return [];
    }
  }

  async getListingById(id: string): Promise<Listing | null> {
    try {
      return await firstValueFrom(this.api.get<Listing>(`/listings/${encodeURIComponent(id)}`));
    } catch {
      return null;
    }
  }

  async searchListings(query: string): Promise<Listing[]> {
    try {
      const page = await firstValueFrom(this.api.get<any>(`/listings/?search=${encodeURIComponent(query)}&page=0&size=100`));
      return page.content || [];
    } catch {
      return [];
    }
  }

  async getRecommendedListings(size: number = 6): Promise<Listing[]> {
    try {
      return await firstValueFrom(this.api.get<Listing[]>(`/listings/recommended?size=${size}`));
    } catch {
      return [];
    }
  }

  async dismissRecommendation(id: string): Promise<boolean> {
    await firstValueFrom(this.api.post(`/listings/${id}/dismiss`, {}));
    return true;
  }

  async borrowListing(id: string, payload: { paymentMethod?: string; paymentToken?: string; durationHours?: number; borrowerPath?: string }): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(id)}/borrow`, {
      paymentMethod: payload.paymentMethod ?? 'CASH',
      paymentToken: payload.paymentToken ?? null,
      durationHours: typeof payload.durationHours === 'number' ? payload.durationHours : 1,
      borrowerPath: payload.borrowerPath ?? 'VERIFIED'
    }));
  }

  async login(userId: string): Promise<User> {
    const data = await firstValueFrom(this.api.post<any>('/auth/login', { email: `${userId}@example.com`, password: 'password123' })); // Simplified for mock
    return data.user;
  }

  async loginWithEmail(email: string, password: string): Promise<any> {
    const data = await firstValueFrom(this.api.post<any>('/auth/login', { email, password }));
    if (data.mfaRequired) {
      throw { code: 'MFA_REQUIRED', token: data.token };
    }
    return data;
  }

  async verify2FALogin(code: string, token: string): Promise<any> {
    const res = await fetch(`${this.api.getBaseUrl()}/auth/verify-2fa-login`, {
      method: 'POST',
      headers: withTenantHeader({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }),
      body: JSON.stringify({ code })
    });
    if (!res.ok) throw new Error('Invalid code');
    const data = await res.json();
    return data;
  }

  async loginAdmin(email: string, password: string): Promise<any> {
    const data = await firstValueFrom(this.api.post<any>('/admin/auth/login', { email, password }));
    if (data.mfaRequired) {
      throw { code: 'MFA_REQUIRED', token: data.token };
    }
    return data;
  }

  async verify2FALoginAdmin(code: string, token: string): Promise<any> {
    const res = await fetch(`${this.api.getBaseUrl()}/admin/auth/verify-2fa-login`, {
      method: 'POST',
      headers: withTenantHeader({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }),
      body: JSON.stringify({ code })
    });
    if (!res.ok) throw new Error('Invalid code');
    return res.json();
  }

  async registerAdmin(name: string, email: string, password: string, signupSecret?: string, adminScope?: 'FULL' | 'PARTNER'): Promise<any> {
    const body = { name, email, password, signupSecret: signupSecret ?? '', adminScope: adminScope ?? 'FULL' };
    const data = await firstValueFrom(this.api.post<any>('/admin/auth/register', body));
    return data;
  }

  async loginPartner(email: string, password: string): Promise<any> {
    const data = await firstValueFrom(this.api.post<any>('/partner/auth/login', { email, password }));
    if (data.mfaRequired) {
      throw { code: 'MFA_REQUIRED', token: data.token };
    }
    return data;
  }

  async verify2FALoginPartner(code: string, token: string): Promise<any> {
    const res = await fetch(`${this.api.getBaseUrl()}/partner/auth/verify-2fa-login`, {
      method: 'POST',
      headers: withTenantHeader({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }),
      body: JSON.stringify({ code })
    });
    if (!res.ok) throw new Error('Invalid code');
    return res.json();
  }

  async registerPartner(payload: { partner: any; partnerPassword: string }): Promise<any> {
    const body = {
      partner: payload.partner,
      partnerPassword: payload.partnerPassword
    };
    const data = await firstValueFrom(this.api.post<any>('/partner/auth/register', body));
    return data;
  }

  async registerUser(name: string, email: string, password: string): Promise<any> {
    const body = { name, email, password, phone: '', address: '', avatarUrl: '', lat: 0.0, lng: 0.0 };
    const reg = await firstValueFrom(this.api.post<any>('/auth/register', body));
    if (reg?.requiresEmailVerification) {
      return reg;
    }
    const data = await firstValueFrom(this.api.post<any>('/auth/login', { email, password }));
    return data;
  }

  async startEmailVerification(email: string, language?: string): Promise<{ token: string }> {
    const data = await firstValueFrom(this.api.post<any>('/auth/email-verification/start', { email, language: language ?? 'en' }));
    return { token: String(data?.token || '') };
  }

  async resendEmailVerification(token: string, language?: string): Promise<void> {
    await firstValueFrom(this.api.post<any>('/auth/email-verification/resend', { token, language: language ?? 'en' }));
  }

  async verifyEmailVerification(token: string, code: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/auth/email-verification/verify', { token, code }));
  }

  async requestPasswordReset(email: string): Promise<void> {
    await firstValueFrom(this.api.post('/auth/forgot-password', { email }));
  }

  async verifyResetCode(email: string, code: string): Promise<{ valid: boolean; token?: string | null }> {
    const data = await firstValueFrom(this.api.post<any>('/auth/verify-reset-code', { email, code }));
    return { valid: !!data?.valid, token: data?.token ?? null };
  }

  async resetPassword(token: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.api.post('/auth/reset-password', { token, newPassword }));
  }

  async getContacts(): Promise<User[]> {
    return firstValueFrom(this.api.get<User[]>('/users/contacts'));
  }

  async getExchangeLocations(): Promise<ExchangeLocation[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/pickup-locations/'));
      return (Array.isArray(list) ? list : []).map((p: any) => ({
        id: String(p.id),
        referenceId: p.referenceId ? String(p.referenceId) : undefined,
        name: p.name ?? '',
        address: p.address ?? '',
        location: { x: Number(p.location?.x ?? 0), y: Number(p.location?.y ?? 0) },
        operatingTimeFrom: p.operatingTimeFrom ?? null,
        operatingTimeTo: p.operatingTimeTo ?? null,
      }));
    } catch {
      return [];
    }
  }

  async getCategories(): Promise<Category[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/categories/'));
      return (Array.isArray(list) ? list : []).map((c: any) => ({
        id: String(c.id),
        name: c.code ?? '',
        slug: String(c.code ?? '').toLowerCase(),
        icon: undefined,
      }));
    } catch {
      return [];
    }
  }

  async uploadFile(file: File): Promise<{ url: string; key?: string }> {
    const form = new FormData();
    form.append('file', file);
    return firstValueFrom(this.api.postFormData<any>('/storage/upload', form));
  }

  async uploadListingImage(file: File): Promise<string> {
    const res = await this.uploadFile(file);
    return res?.url || '';
  }

  async uploadUserAvatar(file: File): Promise<User> {
    const form = new FormData();
    form.append('file', file);
    return firstValueFrom(this.api.postFormData<User>('/users/me/avatar', form));
  }

  async evaluateListingRecommendation(req: ListingRecommendationRequest): Promise<ListingRecommendationResult> {
    const payload = {
      title: req.title,
      category: req.category,
      description: req.description ?? '',
      estimatedValue: typeof req.estimatedValue === 'number' ? req.estimatedValue : null,
    };
    const data = await firstValueFrom(this.api.post<any>('/listings/evaluate', payload));
    return {
      recommendedAction: data.recommendedAction,
      suggestedPrice: typeof data.suggestedPrice === 'number' ? data.suggestedPrice : (data.suggestedPrice != null ? Number(data.suggestedPrice) : undefined),
      confidenceScore: typeof data.confidenceScore === 'number' ? data.confidenceScore : 0,
      reasoning: data.reasoning ?? '',
      similarItems: Array.isArray(data.similarItems) ? data.similarItems.map((s: any) => ({
        id: String(s.id),
        title: s.title ?? '',
        transactionType: s.transactionType ?? '',
        price: s.price != null ? Number(s.price) : undefined
      })) : []
    };
  }

  async createListing(payload: {
    title: string;
    description: string;
    category: string;
    type: any;
    hourlyRate?: number;
    imageUrl: string;
    gallery?: string[];
    autoApprove?: boolean;
    insuranceRequired?: boolean;
    x?: number;
    y?: number;
    streetAddress?: string | null;
    city?: string | null;
    postalCode?: string | null;
    country?: string | null;
    pickupLocationId?: string | null;
    pickupLocationCustom?: string | null;
    pickupLocationStreet?: string | null;
    pickupLocationHouseNumber?: string | null;
    pickupLocationCity?: string | null;
    pickupLocationZip?: string | null;
    availableUnlimited?: boolean;
    availableFrom?: string | null;
    availableTo?: string | null;
  }): Promise<Listing> {
    const body = {
      title: payload.title,
      description: payload.description,
      category: payload.category,
      type: payload.type,
      hourlyRate: payload.hourlyRate ?? 0,
      imageUrl: payload.imageUrl,
      gallery: payload.gallery ?? [],
      autoApprove: !!payload.autoApprove,
      insuranceRequired: !!payload.insuranceRequired,
      x: payload.x ?? 0,
      y: payload.y ?? 0,
      streetAddress: payload.streetAddress ?? null,
      city: payload.city ?? null,
      postalCode: payload.postalCode ?? null,
      country: payload.country ?? null,
      availableUnlimited: !!payload.availableUnlimited,
      availableFrom: this.normalizeLocalDateTime(payload.availableFrom),
      availableTo: this.normalizeLocalDateTime(payload.availableTo),
      pickupLocationId: this.normalizeUuid(payload.pickupLocationId),
      pickupLocationCustom: payload.pickupLocationCustom ?? null,
      pickupLocationStreet: payload.pickupLocationStreet ?? null,
      pickupLocationHouseNumber: payload.pickupLocationHouseNumber ?? null,
      pickupLocationCity: payload.pickupLocationCity ?? null,
      pickupLocationZip: payload.pickupLocationZip ?? null,
    };
    try {
      return await firstValueFrom(this.api.post<Listing>('/listings/', body));
    } catch (error: any) {
      throw new Error(this.extractApiErrorCode(error));
    }
  }

  async updateListing(id: string, payload: {
    title: string;
    description: string;
    category: string;
    type: any;
    hourlyRate?: number;
    imageUrl: string;
    gallery?: string[];
    autoApprove?: boolean;
    insuranceRequired?: boolean;
    x?: number;
    y?: number;
    streetAddress?: string | null;
    city?: string | null;
    postalCode?: string | null;
    country?: string | null;
    pickupLocationId?: string | null;
    pickupLocationCustom?: string | null;
    pickupLocationStreet?: string | null;
    pickupLocationHouseNumber?: string | null;
    pickupLocationCity?: string | null;
    pickupLocationZip?: string | null;
    availableUnlimited?: boolean;
    availableFrom?: string | null;
    availableTo?: string | null;
  }): Promise<Listing> {
    const body = {
      title: payload.title,
      description: payload.description,
      category: payload.category,
      type: payload.type,
      hourlyRate: payload.hourlyRate ?? 0,
      imageUrl: payload.imageUrl,
      gallery: payload.gallery ?? [],
      autoApprove: !!payload.autoApprove,
      insuranceRequired: !!payload.insuranceRequired,
      x: payload.x ?? 0,
      y: payload.y ?? 0,
      streetAddress: payload.streetAddress ?? null,
      city: payload.city ?? null,
      postalCode: payload.postalCode ?? null,
      country: payload.country ?? null,
      availableUnlimited: !!payload.availableUnlimited,
      availableFrom: this.normalizeLocalDateTime(payload.availableFrom),
      availableTo: this.normalizeLocalDateTime(payload.availableTo),
      pickupLocationId: this.normalizeUuid(payload.pickupLocationId),
      pickupLocationCustom: payload.pickupLocationCustom ?? null,
      pickupLocationStreet: payload.pickupLocationStreet ?? null,
      pickupLocationHouseNumber: payload.pickupLocationHouseNumber ?? null,
      pickupLocationCity: payload.pickupLocationCity ?? null,
      pickupLocationZip: payload.pickupLocationZip ?? null,
    };
    try {
      return await firstValueFrom(this.api.put<Listing>(`/listings/${encodeURIComponent(id)}`, body));
    } catch (error: any) {
      throw new Error(this.extractApiErrorCode(error));
    }
  }

  async getConversations(): Promise<User[]> {
    const list = await firstValueFrom(this.api.get<any[]>('/messages/conversations'));
    return (Array.isArray(list) ? list : []).map((u: any) => ({
      id: String(u.id),
      name: u.name ?? '',
      email: '',
      phone: '',
      address: '',
      avatarUrl: u.avatarUrl ?? '',
      trustScore: typeof u.trustScore === 'number' ? u.trustScore : 0,
      vouchCount: 0,
      verificationStatus: 'UNVERIFIED' as any,
      location: { lat: 0, lng: 0 },
      joinedDate: '',
      role: 'MEMBER' as any,
      status: 'ACTIVE' as any,
    }));
  }

  async getMessages(userId: string): Promise<Message[]> {
    return firstValueFrom(this.api.get<Message[]>(`/messages/with/${encodeURIComponent(userId)}`));
  }

  async sendMessage(receiverId: string, content: string, imageUrl?: string): Promise<Message> {
    const receiverEmail = receiverId && receiverId.includes('@') ? receiverId : null;
    return firstValueFrom(this.api.post<Message>('/messages/', { receiverId, receiverEmail, content, imageUrl: imageUrl ?? null }));
  }

  async deleteMessage(id: string): Promise<void> {
    return firstValueFrom(this.api.delete(`/messages/${id}`));
  }

  async getReviewInvite(token: string): Promise<any> {
    return firstValueFrom(this.api.get<any>(`/reviews/invite/${token}`));
  }

  async submitReviewInvite(token: string, rating: number, comment: string): Promise<any> {
    return firstValueFrom(this.api.post(`/reviews/invite/${token}`, { rating, comment }));
  }

  async createReview(targetUserId: string, listingId: string, rating: number, comment: string): Promise<any> {
    return firstValueFrom(this.api.post('/reviews/', { targetUserId, listingId, rating, comment }));
  }

  async getReviews(userId: string): Promise<any[]> {
    const list = await firstValueFrom(this.api.get<any[]>(`/reviews/user/${encodeURIComponent(userId)}`));
    return Array.isArray(list) ? list : [];
  }

  async getSubscriptionConfig(): Promise<{ starter: boolean, plus: boolean, pro: boolean }> {
    return firstValueFrom(this.api.get<any>('/subscriptions/config'));
  }

  async subscribeStarter(): Promise<void> {
    return firstValueFrom(this.api.post('/subscriptions/starter', {}));
  }

  async createSubscriptionCheckoutSession(planType: string, returnPath: string = '/dashboard'): Promise<{ url: string; sessionId: string }> {
    return firstValueFrom(this.api.post<any>('/subscriptions/create-checkout-session', { 
      planType,
      returnPath
    }));
  }

  async syncSubscriptionFromSession(sessionId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/sync-session', { sessionId }));
  }

  async cancelSubscription(): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/cancel', {}));
  }

  async getSubscriptionInvoices(): Promise<any[]> {
    try {
      const data = await firstValueFrom(this.api.get<any[]>('/subscriptions/invoices'));
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  }

  async previewSubscriptionUpgrade(newPlan: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/upgrade/preview', { newPlan }));
  }

  async confirmSubscriptionUpgrade(newPlan: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/upgrade/confirm', { newPlan }));
  }

  async getPaymentMethods(): Promise<any[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/payments/methods'));
      return Array.isArray(list) ? list : [];
    } catch {
      return [];
    }
  }

  async addPaymentMethod(paymentMethodId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/payments/methods', { paymentMethodId }));
  }

  async removePaymentMethod(paymentMethodId: string): Promise<any> {
    return firstValueFrom(this.api.delete<any>(`/payments/methods/${encodeURIComponent(paymentMethodId)}`));
  }

  async connectOnboard(): Promise<{ accountId: string; url: string }> {
    return firstValueFrom(this.api.post<any>('/payments/connect/onboard', {}));
  }

  async getConnectStatus(): Promise<any> {
    return firstValueFrom(this.api.get<any>('/payments/connect/status'));
  }

  async getPaymentTransactions(): Promise<any[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/payments/transactions'));
      return Array.isArray(list) ? list : [];
    } catch {
      return [];
    }
  }

  async getPaymentTransactionInvoiceUrl(transactionId: string): Promise<{ url: string }> {
    return firstValueFrom(this.api.get<any>(`/payments/transactions/${encodeURIComponent(transactionId)}/invoice`));
  }

  async retryEscrowRelease(): Promise<{ attempted: number }> {
    return firstValueFrom(this.api.post<any>('/payments/release/retry', {}));
  }

  async createPaymentIntent(payload: { amount: number; currency: string; listingId: string; durationHours?: number; borrowerPath?: string; paymentMethodId?: string }): Promise<{ clientSecret: string; amount?: number; currency?: string }> {
    return firstValueFrom(this.api.post<any>('/payments/create-payment-intent', {
      amount: payload.amount,
      currency: payload.currency,
      listingId: payload.listingId,
      durationHours: typeof payload.durationHours === 'number' ? payload.durationHours : 0,
      borrowerPath: payload.borrowerPath ?? 'VERIFIED',
      paymentMethodId: payload.paymentMethodId ?? null,
    }));
  }

  async getDevices(): Promise<any[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/devices'));
      return Array.isArray(list) ? list : [];
    } catch {
      return [];
    }
  }

  async revokeDevice(id: string): Promise<void> {
    await firstValueFrom(this.api.delete(`/devices/${encodeURIComponent(id)}`));
  }

  async sendSubscriptionVerificationCode(planType?: string, language?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/send-code', { planType: planType || null, language: language || null }));
  }

  async verifySubscriptionVerificationCode(code: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/subscriptions/verify-code', { code }));
  }

  async updateProfile(payload: { name?: string; displayName?: string; avatarUrl?: string; phone?: string; address?: string; profileVisible?: boolean; showRatings?: boolean }): Promise<User> {
    return firstValueFrom(this.api.patch<User>('/users/me', payload));
  }

  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.api.put<void>('/users/me/password', { oldPassword, newPassword }));
  }

  async deleteMyAccount(): Promise<void> {
    await firstValueFrom(this.api.delete('/users/me')); 
  }

  async requestVerification(payload: { address: string; phone: string }): Promise<User> {
    return firstValueFrom(this.api.post<User>('/users/verification-request', payload));
  }

  async approveVerification(userId: string): Promise<User> {
    return firstValueFrom(this.api.post<User>(`/users/${encodeURIComponent(userId)}/approve-verification`, {}));
  }

  async revokeVerification(userId: string): Promise<User> {
    return firstValueFrom(this.api.post<User>(`/users/${encodeURIComponent(userId)}/revoke-verification`, {}));
  }

  async vouchForUser(userId: string): Promise<User> {
    return firstValueFrom(this.api.post<User>(`/users/${encodeURIComponent(userId)}/vouch`, {}));
  }

  async setup2FA(): Promise<{ secret: string; qrCode: string }> {
    return firstValueFrom(this.api.post<any>('/users/2fa/setup', {}));
  }

  async verify2FASetup(code: string): Promise<any> {
    return firstValueFrom(this.api.post<any>('/users/2fa/verify', { code }));
  }

  async disable2FA(): Promise<any> {
    return firstValueFrom(this.api.post<any>('/users/2fa/disable', {}));
  }

  async getOnlineUserIds(): Promise<string[]> {
    try {
      const list = await firstValueFrom(this.api.get<any[]>('/users/online'));
      return (Array.isArray(list) ? list : []).map(String);
    } catch {
      return [];
    }
  }

  async approveRequest(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/approve`, {}));
  }

  async denyRequest(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/deny`, {}));
  }

  async markReadyForPickup(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/ready-for-pickup`, {}));
  }

  async markPickedUp(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/picked-up`, {}));
  }

  async requestAdminReturn(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/request-admin-return`, {}));
  }

  async returnItem(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/listings/${encodeURIComponent(listingId)}/return`, {}));
  }

  async deleteListing(listingId: string): Promise<void> {
    await firstValueFrom(this.api.delete(`/listings/${encodeURIComponent(listingId)}`));
  }

  async reportListing(listingId: string, reason: string, details?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/report`, { reason, details: details ?? '' }));
  }

  async initiateReturnSession(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/initiate`, {}));
  }

  async submitReturnRequest(listingId: string, payload: { returnMethod: 'QR_CODE' | 'MANUAL'; qrCode?: string | null; itemNumber?: string | null; returnPlace?: string | null; returnAddress?: string | null }): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/submit`, payload));
  }

  async getReturnSession(listingId: string): Promise<any> {
    return firstValueFrom(this.api.get<any>(`/listings/${encodeURIComponent(listingId)}/return`));
  }

  async scanReturnQrCode(listingId: string, qrCode: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/scan`, { qrCode }));
  }

  async manualReturnFallback(listingId: string, itemNumber: string, conciergeWitnessId?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/manual`, { itemNumber, conciergeWitnessId: conciergeWitnessId ?? null }));
  }

  async acceptReturnRequest(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/accept`, {}));
  }

  async initiateReturnDispute(listingId: string, reason: string, photoUrl?: string, conciergeWitnessId?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/listings/${encodeURIComponent(listingId)}/return/dispute`, { reason, photoUrl: photoUrl ?? null, conciergeWitnessId: conciergeWitnessId ?? null }));
  }

  async getAdminSummary(): Promise<any> {
    return firstValueFrom(this.api.get<any>('/admin/summary'));
  }

  async adminListUsers(params: { q?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const q = params.q ? `&q=${encodeURIComponent(params.q)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/users?page=${page}&size=${size}${q}`));
  }

  async adminSetUserStatus(userId: string, status: string): Promise<any> {
    return firstValueFrom(this.api.patch<any>(`/admin/users/${encodeURIComponent(userId)}/status`, { status }));
  }

  async adminDeleteUser(userId: string): Promise<any> {
    return firstValueFrom(this.api.delete<any>(`/admin/users/${encodeURIComponent(userId)}`));
  }

  async adminListListings(params: { status?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const status = params.status ? `&status=${encodeURIComponent(params.status)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/listings?page=${page}&size=${size}${status}`));
  }

  async adminListPartnerListingRequests(params: { page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/partner/listing-requests?page=${page}&size=${size}`));
  }

  async adminListPartnerSubmissions(params: { page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/partner/submissions?page=${page}&size=${size}`));
  }

  async adminListPartnerListings(params: { status?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const status = params.status ? `&status=${encodeURIComponent(params.status)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/partner/listings?page=${page}&size=${size}${status}`));
  }

  async adminListPartnerItems(params: { status?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const status = params.status ? `&status=${encodeURIComponent(params.status)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/partner/items?page=${page}&size=${size}${status}`));
  }

  async adminActivatePartnerItem(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/items/${encodeURIComponent(listingId)}/activate`, {}));
  }

  async adminDeactivatePartnerItem(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/items/${encodeURIComponent(listingId)}/deactivate`, {}));
  }

  async adminListPartnerBorrowRequests(params: { page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/partner/borrow-requests?page=${page}&size=${size}`));
  }

  async adminBlockListing(listingId: string, blocked: boolean): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/listings/${encodeURIComponent(listingId)}/block`, { blocked }));
  }

  async adminDeleteListing(listingId: string): Promise<any> {
    return firstValueFrom(this.api.delete<any>(`/admin/listings/${encodeURIComponent(listingId)}`));
  }

  async adminApprovePartnerListing(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/listings/${encodeURIComponent(listingId)}/approve`, {}));
  }

  async adminRejectPartnerListing(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/listings/${encodeURIComponent(listingId)}/reject`, {}));
  }

  async adminApprovePartnerSubmission(listingId: string, note?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/submissions/${encodeURIComponent(listingId)}/approve`, note ? { note } : {}));
  }

  async adminRejectPartnerSubmission(listingId: string, reason?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/submissions/${encodeURIComponent(listingId)}/reject`, reason ? { reason } : {}));
  }

  async adminApprovePartnerBorrowRequest(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/borrow-requests/${encodeURIComponent(listingId)}/approve`, {}));
  }

  async adminRejectPartnerBorrowRequest(listingId: string, reason?: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/partner/borrow-requests/${encodeURIComponent(listingId)}/reject`, reason ? { reason } : {}));
  }

  async adminListTransactions(params: { status?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const status = params.status ? `&status=${encodeURIComponent(params.status)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/transactions?page=${page}&size=${size}${status}`));
  }

  async adminDeleteTransaction(transactionId: string): Promise<any> {
    return firstValueFrom(this.api.delete<any>(`/admin/transactions/${encodeURIComponent(transactionId)}`));
  }

  async adminRetryTransactionRelease(transactionId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/transactions/${encodeURIComponent(transactionId)}/retry-release`, {}));
  }

  async adminListSubscriptions(params: { status?: string; page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const status = params.status ? `&status=${encodeURIComponent(params.status)}` : '';
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/subscriptions?page=${page}&size=${size}${status}`));
  }

  async adminListDisputes(params: { page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 20;
    return firstValueFrom(this.api.get<any>(`/admin/disputes?page=${page}&size=${size}`));
  }

  async adminGetAppSettings(): Promise<any> {
    return firstValueFrom(this.api.get<any>('/admin/app-settings'));
  }

  async adminUpdateAppSettings(updates: { key: string; value: any }[]): Promise<any> {
    return firstValueFrom(this.api.put<any>('/admin/app-settings', { updates: updates || [] }));
  }

  async adminProvisionStripeSubscriptions(payload?: {
    currency?: string;
    plusAmountCents?: number;
    proAmountCents?: number;
    plusTrialDays?: number;
    proTrialDays?: number;
  }): Promise<any> {
    return firstValueFrom(this.api.post<any>('/admin/stripe/provision-subscriptions', payload || {}));
  }

  async adminListExchangeLocations(): Promise<ExchangeLocation[]> {
    return firstValueFrom(this.api.get<ExchangeLocation[]>('/admin/pickup-locations'));
  }

  async adminCreateExchangeLocation(payload: {
    name: string;
    address?: string | null;
    streetAddress?: string | null;
    city?: string | null;
    postalCode?: string | null;
    country?: string | null;
    latitude?: number | null;
    longitude?: number | null;
    operatingTimeFrom?: string | null;
    operatingTimeTo?: string | null;
    active?: boolean | null;
  }): Promise<ExchangeLocation> {
    return firstValueFrom(this.api.post<ExchangeLocation>('/admin/pickup-locations', payload));
  }

  async adminUpdateExchangeLocation(id: string, payload: {
    name?: string | null;
    address?: string | null;
    streetAddress?: string | null;
    city?: string | null;
    postalCode?: string | null;
    country?: string | null;
    latitude?: number | null;
    longitude?: number | null;
    operatingTimeFrom?: string | null;
    operatingTimeTo?: string | null;
    active?: boolean | null;
  }): Promise<ExchangeLocation> {
    return firstValueFrom(this.api.put<ExchangeLocation>(`/admin/pickup-locations/${encodeURIComponent(id)}`, payload));
  }

  async adminDeleteExchangeLocation(id: string): Promise<void> {
    await firstValueFrom(this.api.delete<any>(`/admin/pickup-locations/${encodeURIComponent(id)}`));
  }

  async adminCancelAndRefundDispute(listingId: string, reason: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/disputes/${encodeURIComponent(listingId)}/cancel-refund`, { reason }));
  }

  async adminAcceptReturnDispute(listingId: string, reason: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/returns/${encodeURIComponent(listingId)}/accept`, { reason }));
  }

  async adminReopenReturn(listingId: string, minutes?: number): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/admin/returns/${encodeURIComponent(listingId)}/reopen`, { minutes: typeof minutes === 'number' ? minutes : null }));
  }

  async adminGetReports(): Promise<any[]> {
    return firstValueFrom(this.api.get<any[]>('/admin/reports'));
  }

  async adminListReports(params: { page?: number; size?: number }): Promise<{ items: any[]; total: number; page: number; size: number }> {
    const page = typeof params.page === 'number' ? params.page : 0;
    const size = typeof params.size === 'number' ? params.size : 50;
    try {
      const res = await firstValueFrom(this.api.get<any>(`/admin/reports?page=${page}&size=${size}`));
      if (res && Array.isArray(res.items)) {
        return {
          items: res.items,
          total: typeof res.total === 'number' ? res.total : Number(res.total || 0),
          page: typeof res.page === 'number' ? res.page : page,
          size: typeof res.size === 'number' ? res.size : size,
        };
      }
      if (Array.isArray(res)) {
        return { items: res, total: res.length, page, size };
      }
    } catch { }
    const list = await this.adminGetReports();
    const items = Array.isArray(list) ? list : [];
    return { items: items.slice(page * size, page * size + size), total: items.length, page, size };
  }

  async adminDeleteReport(reportId: string): Promise<any> {
    return firstValueFrom(this.api.delete<any>(`/admin/reports/${encodeURIComponent(reportId)}`));
  }

  // --- Mailbox / Messages ---
  async getInbox(): Promise<Message[]> {
    return firstValueFrom(this.api.get<Message[]>('/messages/inbox'));
  }

  async getOutbox(): Promise<Message[]> {
    return firstValueFrom(this.api.get<Message[]>('/messages/outbox'));
  }
}
