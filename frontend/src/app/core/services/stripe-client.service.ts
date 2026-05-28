import { Injectable, inject } from '@angular/core';
import { Stripe, loadStripe } from '@stripe/stripe-js';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class StripeClientService {
  private api = inject(ApiService);
  private stripePromise: Promise<Stripe | null> | null = null;

  async getStripe(): Promise<Stripe | null> {
    if (this.stripePromise) return this.stripePromise;
    this.stripePromise = this.initStripe();
    return this.stripePromise;
  }

  private async initStripe(): Promise<Stripe | null> {
    try {
      const cfg = await this.api.getPublicConfig();
      const pk = String(cfg?.stripePublicKey || '').trim();
      if (!pk) return null;
      return await loadStripe(pk);
    } catch {
      return null;
    }
  }
}
