import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Shield, CheckCircle2, CreditCard, ArrowLeft, Loader2, Info } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { ButtonComponent } from '../../shared/components/button/button';
import { SessionService } from '../../core/services/session.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';

@Component({
  selector: 'app-subscription-checkout',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ButtonComponent],
  templateUrl: './subscription-checkout.component.html',
  styleUrl: './subscription-checkout.component.css'
})
export class SubscriptionCheckoutComponent implements OnInit {
  // PLATFORM SUBSCRIPTION ONLY:
  // This Stripe checkout screen is for the legacy platform/lender subscription flow.
  // Backend checkout-session creation is intentionally disabled to avoid accidental
  // real Stripe charges while the platform subscription feature is turned off.
  route = inject(ActivatedRoute);
  router = inject(Router);
  api = inject(ApiService);
  cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);
  session = inject(SessionService);
  settingsConfig = inject(SettingsConfigService);

  readonly Shield = Shield;
  readonly CheckCircle2 = CheckCircle2;
  readonly CreditCard = CreditCard;
  readonly ArrowLeft = ArrowLeft;
  readonly Loader2 = Loader2;
  readonly Info = Info;

  stripeAvailable = false;
  loading = true;
  redirecting = false;
  error: string | null = null;
  plan = 'plus';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['plan']) {
        this.plan = params['plan'];
        this.render();
      }
    });

    this.route.queryParams.subscribe(params => {
      const sessionId = String(params['session_id'] || '');
      if (sessionId) {
        this.handleStripeReturn(sessionId);
      }
    });

    this.api.getCurrentUser().then(u => {
      if (!u) {
        this.router.navigate(['/connect']);
        return;
      }
      this.initStripe();
    }).catch(() => {
      this.router.navigate(['/connect']);
    });
  }

  async initStripe() {
    try {
      const config = await this.api.getPublicConfig();
      if (config.stripePublicKey) {
        this.stripeAvailable = true;
      } else {
        this.error = this.i18n.t('subscription.checkout.stripe_unavailable');
      }
    } catch {
      this.error = this.i18n.t('subscription.checkout.init_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  get isPro() {
    return this.plan === 'pro';
  }

  get priceLabel() {
    return this.isPro ? this.i18n.t('subscription.checkout.price_pro') : this.i18n.t('subscription.checkout.price_plus');
  }

  async handleCheckout() {
    if (!this.stripeAvailable) return;
    this.redirecting = true;
    this.error = null;
    this.render();
    
    try {
      const { url, sessionId } = await this.api.createSubscriptionCheckoutSession(this.plan, '/subscription/checkout');
      
      if (url) {
        window.location.href = url;
      } else if (sessionId) {
        this.error = this.i18n.t('subscription.checkout.session_url_missing');
        this.redirecting = false;
      } else {
        this.error = this.i18n.t('subscription.checkout.session_create_failed');
        this.redirecting = false;
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('subscription.checkout.failed');
      this.redirecting = false;
      this.render();
    }
  }

  private async handleStripeReturn(sessionId: string) {
    if (!sessionId) return;
    if (this.redirecting) return;
    this.redirecting = true;
    this.error = null;
    this.render();
    try {
      await this.api.syncSubscriptionFromSession(sessionId);
      await this.session.refresh();
      await this.settingsConfig.reload();
      await this.router.navigate(['/dashboard'], { state: { upgradeSuccess: true }, replaceUrl: true });
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('subscription.checkout.failed');
      this.redirecting = false;
      this.render();
    }
  }

  goBack() {
    this.router.navigate(['/subscription/confirm'], { queryParams: { plan: this.plan } });
  }
}
