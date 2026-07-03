import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CheckoutSessionResponse, SubscriptionConfig } from '../models/commerce.models';
import { ApiClientService } from './api-client.service';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly api = inject(ApiClientService);

  async getConfig(): Promise<SubscriptionConfig> {
    return firstValueFrom(this.api.get<SubscriptionConfig>('/subscriptions/config'));
  }

  async createCheckoutSession(planType: string, returnPath: string): Promise<CheckoutSessionResponse> {
    return firstValueFrom(this.api.post<CheckoutSessionResponse>('/subscriptions/create-checkout-session', {
      planType,
      returnPath
    }));
  }
}
