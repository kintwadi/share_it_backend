import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { Listing, Partner, PartnerBorrowRequest, PartnerSettings } from '../models/types';

@Injectable({
  providedIn: 'root'
})
export class PartnerService {
  private api = inject(ApiClientService);

  async getMyPartners(): Promise<Partner[]> {
    return firstValueFrom(this.api.get<Partner[]>('/partner/my-partners'));
  }

  async registerPartner(payload: Omit<Partner, 'id' | 'status'>): Promise<Partner> {
    return firstValueFrom(this.api.post<Partner>('/partner/register', payload));
  }

  async addListing(payload: any): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>('/partner/listings', payload));
  }

  async getListings(): Promise<Listing[]> {
    return firstValueFrom(this.api.get<Listing[]>('/partner/listings'));
  }

  async updateListing(id: string, payload: any): Promise<Listing> {
    return firstValueFrom(this.api.put<Listing>(`/partner/listings/${encodeURIComponent(id)}`, payload));
  }

  async deleteListing(id: string): Promise<void> {
    await firstValueFrom(this.api.delete(`/partner/listings/${encodeURIComponent(id)}`));
  }

  async getRequests(): Promise<PartnerBorrowRequest[]> {
    return firstValueFrom(this.api.get<PartnerBorrowRequest[]>('/partner/requests'));
  }

  async approveRequest(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/partner/requests/${encodeURIComponent(listingId)}/approve`, {}));
  }

  async rejectRequest(listingId: string): Promise<Listing> {
    return firstValueFrom(this.api.post<Listing>(`/partner/requests/${encodeURIComponent(listingId)}/reject`, {}));
  }

  async getSettings(partnerId?: string | null): Promise<PartnerSettings> {
    const qs = partnerId ? `?partnerId=${encodeURIComponent(partnerId)}` : '';
    return firstValueFrom(this.api.get<PartnerSettings>(`/partner/settings${qs}`));
  }

  async updateSettings(payload: PartnerSettings): Promise<PartnerSettings> {
    const qs = payload.partnerId ? `?partnerId=${encodeURIComponent(payload.partnerId)}` : '';
    return firstValueFrom(this.api.put<PartnerSettings>(`/partner/settings${qs}`, payload));
  }
}

