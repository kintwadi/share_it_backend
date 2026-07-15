import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, Shield, ArrowLeft, Loader2, CheckCircle2, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User, UserRole } from '../../core/models/types';
import { LayoutModeService } from '../../core/services/layout-mode.service';

@Component({
  selector: 'app-borrower-subscription',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './borrower-subscription.component.html',
  styleUrl: './borrower-subscription.component.css'
})
export class BorrowerSubscriptionComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);
  layoutMode = inject(LayoutModeService);

  readonly Shield = Shield;
  readonly ArrowLeft = ArrowLeft;
  readonly Loader2 = Loader2;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;

  user: User | null = null;
  loading = true;
  starting = false;
  error: string | null = null;
  currentBorrowingSub: any | null = null;
  plusTrialDays = 14;
  plusMonthlyAmountCents = 499;
  subscriptionCurrency = 'EUR';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    try {
      const u = await this.api.getCurrentUser();
      this.user = u;
      if (!u || u.role === UserRole.ADMIN) {
        this.router.navigate(['/dashboard']);
        return;
      }
      try {
        const [cfg, subscription] = await Promise.all([
          this.api.getPublicConfig(),
          this.api.getCurrentBorrowingSubscription().catch(() => null)
        ]);
        const sub = cfg?.subscription || {};
        const td = Number(sub?.plusTrialDays);
        const cents = Number(sub?.plusMonthlyAmountCents);
        const curr = String(sub?.currency || '');
        if (!Number.isNaN(td) && td > 0) this.plusTrialDays = td;
        if (!Number.isNaN(cents) && cents >= 0) this.plusMonthlyAmountCents = cents;
        if (curr) this.subscriptionCurrency = curr;
        this.currentBorrowingSub = subscription;
      } catch { }
    } finally {
      this.loading = false;
      this.render();
    }
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  get hasActiveSubscription(): boolean {
    const sub = this.currentBorrowingSub;
    if (!sub) return false;
    if (typeof sub?.borrowDirectly === 'boolean') return sub.borrowDirectly;
    if (typeof sub?.active === 'boolean' && sub.active) return true;
    const status = String(sub?.status || '').trim().toLowerCase();
    return status === 'active' || status === 'trialing' || status === 'trial_active';
  }

  get monthlyLabel(): string {
    const amount = (this.plusMonthlyAmountCents || 0) / 100;
    const curr = String(this.subscriptionCurrency || '').toUpperCase();
    const formatted = amount.toFixed(2);
    if (curr === 'EUR') return `EUR ${formatted}`;
    if (curr === 'USD') return `USD ${formatted}`;
    if (curr === 'GBP') return `GBP ${formatted}`;
    return `${curr} ${formatted}`;
  }

  private get languageKey(): string {
    return String(this.i18n.language() || 'en').toLowerCase();
  }

  get pageTitle(): string {
    return this.hasActiveSubscription
      ? this.i18n.t('borrowerSub.title_active')
      : this.i18n.t('borrowerSub.title');
  }

  get pageSubtitle(): string {
    return this.hasActiveSubscription
      ? this.i18n.t('borrowerSub.subtitle_active')
      : this.i18n.t('borrowerSub.subtitle');
  }

  get planSummary(): string {
    const days = this.plusTrialDays;
    const price = this.monthlyLabel;
    if (this.languageKey.startsWith('pt')) {
      return `${days} dias grátis de teste, depois ${price} por mês`;
    }
    if (this.languageKey.startsWith('de')) {
      return `${days} kostenlose Testtage, danach ${price} pro Monat`;
    }
    return `${days} free trial days, then ${price} per month`;
  }

  get benefitItems(): string[] {
    return [
      this.i18n.t('borrowerSub.benefit_1'),
      this.i18n.t('borrowerSub.benefit_2')
    ];
  }

  async startSubscription() {
    if (this.starting || this.hasActiveSubscription || !this.user) return;
    this.starting = true;
    this.error = null;
    this.render();
    try {
      await this.api.sendBorrowingSubscriptionVerificationCode(this.i18n.language());
      await this.router.navigate(['/verification/email'], {
        queryParams: {
          plan: 'verified',
          scope: 'borrower',
          from: '/dashboard',
          email: this.user.email || '',
          trialDays: this.plusTrialDays,
          sent: '1'
        }
      });
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('borrowerSub.start_error');
      this.starting = false;
      this.render();
    }
  }

  manageSubscription() {
    this.router.navigate(['/settings'], { queryParams: { tab: 'subscription' } });
  }
}
