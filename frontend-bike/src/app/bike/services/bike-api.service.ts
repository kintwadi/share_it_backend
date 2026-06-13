import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from '../../core/services/api-client.service';
import { BikeDetail, BikePage, RentToOwnQuote } from '../models/bike';

@Injectable({
  providedIn: 'root'
})
export class BikeApiService {
  private api = inject(ApiClientService);

  async search(params: { search?: string; frameSize?: string; bikeType?: string; page?: number; size?: number }): Promise<BikePage> {
    const query = {
      search: params.search ?? '',
      frameSize: params.frameSize ?? '',
      bikeType: params.bikeType ?? '',
      page: params.page ?? 0,
      size: params.size ?? 12
    };
    return firstValueFrom(this.api.get<BikePage>('/bikes', { params: query }));
  }

  async getById(id: string): Promise<BikeDetail> {
    return firstValueFrom(this.api.get<BikeDetail>(`/bikes/${encodeURIComponent(id)}`));
  }

  async getRentToOwnQuote(id: string): Promise<RentToOwnQuote> {
    return firstValueFrom(this.api.get<RentToOwnQuote>(`/bikes/${encodeURIComponent(id)}/rent-to-own/quote`));
  }
}
