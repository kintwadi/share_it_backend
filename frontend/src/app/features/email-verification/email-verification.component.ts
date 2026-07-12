import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Mail, ArrowLeft, Loader2, RefreshCcw, CheckCircle2, AlertTriangle, Shield, Info } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { SubscriptionFeatureService } from '../../core/services/subscription-feature.service';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { SessionService } from '../../core/services/session.service';
import { LayoutModeService } from '../../core/services/layout-mode.service';

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
  private authStorage = inject(AuthStorageService);
  private session = inject(SessionService);
  layoutMode = inject(LayoutModeService);

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
  flow: 'subscription' | 'signup' = 'subscription';
  subscriptionScope: 'platform' | 'borrower' = 'platform';
  bookingListingId = '';
  bookingFrom = '';
  borrowerTrialDays = 14;
  verificationToken = '';
  subscriptionCodeAlreadySent = false;
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

  private mapSubscriptionVerificationError(error: any): string {
    const raw = String(error?.error?.error || error?.error?.message || error?.message || '').trim();
    switch (raw) {
      case 'invalid_verification_code':
      case 'verification_code_expired':
      case 'verification_code_already_used':
        return this.i18n.t('verification.email.invalid_code');
      case 'subscription_disabled':
      case 'borrower_subscription_disabled':
      case 'borrower_subscription_plan_disabled':
        return this.i18n.t('subscription.disabled');
      default:
        return raw || this.i18n.t('verification.email.invalid_code');
    }
  }

  private get languageKey(): string {
    return String(this.i18n.language() || 'en').toLowerCase();
  }

  get isBorrowerSubscriptionFlow(): boolean {
    return this.flow === 'subscription' && this.subscriptionScope === 'borrower';
  }

  get verificationHelpText(): string {
    if (!this.isBorrowerSubscriptionFlow) {
      return this.i18n.t('verification.email.help');
    }
    const days = this.borrowerTrialDays;
    if (this.languageKey.startsWith('pt')) {
      return `Introduza o codigo de 4 digitos para ativar a sua subscricao de emprestimo. O seu teste dura ${days} dias e o cartao nao sera cobrado pela subscricao ate ao fim desse periodo.`;
    }
    if (this.languageKey.startsWith('de')) {
      return `Gib den 4-stelligen Code ein, um dein Ausleih-Abo zu aktivieren. Dein Testzeitraum dauert ${days} Tage und deine Karte wird fuer das Abo erst nach diesem Zeitraum belastet.`;
    }
    return `Enter the 4-digit code to activate your borrowing subscription. Your trial lasts ${days} days, and your card will not be charged for the subscription until that trial ends.`;
  }

  get borrowerTrialNoticeText(): string | null {
    if (!this.isBorrowerSubscriptionFlow) return null;
    const days = this.borrowerTrialDays;
    if (this.languageKey.startsWith('pt')) {
      return `Vai ver o checkout depois da verificacao, mas esse checkout cria a subscricao com ${days} dias de teste. Hoje nao paga a subscricao nesse mesmo momento.`;
    }
    if (this.languageKey.startsWith('de')) {
      return `Nach der Bestaetigung wirst du zum Checkout weitergeleitet, aber dort wird dein Abo mit ${days} Testtagen gestartet. Heute wird das Abo nicht sofort belastet.`;
    }
    return `After verification you will continue to checkout, but that checkout starts your subscription with ${days} free days. The subscription itself is not charged today.`;
  }

  get verificationLoadingText(): string {
    if (!this.isBorrowerSubscriptionFlow) {
      return this.i18n.t('common.loading');
    }
    const days = this.borrowerTrialDays;
    if (this.languageKey.startsWith('pt')) {
      return `A preparar o seu teste de ${days} dias...`;
    }
    if (this.languageKey.startsWith('de')) {
      return `Dein ${days}-Tage-Test wird vorbereitet...`;
    }
    return `Preparing your ${days}-day trial...`;
  }

  get confirmLoadingText(): string {
    if (!this.isBorrowerSubscriptionFlow) {
      return this.i18n.t('verification.email.confirm_loading');
    }
    if (this.languageKey.startsWith('pt')) {
      return 'A confirmar e a iniciar o seu teste...';
    }
    if (this.languageKey.startsWith('de')) {
      return 'Wird bestaetigt und dein Test gestartet...';
    }
    return 'Confirming and starting your trial...';
  }

  get verificationFooterHintText(): string {
    if (!this.isBorrowerSubscriptionFlow) {
      return this.i18n.t('verification.email.footer_hint');
    }
    const days = this.borrowerTrialDays;
    if (this.languageKey.startsWith('pt')) {
      return `Depois da verificacao segue para o checkout da Stripe para iniciar a subscricao. O cartao fica autorizado agora, mas a cobranca da subscricao so acontece apos ${days} dias de teste.`;
    }
    if (this.languageKey.startsWith('de')) {
      return `Nach der Bestaetigung geht es zum Stripe-Checkout, um das Abo zu starten. Die Karte wird jetzt hinterlegt, aber das Abo wird erst nach ${days} Testtagen belastet.`;
    }
    return `After verification you continue to Stripe checkout to start the subscription. Your card is saved there, but the subscription is not charged until the ${days}-day trial ends.`;
  }

  async ngOnInit() {
    await this.settingsConfig.ensureLoaded();
    const initParams = this.route.snapshot.queryParams || {};
    this.plan = initParams['plan'] || 'plus';
    const initFlow = String(initParams['flow'] || '').toLowerCase();
    this.flow = initFlow === 'signup' ? 'signup' : 'subscription';
    this.subscriptionScope = String(initParams['scope'] || '').toLowerCase() === 'borrower' ? 'borrower' : 'platform';
    this.bookingListingId = String(initParams['listingId'] || '');
    this.bookingFrom = String(initParams['from'] || '');
    const initTrialDays = Number(initParams['trialDays']);
    if (!Number.isNaN(initTrialDays) && initTrialDays > 0) this.borrowerTrialDays = initTrialDays;
    this.verificationToken = String(initParams['token'] || '');
    this.subscriptionCodeAlreadySent = String(initParams['sent'] || '').toLowerCase() === '1' || String(initParams['sent'] || '').toLowerCase() === 'true';
    const initEmail = String(initParams['email'] || '');
    if (initEmail) this.userEmail = initEmail;
    if (this.flow !== 'signup' && !this.subscriptionFeature.enabled() && this.verificationToken) {
      this.flow = 'signup';
    }
    this.render();

    this.route.queryParams.subscribe(params => {
      this.plan = params['plan'] || this.plan;
      const flow = String(params['flow'] || '').toLowerCase();
      this.flow = flow === 'signup' ? 'signup' : 'subscription';
      this.subscriptionScope = String(params['scope'] || '').toLowerCase() === 'borrower' ? 'borrower' : 'platform';
      this.bookingListingId = String(params['listingId'] || this.bookingListingId || '');
      this.bookingFrom = String(params['from'] || this.bookingFrom || '');
      const trialDays = Number(params['trialDays']);
      if (!Number.isNaN(trialDays) && trialDays > 0) this.borrowerTrialDays = trialDays;
      this.verificationToken = String(params['token'] || this.verificationToken);
      this.subscriptionCodeAlreadySent = String(params['sent'] || '').toLowerCase() === '1' || String(params['sent'] || '').toLowerCase() === 'true';
      const email = String(params['email'] || '');
      if (email) this.userEmail = email;
      if (this.flow !== 'signup' && !this.subscriptionFeature.enabled() && this.verificationToken) {
        this.flow = 'signup';
      }
      this.render();
    });

    if (this.flow === 'signup') {
      if (!this.verificationToken && this.userEmail) {
        try {
          const started = await this.api.startEmailVerification(this.userEmail, this.i18n.language());
          this.verificationToken = String(started?.token || '');
        } catch { }
      }
      if (!this.userEmail) {
        this.router.navigate(['/connect']);
        return;
      }
      await this.bootstrap();
      return;
    }

    if (!this.subscriptionFlowEnabled) {
      this.router.navigate(['/dashboard']);
      return;
    }

    if (this.subscriptionScope === 'borrower' && this.userEmail) {
      await this.bootstrap();
      return;
    }

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
      if (this.flow === 'signup') {
        this.sent = true;
        this.startResendCooldown(30);
      } else if (this.subscriptionCodeAlreadySent) {
        this.sent = true;
        this.startResendCooldown(30);
      } else {
        await this.sendCode();
      }
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
      if (this.flow === 'signup') {
        if (!this.verificationToken) throw new Error('Missing token');
        await this.api.resendEmailVerification(this.verificationToken, this.i18n.language());
      } else {
        if (this.subscriptionScope === 'borrower') {
          await this.api.sendBorrowingSubscriptionVerificationCode(this.i18n.language());
        } else {
          await this.api.sendSubscriptionVerificationCode(this.plan, this.i18n.language());
        }
      }
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
      if (this.flow === 'signup') {
        if (!this.verificationToken) throw new Error('Missing token');
        const data = await this.api.verifyEmailVerification(this.verificationToken, this.codeValue);
        if (data?.token) this.authStorage.setToken(data.token);
        if (data?.user?.id) this.authStorage.setUserId(data.user.id);
        this.authStorage.setAuthContext('user');
        if (data?.user) this.session.user.set(data.user);
        this.router.navigate([this.subscriptionFeature.enabled() ? '/subscription' : '/dashboard']);
        return;
      }

      if (this.subscriptionScope === 'borrower') {
        await this.api.verifyBorrowingSubscriptionVerificationCode(this.codeValue);
      } else {
        await this.api.verifySubscriptionVerificationCode(this.codeValue);
      }

      if (this.plan === 'starter') {
        await this.api.subscribeStarter();
        this.router.navigate(['/dashboard']);
        return;
      }

      const { url, sessionId } = this.subscriptionScope === 'borrower'
        ? await this.api.createBorrowingSubscriptionCheckoutSession(this.borrowerBookingReturnPath)
        : await this.api.createSubscriptionCheckoutSession(this.plan, '/dashboard');
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
      this.error = this.flow === 'signup'
        ? (e?.message || this.i18n.t('verification.email.invalid_code'))
        : this.mapSubscriptionVerificationError(e);
    } finally {
      this.submitting = false;
      this.render();
    }
  }

  goBack() {
    if (this.flow === 'signup') {
      this.router.navigate(['/connect']);
      return;
    }
    if (this.subscriptionScope === 'borrower' && this.bookingListingId) {
      this.router.navigate([`/listing/${encodeURIComponent(this.bookingListingId)}/book`], {
        queryParams: this.bookingFrom ? { from: this.bookingFrom } : undefined
      });
      return;
    }
    this.router.navigate(['/subscription/confirm'], { queryParams: { plan: this.plan } });
  }

  private get borrowerBookingReturnPath(): string {
    const listingId = String(this.bookingListingId || '').trim();
    if (!listingId) {
      return '/dashboard?borrower_subscription=1';
    }
    let path = `/listing/${encodeURIComponent(listingId)}/book?borrower_subscription=1`;
    if (this.bookingFrom) {
      path += `&from=${encodeURIComponent(this.bookingFrom)}`;
    }
    return path;
  }

  private get subscriptionFlowEnabled(): boolean {
    if (this.subscriptionScope === 'borrower') {
      return this.settingsConfig.getBoolean('enable', 'borrowing.subscription', true);
    }
    return this.subscriptionFeature.enabled();
  }
}
