import { Injectable, computed, inject } from '@angular/core';
import { SessionService } from './session.service';
import { SettingsConfigService } from './settings-config.service';

@Injectable({
  providedIn: 'root'
})
export class SubscriptionFeatureService {
  // PLATFORM SUBSCRIPTION ONLY:
  // These computed values describe the legacy platform/lender plan state.
  // Borrower subscription checks belong to the borrower flow components/services.
  private settings = inject(SettingsConfigService);
  private session = inject(SessionService);

  enabled = computed(() => this.settings.isSectionEnabled('enable', 'subscription'));

  hasEntitlement = computed(() => {
    if (!this.session.user()) return false;
    if (!this.enabled()) return true;
    const sub = this.session.subscription();
    const status = String(sub?.status || '').toLowerCase();
    return status === 'active' || status === 'trialing' || status === 'trial_active';
  });

  effectivePlan = computed<'starter' | 'plus' | 'pro'>(() => {
    if (!this.enabled()) return 'pro';
    const sub = this.session.subscription();
    const pt = String(sub?.planType || '').toLowerCase();
    if (pt === 'premium_lender') return 'pro';
    if (pt === 'starter' || pt === 'plus' || pt === 'pro') return pt as any;
    return 'starter';
  });

  constructor() {
    this.settings.ensureLoaded();
  }
}

