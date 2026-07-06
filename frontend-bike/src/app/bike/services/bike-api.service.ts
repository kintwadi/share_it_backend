import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from '../../core/services/api-client.service';
import {
  BikeAdminUpsertBikeRequest,
  BikeDetail,
  BikePage,
  BikeShopDetail,
  BikeShopSearchRequest,
  BikeShopSearchResponse,
  RentToOwnQuote
} from '../models/bike';

@Injectable({
  providedIn: 'root'
})
export class BikeApiService {
  private api = inject(ApiClientService);

  async search(params: {
    search?: string;
    city?: string;
    frameSize?: string;
    bikeType?: string;
    inventoryStatus?: string;
    sort?: string;
    page?: number;
    size?: number;
  }): Promise<BikePage> {
    const query = {
      search: params.search ?? '',
      city: params.city ?? '',
      frameSize: params.frameSize ?? '',
      bikeType: params.bikeType ?? '',
      inventoryStatus: params.inventoryStatus ?? '',
      sort: params.sort ?? 'newest',
      page: params.page ?? 0,
      size: params.size ?? 10
    };
    return firstValueFrom(this.api.get<BikePage>('/bikes', { params: query }));
  }

  async getById(id: string): Promise<BikeDetail> {
    return firstValueFrom(this.api.get<BikeDetail>(`/bikes/${encodeURIComponent(id)}`));
  }

  async searchShopCatalog(request: BikeShopSearchRequest): Promise<BikeShopSearchResponse> {
    return firstValueFrom(this.api.post<BikeShopSearchResponse>('/bikes/shop/search', request));
  }

  async getShopBike(id: number | string): Promise<BikeShopDetail> {
    return firstValueFrom(this.api.get<BikeShopDetail>(`/bikes/shop/${encodeURIComponent(String(id))}`));
  }

  async upsertShopBike(request: BikeAdminUpsertBikeRequest): Promise<BikeShopDetail> {
    return firstValueFrom(this.api.post<BikeShopDetail>('/bikes/admin/catalog', request));
  }

  async getRentToOwnQuote(id: string): Promise<RentToOwnQuote> {
    return firstValueFrom(this.api.get<RentToOwnQuote>(`/bikes/${encodeURIComponent(id)}/rent-to-own/quote`));
  }
}
