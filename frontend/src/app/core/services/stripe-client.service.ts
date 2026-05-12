import { Injectable } from '@angular/core';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class StripeClientService {
  private stripePromise: Promise<Stripe | null> | null = null;

  constructor(private api: ApiService) {}

  async getStripe(): Promise<Stripe | null> {
    if (this.stripePromise) return this.stripePromise;
    this.stripePromise = this.api.getPublicConfig()
      .then(cfg => {
        const key = cfg?.stripePublicKey;
        if (!key) return null;
        return loadStripe(key);
      })
      .catch(() => null);
    return this.stripePromise;
  }
}

