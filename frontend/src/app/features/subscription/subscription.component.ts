import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { LucideAngularModule, Shield, Star, ArrowLeft, CheckCircle2 } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User } from '../../core/models/types';
import { ButtonComponent } from '../../shared/components/button/button';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ButtonComponent],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.css',
})
export class SubscriptionComponent implements OnInit {
  // PLATFORM SUBSCRIPTION ONLY:
  // This screen is for the legacy platform/lender plan chooser.
  // It is separate from the borrower subscription flow used during borrowing.
  router = inject(Router);
  route = inject(ActivatedRoute);
  api = inject(ApiService);
  cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly Shield = Shield;
  readonly Star = Star;
  readonly ArrowLeft = ArrowLeft;
  readonly CheckCircle2 = CheckCircle2;

  user: User | null = null;
  loading = true;
  config = { starter: true, plus: true, pro: true };
  requiredPlan: 'plus' | 'pro' | undefined;
  fromUpgrade = false;
  error: string | null = null;
  currentPlan: 'starter' | 'plus' | 'pro' | null = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch {}
  }

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      const qp = (params['requiredPlan'] || params['plan']) as any;
      if (qp) this.requiredPlan = qp;
      const from = params['fromUpgrade'];
      if (from === '1' || from === 'true') this.fromUpgrade = true;
    });

    try {
      const state = this.router.getCurrentNavigation()?.extras?.state as any;
      const persisted = (history.state || {}) as any;
      const combined = { ...persisted, ...state };
      const required = state?.requiredPlan as any;
      if (required) this.requiredPlan = required;
      this.fromUpgrade = combined?.fromUpgrade === true;
    } catch {}

    this.api.getSubscriptionConfig().then((cfg) => {
      this.config = cfg;
      this.render();
    });

    this.api.getCurrentUser().then((u) => {
      this.user = u;
      this.loading = false;
      this.render();
      if (!u) {
        this.router.navigate(['/connect']);
      }
      this.api.getCurrentSubscription().then((sub) => {
        const pt = String(sub?.planType || '').toLowerCase();
        if (pt === 'starter' || pt === 'plus' || pt === 'pro' || pt === 'premium_lender') {
          this.currentPlan = pt === 'premium_lender' ? 'pro' : (pt as any);
        } else {
          this.currentPlan = null;
        }
        this.render();
      });
    });
  }

  get enabledCount() {
    return (this.config.starter ? 1 : 0) + (this.config.plus ? 1 : 0) + (this.config.pro ? 1 : 0);
  }

  get gridClass() {
    if (this.enabledCount === 1) return 'grid md:grid-cols-1 max-w-sm mx-auto gap-4 items-stretch';
    if (this.enabledCount === 2) return 'grid md:grid-cols-2 max-w-4xl mx-auto gap-4 items-stretch';
    return 'grid md:grid-cols-3 gap-4 items-stretch';
  }

  async handleStarterSelect() {
    this.router.navigate(['/verification/email'], { queryParams: { plan: 'starter' } });
  }

  isCurrentPlan(plan: 'starter' | 'plus' | 'pro') {
    return this.currentPlan === plan;
  }

  goToCheckout(plan: string) {
    const qp: any = { plan };
    if (this.fromUpgrade) qp.fromUpgrade = 1;
    this.router.navigate(['/subscription/confirm'], {
      queryParams: qp,
      state: { fromUpgrade: this.fromUpgrade } as any,
    });
  }

  goBack() {
    this.router.navigate(['/']);
  }
}
