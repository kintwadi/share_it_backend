import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from '../core/services/api-client.service';
import {
  FahrradFuchsToGoBooking,
  FahrradFuchsToGoCheckoutRequest,
  FahrradFuchsToGoCheckoutResponse,
  FahrradFuchsToGoListingDetail,
  FahrradFuchsToGoStorefrontResponse
} from './fahrrad-fuchs-to-go.models';

@Injectable({ providedIn: 'root' })
export class FahrradFuchsToGoApiService {
  private readonly api = inject(ApiClientService);

  async getStorefront(): Promise<FahrradFuchsToGoStorefrontResponse> {
    return firstValueFrom(this.api.get<FahrradFuchsToGoStorefrontResponse>('/fahrad-fuchs/bikes'));
  }

  async getBike(slug: string): Promise<FahrradFuchsToGoListingDetail> {
    return firstValueFrom(this.api.get<FahrradFuchsToGoListingDetail>(`/fahrad-fuchs/bikes/${encodeURIComponent(slug)}`));
  }

  async checkout(slug: string, payload: FahrradFuchsToGoCheckoutRequest): Promise<FahrradFuchsToGoCheckoutResponse> {
    return firstValueFrom(this.api.post<FahrradFuchsToGoCheckoutResponse>(`/fahrad-fuchs/bikes/${encodeURIComponent(slug)}/checkout`, payload));
  }

  async getBookings(): Promise<FahrradFuchsToGoBooking[]> {
    return firstValueFrom(this.api.get<FahrradFuchsToGoBooking[]>('/fahrad-fuchs/bookings'));
  }
}
