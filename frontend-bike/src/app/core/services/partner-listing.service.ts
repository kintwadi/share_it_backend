import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PartnerListingForm, PartnerListingSummary } from '../models/commerce.models';
import { ApiClientService } from './api-client.service';

@Injectable({ providedIn: 'root' })
export class PartnerListingService {
  private readonly api = inject(ApiClientService);

  async listMine(): Promise<PartnerListingSummary[]> {
    return firstValueFrom(this.api.get<PartnerListingSummary[]>('/partner/listings'));
  }

  async create(payload: PartnerListingForm): Promise<PartnerListingSummary> {
    return firstValueFrom(this.api.post<PartnerListingSummary>('/partner/listings', payload));
  }
}
