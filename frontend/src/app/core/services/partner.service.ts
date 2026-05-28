import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { Partner, PartnerBorrowRequest, PartnerReturnRequest, PartnerSettings, Listing } from '../models/types';

@Injectable({
  providedIn: 'root'
})
export class PartnerService {
  private api = inject(ApiClientService);

  async getMyPartners(): Promise<Partner[]> {
    const list = await firstValueFrom(this.api.get<any[]>('/partner/my-partners'));
    return Array.isArray(list) ? (list as any) : [];
  }

  async getListings(): Promise<Listing[]> {
    const list = await firstValueFrom(this.api.get<any[]>('/partner/listings'));
    return Array.isArray(list) ? (list as any) : [];
  }

  async addListing(payload: any): Promise<any> {
    return firstValueFrom(this.api.post<any>('/partner/listings', payload));
  }

  async updateListing(listingId: string, payload: any): Promise<any> {
    return firstValueFrom(this.api.put<any>(`/partner/listings/${encodeURIComponent(listingId)}`, payload));
  }

  async deleteListing(listingId: string): Promise<void> {
    await firstValueFrom(this.api.delete(`/partner/listings/${encodeURIComponent(listingId)}`));
  }

  async getRequests(): Promise<PartnerBorrowRequest[]> {
    const list = await firstValueFrom(this.api.get<any[]>('/partner/requests'));
    return Array.isArray(list) ? (list as any) : [];
  }

  async approveRequest(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/partner/requests/${encodeURIComponent(listingId)}/approve`, {}));
  }

  async rejectRequest(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/partner/requests/${encodeURIComponent(listingId)}/reject`, {}));
  }

  async getPendingManualReturns(): Promise<PartnerReturnRequest[]> {
    const list = await firstValueFrom(this.api.get<any[]>('/partner/returns/manual/pending'));
    return Array.isArray(list) ? (list as any) : [];
  }

  async acceptManualReturn(listingId: string): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/partner/returns/${encodeURIComponent(listingId)}/accept`, {}));
  }

  async denyManualReturn(listingId: string, reason?: string | null): Promise<any> {
    return firstValueFrom(this.api.post<any>(`/partner/returns/${encodeURIComponent(listingId)}/deny`, { reason: reason ?? null }));
  }

  async getSettings(partnerId?: string | null): Promise<PartnerSettings> {
    const params: any = {};
    if (partnerId) params.partnerId = partnerId;
    return firstValueFrom(this.api.get<PartnerSettings>('/partner/settings', { params }));
  }

  async updateSettings(payload: PartnerSettings): Promise<PartnerSettings> {
    const params: any = {};
    if (payload?.partnerId) params.partnerId = payload.partnerId;
    return firstValueFrom(this.api.put<PartnerSettings>('/partner/settings', payload, { params }));
  }
}
