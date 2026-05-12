import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ShieldCheck, CheckCircle2, ArrowLeft, Info, Shield, Loader2 } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-subscription-confirm',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './subscription-confirm.component.html',
  styleUrl: './subscription-confirm.component.css'
})
export class SubscriptionConfirmComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ShieldCheck = ShieldCheck;
  readonly CheckCircle2 = CheckCircle2;
  readonly ArrowLeft = ArrowLeft;
  readonly Info = Info;
  readonly Shield = Shield;
  readonly Loader2 = Loader2;

  plan = 'plus';
  processing = false;
  error: string | null = null;
  fromUpgrade = false;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.plan = params['plan'] || 'plus';
      const from = params['fromUpgrade'];
      if (from === '1' || from === 'true') this.fromUpgrade = true;
      this.render();
    });

    try {
      const state = this.router.getCurrentNavigation()?.extras?.state as any;
      const persisted = (history.state || {}) as any;
      const combined = { ...persisted, ...state };
      this.fromUpgrade = combined?.fromUpgrade === true;
    } catch { }

    this.api.getCurrentUser().then(u => {
      if (!u) this.router.navigate(['/connect']);
    }).catch(() => this.router.navigate(['/connect']));
  }

  get isPro() {
    return this.plan === 'pro' || this.plan === 'premium';
  }

  get isPaidPlan() {
    return this.plan === 'plus' || this.plan === 'pro' || this.plan === 'premium';
  }

  get contentPlanKey(): 'starter' | 'plus' | 'pro' {
    if (!this.isPaidPlan) return 'starter';
    if (this.plan === 'premium') return 'pro';
    return this.plan === 'pro' ? 'pro' : 'plus';
  }

  get contentKeyPrefix(): string {
    return `lenderPost.${this.contentPlanKey}.`;
  }

  get planLabel(): string {
    if (this.plan === 'starter') return this.i18n.t('subscription.starter.name');
    if (this.plan === 'plus') return this.i18n.t('subscription.plus.name');
    if (this.plan === 'pro' || this.plan === 'premium') return this.i18n.t('subscription.pro.name');
    return this.plan;
  }

  async handlePrimary() {
    if (this.processing) return;
    this.processing = true;
    this.error = null;
    this.render();
    try {
      await this.api.sendSubscriptionVerificationCode(this.contentPlanKey, this.i18n.language());
      this.router.navigate(['/verification/email'], { queryParams: { plan: this.contentPlanKey } });
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('subscription.confirm.failed');
    } finally {
      this.processing = false;
      this.render();
    }
  }

  handleSecondary() {
    this.router.navigate(['/subscription'], { state: { fromUpgrade: this.fromUpgrade } as any });
  }
}
