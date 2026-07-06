import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ShieldCheck, ArrowLeft, Loader2, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-subscription-upgrade',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './subscription-upgrade.component.html',
  styleUrl: './subscription-upgrade.component.css'
})
export class SubscriptionUpgradeComponent implements OnInit {
  // PLATFORM SUBSCRIPTION ONLY:
  // This upgrade screen belongs to the legacy platform/lender plan model.
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  i18n = inject(I18nService);
  private cdr = inject(ChangeDetectorRef);

  readonly ShieldCheck = ShieldCheck;
  readonly ArrowLeft = ArrowLeft;
  readonly Loader2 = Loader2;
  readonly AlertTriangle = AlertTriangle;

  plan = 'premium';
  preview: any | null = null;
  loading = true;
  upgrading = false;
  error: string | null = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.plan = params['plan'] || 'premium';
      this.loadPreview();
    });
  }

  get displayPlanName(): string {
    return this.plan === 'premium' ? this.i18n.t('subscription.upgrade.plan_premium') : this.i18n.t('subscription.upgrade.plan_verified');
  }

  private normalizePlan(): string {
    if (this.plan === 'premium') return 'pro';
    if (this.plan === 'verified') return 'verified';
    return this.plan;
  }

  async loadPreview() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      const u = await this.api.getCurrentUser();
      if (!u) {
        this.router.navigate(['/connect']);
        return;
      }
      const newPlan = this.normalizePlan();
      const data = await this.api.previewSubscriptionUpgrade(newPlan);
      this.preview = data;
    } catch {
      this.error = this.i18n.t('subscription.upgrade.preview_failed');
      this.preview = null;
    } finally {
      this.loading = false;
      this.render();
    }
  }

  formatCents(cents: number): string {
    return this.i18n.formatPrice((cents || 0) / 100);
  }

  async confirm() {
    if (this.upgrading) return;
    this.upgrading = true;
    this.error = null;
    this.render();
    try {
      await this.api.confirmSubscriptionUpgrade(this.normalizePlan());
      this.router.navigate(['/dashboard'], { state: { upgradeSuccess: true } as any });
    } catch {
      this.error = this.i18n.t('subscription.upgrade.failed');
      this.upgrading = false;
      this.render();
    }
  }

  goBack() {
    this.router.navigate(['/settings'], { queryParams: { tab: 'subscription' } });
  }
}
