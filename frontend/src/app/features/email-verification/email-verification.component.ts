import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Mail, ArrowLeft, Loader2, RefreshCcw, CheckCircle2, AlertTriangle, Shield, Info } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { SubscriptionFeatureService } from '../../core/services/subscription-feature.service';

@Component({
  selector: 'app-email-verification',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.css'
})
export class EmailVerificationComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);
  private settingsConfig = inject(SettingsConfigService);
  subscriptionFeature = inject(SubscriptionFeatureService);

  readonly Mail = Mail;
  readonly ArrowLeft = ArrowLeft;
  readonly Loader2 = Loader2;
  readonly RefreshCcw = RefreshCcw;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;
  readonly Shield = Shield;
  readonly Info = Info;

  @ViewChildren('digitInput') digitInputs!: QueryList<ElementRef<HTMLInputElement>>;

  plan = 'plus';
  digits: string[] = ['', '', '', ''];
  userEmail = '';
  loading = true;
  submitting = false;
  sending = false;
  error: string | null = null;
  sent = false;
  resendSeconds = 0;
  private timer: any = null;

  trackByIndex(index: number) {
    return index;
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    await this.settingsConfig.ensureLoaded();
    if (!this.subscriptionFeature.enabled()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.route.queryParams.subscribe(params => {
      this.plan = params['plan'] || 'plus';
      this.render();
    });

    this.api.getCurrentUser().then(u => {
      if (!u) {
        this.router.navigate(['/connect']);
        return;
      }
      this.userEmail = u.email || '';
      this.bootstrap();
    }).catch(() => this.router.navigate(['/connect']));
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  async bootstrap() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      await this.sendCode();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private startResendCooldown(seconds: number) {
    this.resendSeconds = seconds;
    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => {
      this.resendSeconds = Math.max(0, this.resendSeconds - 1);
      this.render();
      if (this.resendSeconds === 0 && this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    }, 1000);
  }

  async sendCode() {
    if (this.resendSeconds > 0) return;
    this.sending = true;
    this.error = null;
    this.render();
    try {
      await this.api.sendSubscriptionVerificationCode(this.plan, this.i18n.language());
      this.sent = true;
      this.startResendCooldown(30);
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('verification.email.send_failed');
    } finally {
      this.sending = false;
      this.render();
    }
  }

  private focusIndex(index: number) {
    try {
      const el = this.digitInputs?.get(index)?.nativeElement;
      if (el) el.focus();
    } catch { }
  }

  onDigitChange(index: number, v: string) {
    const value = (v || '').replace(/\D/g, '').slice(0, 1);
    const next = [...this.digits];
    next[index] = value;
    this.digits = next;
    this.render();
    if (value && index < 3) {
      this.focusIndex(index + 1);
    }
    if (this.digits.every(d => !!d)) {
      this.verifyAndCheckout();
    }
  }

  onDigitKeyDown(index: number, event: KeyboardEvent) {
    if (event.key === 'Backspace' && !this.digits[index] && index > 0) {
      this.focusIndex(index - 1);
    }
  }

  get codeValue(): string {
    return this.digits.join('');
  }

  async verifyAndCheckout() {
    if (this.submitting) return;
    if (this.digits.some(d => !d)) return;
    this.submitting = true;
    this.error = null;
    this.render();
    try {
      await this.api.verifySubscriptionVerificationCode(this.codeValue);

      if (this.plan === 'starter') {
        await this.api.subscribeStarter();
        this.router.navigate(['/dashboard']);
        return;
      }

      const { url, sessionId } = await this.api.createSubscriptionCheckoutSession(this.plan, '/dashboard');
      if (url) {
        window.location.href = url;
        return;
      }
      if (!sessionId) {
        this.error = this.i18n.t('verification.email.session_create_failed');
      } else {
        this.error = this.i18n.t('verification.email.session_url_missing');
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('verification.email.invalid_code');
    } finally {
      this.submitting = false;
      this.render();
    }
  }

  goBack() {
    this.router.navigate(['/subscription/confirm'], { queryParams: { plan: this.plan } });
  }
}
