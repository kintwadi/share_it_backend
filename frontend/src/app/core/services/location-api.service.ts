import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from './api-client.service';

export interface LocationResponse {
  displayName?: string | null;
  streetAddress?: string | null;
  city?: string | null;
  postalCode?: string | null;
  country?: string | null;
  countryCode?: string | null;
  latitude?: number | null;
  longitude?: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class LocationApiService {
  private api = inject(ApiClientService);

  async reverseGeocode(lat: number, lng: number): Promise<LocationResponse> {
    return firstValueFrom(
      this.api.get<LocationResponse>('/location/reverse', { params: { lat, lng } })
    );
  }

  async autocomplete(query: string, countryCodes?: string, limit: number = 5): Promise<LocationResponse[]> {
    return firstValueFrom(
      this.api.get<LocationResponse[]>('/location/autocomplete', {
        params: {
          q: query,
          ...(countryCodes ? { countryCodes } : {}),
          limit
        }
      })
    );
  }
}

