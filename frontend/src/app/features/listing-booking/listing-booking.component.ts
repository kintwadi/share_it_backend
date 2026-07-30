import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, ShieldCheck, CheckCircle2, AlertTriangle, Loader2, Lock as LockIcon, DollarSign, ChevronLeft, Plus, Minus, CreditCard, Wallet } from 'lucide-angular';
import { Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, ListingType, InsuranceTypeInfo, InsuranceQuoteResponse, AvailabilityStatus, User } from '../../core/models/types';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { StripeClientService } from '../../core/services/stripe-client.service';
import { getListingPrimaryRate, getListingPricingUnit, getPricingUnitLong, getPricingUnitPlural, getPricingUnitShort } from '../../core/utils/listing-pricing';
import { LayoutModeService } from '../../core/services/layout-mode.service';

type PendingBorrowerBookingState = {
  listingId: string;
  from: string;
  createdAt: number;
};

@Component({
  selector: 'app-listing-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-booking.component.html',
  styleUrl: './listing-booking.component.css'
})
export class ListingBookingComponent implements OnInit, OnDestroy {
  private readonly pendingBorrowerBookingStorageKey = 'borrower-subscription-pending-booking';
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  i18n = inject(I18nService);
  private settingsConfig = inject(SettingsConfigService);
  private stripeClient = inject(StripeClientService);
  private cdr = inject(ChangeDetectorRef);
  layoutMode = inject(LayoutModeService);

  readonly ArrowLeft = ArrowLeft;
  readonly ShieldCheck = ShieldCheck;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;
  readonly Loader2 = Loader2;
  readonly LockIcon = LockIcon;
  readonly DollarSign = DollarSign;
  readonly ChevronLeft = ChevronLeft;
  readonly Plus = Plus;
  readonly Minus = Minus;
  readonly CreditCard = CreditCard;
  readonly Wallet = Wallet;

  @ViewChild('cardPayMount') cardPayMount?: ElementRef<HTMLDivElement>;

  listing: Listing | null = null;
  loading = true;
  error: string | null = null;
  currentUserEmail = '';
  currentUser: User | null = null;

  private backTo = '/';

  bookingStep: 'PATH_SELECTION' | 'DURATION' | 'INSURANCE' | 'PAYMENT' | 'CARD_FORM' = 'PATH_SELECTION';
  selectedPath: 'DEPOSIT' | 'VERIFIED' | 'FEE' = 'VERIFIED';
  bookingDuration = 1;
  paymentMethod: 'CARD' | 'PAYPAL' | 'CASH' = 'CARD';

  borrowing = false;
  actionError: string | null = null;
  actionNotice: string | null = null;
  stripe: Stripe | null = null;
  elements: StripeElements | null = null;
  card: StripeCardElement | null = null;
  stripeReady = false;
  paymentMethods: any[] = [];
  selectedSavedPaymentMethodId: string | null = null;
  cardProcessing = false;
  cardError: string | null = null;

  insuranceTypes: InsuranceTypeInfo[] = [];
  insuranceLoading = false;
  insuranceError: string | null = null;
  selectedInsuranceType: string | null = null;
  insuranceZipCode = '';
  insuranceQuote: InsuranceQuoteResponse | null = null;

  plusTrialDays = 14;
  plusMonthlyAmountCents = 499;
  subscriptionCurrency = 'EUR';
  borrowerCanBorrowDirectly = false;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnDestroy() {
    this.cleanupCardElement();
  }

  async ngOnInit() {
    this.settingsConfig.ensureLoaded();

    const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    const sessionId = String(this.route.snapshot.queryParamMap.get('session_id') || '').trim();
    const borrowerSubscription = String(this.route.snapshot.queryParamMap.get('borrower_subscription') || '').trim().toLowerCase();
    const shouldResumeBorrowing = !!sessionId && (borrowerSubscription === '1' || borrowerSubscription === 'true');
    const pendingBorrowerBooking = this.readPendingBorrowerBookingState();
    this.backTo = from.startsWith('/') ? from : `/listing/${encodeURIComponent(id)}`;

    if (!id) {
      this.router.navigateByUrl(from.startsWith('/') ? from : '/');
      return;
    }

    this.loading = true;
    this.error = null;
    this.render();
    try {
      const listing = await this.api.getListingById(id);
      if (!listing) {
        this.router.navigateByUrl(this.backTo);
        return;
      }
      this.listing = listing;
      const me = await this.api.getCurrentUser();
      if (!me) {
        this.router.navigate(['/connect']);
        return;
      }
      this.currentUser = me;
      this.currentUserEmail = String(me.email || '');

      if (shouldResumeBorrowing) {
        try {
          await this.api.syncBorrowingSubscriptionFromSession(sessionId);
        } catch { }
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
        this.borrowerCanBorrowDirectly = this.resolveBorrowDirectly(subscription);
        if (shouldResumeBorrowing && this.borrowerCanBorrowDirectly && this.matchesPendingBorrowerBooking(pendingBorrowerBooking, id)) {
          this.actionNotice = this.borrowerSubscriptionResumeNotice;
        }
      } catch { }

      if (shouldResumeBorrowing) {
        try {
          await this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { session_id: null, borrower_subscription: null },
            queryParamsHandling: 'merge',
            replaceUrl: true
          });
        } catch { }
      }

      await this.initStripe();
      this.initDefaultFlow();
      if (this.requiresInsurance) {
        this.loadInsuranceTypes();
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('listing.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  back() {
    this.router.navigateByUrl(this.backTo || '/');
  }

  private initDefaultFlow() {
    const listing = this.listing;
    if (!listing) return;

    if (listing.status !== AvailabilityStatus.AVAILABLE && listing.status !== AvailabilityStatus.PARTNER_ACTIVE && listing.status !== AvailabilityStatus.PARTNER_BORROW_REQUESTED) {
      return;
    }

    if (listing.type === ListingType.GIVE) {
      this.bookingStep = 'PAYMENT';
      this.render();
      return;
    }

    if (this.isPartnerListing) {
      this.bookingDuration = 1;
      this.selectedPath = 'VERIFIED';
      this.paymentMethod = 'CASH';
      this.selectedSavedPaymentMethodId = null;
      this.cardError = null;
      this.insuranceError = null;
      this.selectedInsuranceType = null;
      this.insuranceZipCode = '';
      this.insuranceQuote = null;
      this.bookingStep = 'DURATION';
      this.render();
      return;
    }

    this.selectedPath = this.borrowTierEnabled.deposit ? 'DEPOSIT' : 'FEE';
    this.bookingDuration = 1;
    this.paymentMethod = this.resolvePreferredPaymentMethod();
    this.selectedSavedPaymentMethodId = null;
    this.cardError = null;
    this.insuranceError = null;
    this.selectedInsuranceType = null;
    this.insuranceZipCode = '';
    this.insuranceQuote = null;
    if (this.shouldSkipBorrowingOptions) {
      this.selectedPath = 'VERIFIED';
      this.bookingStep = 'DURATION';
      this.render();
      return;
    }
    this.bookingStep = 'PATH_SELECTION';
    this.render();
  }

  get isPartnerListing(): boolean {
    return !!this.listing?.partnerId;
  }

  get isTimeBased() {
    if (!this.listing) return true;
    return this.listing.type !== ListingType.GIVE && this.listing.type !== ListingType.SELL;
  }

  get isFree() {
    return this.finalTotalWithInsurance <= 0;
  }

  get borrowTierEnabled() {
    if (this.isPartnerListing) return { deposit: false, verified: false, fee: false };
    return {
      deposit: this.settingsConfig.isSectionEnabled('borrowing', 'deposit'),
      verified: this.borrowingSubscriptionEnabled && this.settingsConfig.isSectionEnabled('borrowing', 'verified'),
      fee: this.settingsConfig.isSectionEnabled('borrowing', 'fee')
    };
  }

  get borrowingSubscriptionEnabled(): boolean {
    return this.settingsConfig.getBoolean('enable', 'borrowing.subscription', true);
  }

  get paymentOptions() {
    if (this.isPartnerListing) return { card: false, paypal: false, cash: false };
    const cashEnabledBySettings = this.settingsConfig.isSectionEnabled('borrowing', 'payments.cash');
    const cashEnabledByBackendProperty = this.settingsConfig.getBoolean('pay', 'with.cash', false);
    return {
      card: this.settingsConfig.isSectionEnabled('borrowing', 'payments.card'),
      paypal: this.settingsConfig.isSectionEnabled('borrowing', 'payments.paypal'),
      // Verified borrowing is the subscription path, so cash should not be offered there.
      cash: cashEnabledBySettings && cashEnabledByBackendProperty && this.selectedPath !== 'VERIFIED'
    };
  }

  private resolvePreferredPaymentMethod(): 'CARD' | 'PAYPAL' | 'CASH' {
    if (this.paymentOptions.card) return 'CARD';
    if (this.paymentOptions.paypal) return 'PAYPAL';
    if (this.paymentOptions.cash) return 'CASH';
    return 'CARD';
  }

  get shouldSkipBorrowingOptions(): boolean {
    if (this.isPartnerListing) return false;
    return this.listing?.type === ListingType.LEND && this.borrowingSubscriptionEnabled && this.borrowerCanBorrowDirectly;
  }

  private ensureValidPaymentMethod() {
    const options = this.paymentOptions;
    const currentIsAllowed =
      (this.paymentMethod === 'CARD' && options.card) ||
      (this.paymentMethod === 'PAYPAL' && options.paypal) ||
      (this.paymentMethod === 'CASH' && options.cash);

    if (!currentIsAllowed) {
      this.paymentMethod = this.resolvePreferredPaymentMethod();
    }
  }

  private resolveBorrowDirectly(subscription: any): boolean {
    if (!subscription) return false;
    if (typeof subscription?.borrowDirectly === 'boolean') return subscription.borrowDirectly;
    const active = typeof subscription?.active === 'boolean' ? subscription.active : false;
    const planType = String(subscription?.planType || '').trim().toLowerCase();
    if (active) {
      return !!planType && planType !== 'starter';
    }
    const status = String(subscription?.status || '').trim().toLowerCase();
    return !!planType && planType !== 'starter' && (status === 'active' || status === 'trialing' || status === 'trial_active');
  }

  get baseTotal() {
    if (!this.listing) return 0;
    const rate = getListingPrimaryRate(this.listing);
    const duration = this.isTimeBased ? this.bookingDuration : 1;
    return rate * duration;
  }

  get bookingRateSuffix() {
    return getPricingUnitShort(getListingPricingUnit(this.listing));
  }

  get bookingRateLabel() {
    return `${this.i18n.formatPrice(getListingPrimaryRate(this.listing))}${this.bookingRateSuffix}`;
  }

  get bookingDurationUnitLabel() {
    return this.bookingDuration === 1
      ? getPricingUnitLong(getListingPricingUnit(this.listing))
      : getPricingUnitPlural(getListingPricingUnit(this.listing));
  }

  get bookingDurationMax() {
    const unit = getListingPricingUnit(this.listing);
    if (unit === 'MONTHLY') return 12;
    if (unit === 'DAILY') return 30;
    return 24;
  }

  get serviceFee() {
    if (this.isPartnerListing) return 0;
    if (this.borrowerCanBorrowDirectly) return 0;
    if (this.subscriptionDisabledLendFee > 0) return this.subscriptionDisabledLendFee;
    if (this.selectedPath !== 'FEE') return 0;
    return Math.round(this.baseTotal * this.serviceFeeRate * 100) / 100;
  }

  get waivedServiceFee() {
    if (this.isPartnerListing) return 0;
    if (!this.borrowerCanBorrowDirectly) return 0;
    if (!this.listing || this.listing.type !== ListingType.LEND) return 0;
    if (this.subscriptionDisabledLendFee > 0) return this.subscriptionDisabledLendFee;
    return Math.round(this.baseTotal * this.serviceFeeRate * 100) / 100;
  }

  get serviceFeeRate() {
    const rate = this.settingsConfig.getNumber('service', 'fee_percent', 0.08);
    if (!Number.isFinite(rate) || rate < 0) return 0.08;
    return rate;
  }

  get serviceFeePercentLabel() {
    const percent = this.serviceFeeRate * 100;
    const rounded = Math.round(percent * 100) / 100;
    return `${Number.isInteger(rounded) ? rounded.toFixed(0) : rounded}%`;
  }

  get subscriptionDisabledLendFee() {
    if (this.isPartnerListing) return 0;
    if (!this.listing) return 0;
    const subscriptionEnabled = this.settingsConfig.isSectionEnabled('enable', 'subscription');
    if (subscriptionEnabled) return 0;
    if (this.listing.type !== ListingType.LEND) return 0;
    const fee = this.settingsConfig.getNumber('service', 'fee', 2.99);
    if (!Number.isFinite(fee) || fee <= 0) return 0;
    return Math.round(fee * 100) / 100;
  }

  get depositAmount() {
    if (this.isPartnerListing) return 0;
    if (this.selectedPath !== 'DEPOSIT') return 0;
    return 50;
  }

  get finalTotal() {
    return this.baseTotal + this.serviceFee + this.depositAmount;
  }

  get insuranceCost() {
    return this.insuranceQuote?.insuranceCost || 0;
  }

  get finalTotalWithInsurance() {
    return this.finalTotal + this.insuranceCost;
  }

  get requiresInsurance(): boolean {
    if (this.isPartnerListing) return false;
    return !!(this.listing as any)?.insuranceRequired;
  }

  get plusMonthlyAmount() {
    return (this.plusMonthlyAmountCents || 0) / 100;
  }

  get plusMonthlyLabel() {
    const amount = this.plusMonthlyAmount;
    const curr = String(this.subscriptionCurrency || '').toUpperCase();
    const formatted = amount.toFixed(2);
    if (curr === 'EUR') return `€${formatted}`;
    if (curr === 'USD') return `$${formatted}`;
    if (curr === 'GBP') return `£${formatted}`;
    return `${curr} ${formatted}`;
  }

  get borrowerSubscriptionResumeNotice() {
    const days = this.plusTrialDays;
    const lang = String(this.i18n.language() || 'en').toLowerCase();
    if (lang.startsWith('pt')) {
      return `A subscricao verificada esta ativa. Hoje paga apenas este emprestimo. Se acabou de aderir, o cartao da subscricao so sera cobrado apos os ${days} dias de teste.`;
    }
    if (lang.startsWith('de')) {
      return `Dein verifiziertes Abo ist aktiv. Heute zahlst du nur fuer diese Ausleihe. Wenn du das Abo gerade gestartet hast, wird deine Karte fuer das Abo erst nach ${days} Testtagen belastet.`;
    }
    return `Your verified subscription is active. Today you only pay for this borrowing. If you just subscribed, your card will not be charged for the subscription until the ${days}-day trial ends.`;
  }

  get waivedServiceFeeNotice() {
    const days = this.plusTrialDays;
    const lang = String(this.i18n.language() || 'en').toLowerCase();
    if (lang.startsWith('pt')) {
      return `A taxa de servico foi removida pela sua subscricao. Hoje paga apenas o item e extras opcionais. Novas subscricoes comecam com ${days} dias de teste antes de qualquer cobranca.`;
    }
    if (lang.startsWith('de')) {
      return `Die Servicegebuehr entfaellt durch dein Abo. Heute zahlst du nur fuer den Artikel und optionale Extras. Neue Abos starten mit ${days} Testtagen vor der ersten Belastung.`;
    }
    return `The service fee is removed by your subscription. Today you only pay for the item and optional extras. New subscriptions start with ${days} free days before any subscription charge.`;
  }

  get borrowerSubscriptionCheckoutNotice() {
    const days = this.plusTrialDays;
    const lang = String(this.i18n.language() || 'en').toLowerCase();
    if (lang.startsWith('pt')) {
      return `Este pagamento e apenas para o emprestimo. A subscricao e separada: novas adesoes recebem ${days} dias de teste e o cartao nao e cobrado pela subscricao ate ao fim desse periodo.`;
    }
    if (lang.startsWith('de')) {
      return `Diese Zahlung gilt nur fuer die Ausleihe. Das Abo ist getrennt: Neue Abos erhalten ${days} Testtage und die Karte wird fuer das Abo erst nach diesem Zeitraum belastet.`;
    }
    return `This payment is only for the borrowing. The subscription is separate: new subscriptions get ${days} free days, and the card is not charged for the subscription until that trial ends.`;
  }

  decBookingDuration() {
    this.bookingDuration = Math.max(1, this.bookingDuration - 1);
    this.render();
  }

  incBookingDuration() {
    this.bookingDuration = Math.min(this.bookingDurationMax, this.bookingDuration + 1);
    this.render();
  }

  async continueFromPath() {
    if (this.selectedPath === 'VERIFIED' && !this.borrowerCanBorrowDirectly) {
      this.selectedPath = this.borrowTierEnabled.deposit ? 'DEPOSIT' : 'FEE';
    }
    this.ensureValidPaymentMethod();
    this.bookingStep = 'DURATION';
    this.render();
  }

  backToPath() {
    if (this.shouldSkipBorrowingOptions) {
      this.bookingStep = 'DURATION';
      this.render();
      return;
    }
    this.bookingStep = 'PATH_SELECTION';
    this.render();
  }

  proceedFromDuration() {
    if (this.isPartnerListing) {
      this.confirmRequest();
      return;
    }
    this.bookingStep = this.requiresInsurance ? 'INSURANCE' : 'PAYMENT';
    this.render();
  }

  backToDuration() {
    this.bookingStep = 'DURATION';
    this.render();
  }

  async proceedFromInsurance() {
    if (this.requiresInsurance && !this.insuranceQuote) {
      this.insuranceError = 'Please select an insurance option to continue.';
      this.render();
      return;
    }
    this.bookingStep = 'PAYMENT';
    this.render();
  }

  async proceedFromPayment() {
    this.ensureValidPaymentMethod();
    if (this.paymentMethod === 'CARD' && this.finalTotalWithInsurance > 0) {
      this.bookingStep = 'CARD_FORM';
      this.render();
      setTimeout(() => this.mountPaymentCard(), 0);
      return;
    }
    await this.confirmRequest();
  }

  private async initStripe() {
    this.stripe = await this.stripeClient.getStripe();
    this.stripeReady = !!this.stripe;
    if (this.stripeReady) {
      try {
        this.paymentMethods = await this.api.getPaymentMethods();
      } catch {
        this.paymentMethods = [];
      }
    } else {
      this.paymentMethods = [];
    }
    this.render();
  }

  mountPaymentCard() {
    if (this.bookingStep !== 'CARD_FORM') return;
    if (!this.stripe) return;
    if (!this.cardPayMount?.nativeElement) return;
    if (this.card) return;

    this.elements = this.stripe.elements();
    this.card = this.elements.create('card');
    this.card.mount(this.cardPayMount.nativeElement);
  }

  private cleanupCardElement() {
    if (this.card) {
      try {
        this.card.destroy();
      } catch { }
    }
    this.card = null;
    this.elements = null;
  }

  async submitCardPayment() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.stripe) return;
    if (this.cardProcessing) return;
    if (this.finalTotalWithInsurance <= 0) {
      this.cardError = this.i18n.t('listing.error.payment_not_required');
      this.render();
      return;
    }
    if (!this.selectedSavedPaymentMethodId && !this.card) {
      this.cardError = this.i18n.t('listing.error.card_details');
      this.render();
      return;
    }

    this.cardProcessing = true;
    this.cardError = null;
    this.render();
    try {
      const intent = await this.api.createPaymentIntent({
        amount: this.finalTotalWithInsurance,
        currency: 'usd',
        listingId: listing.id,
        durationHours: this.isTimeBased ? this.bookingDuration : 0,
        durationValue: this.isTimeBased ? this.bookingDuration : 0,
        durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined,
        borrowerPath: this.selectedPath,
        paymentMethodId: this.selectedSavedPaymentMethodId || undefined
      });
      const clientSecret = intent?.clientSecret;
      if (!clientSecret) {
        this.cardError = this.i18n.t('listing.error.payment_init_failed');
        return;
      }

      const confirmRes = this.selectedSavedPaymentMethodId
        ? await this.stripe.confirmCardPayment(clientSecret, { payment_method: this.selectedSavedPaymentMethodId })
        : await this.stripe.confirmCardPayment(clientSecret, { payment_method: { card: this.card! } as any });

      if (confirmRes.error) {
        this.cardError = confirmRes.error.message || this.i18n.t('listing.error.payment_failed');
        return;
      }
      const pi = confirmRes.paymentIntent;
      if (!pi || pi.status !== 'succeeded') {
        this.cardError = this.i18n.t('listing.error.payment_not_completed');
        return;
      }

      await this.api.borrowListing(listing.id, {
        paymentMethod: 'STRIPE',
        paymentToken: pi.id,
        durationHours: this.isTimeBased ? this.bookingDuration : 0,
        durationValue: this.isTimeBased ? this.bookingDuration : 0,
        durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined,
        borrowerPath: this.selectedPath
      });
      await this.purchaseInsuranceIfSelected();
      await this.sendOwnerBorrowMessage(listing);
      this.navigateBackWithNotice(listing.autoApprove ? this.i18n.t('listing.success.booked') : this.i18n.t('listing.success.request_sent'));
    } catch (e: any) {
      this.cardError = e?.message || this.i18n.t('listing.error.payment_failed');
    } finally {
      this.cardProcessing = false;
      this.render();
    }
  }

  private navigateBackWithNotice(message: string) {
    this.clearPendingBorrowerBookingState();
    this.router.navigateByUrl(this.backTo || '/', { state: { noticeSuccess: message } as any });
  }

  private persistPendingBorrowerBookingState() {
    const listingId = String(this.listing?.id || '').trim();
    if (!listingId) return;
    const payload: PendingBorrowerBookingState = {
      listingId,
      from: this.backTo || '/',
      createdAt: Date.now()
    };
    try {
      localStorage.setItem(this.pendingBorrowerBookingStorageKey, JSON.stringify(payload));
    } catch { }
  }

  private clearPendingBorrowerBookingState() {
    try {
      localStorage.removeItem(this.pendingBorrowerBookingStorageKey);
    } catch { }
  }

  private readPendingBorrowerBookingState(): PendingBorrowerBookingState | null {
    try {
      const raw = String(localStorage.getItem(this.pendingBorrowerBookingStorageKey) || '').trim();
      if (!raw) return null;
      const parsed = JSON.parse(raw) as PendingBorrowerBookingState | null;
      if (!parsed?.listingId) return null;
      return parsed;
    } catch {
      return null;
    }
  }

  private matchesPendingBorrowerBooking(state: PendingBorrowerBookingState | null, listingId: string): boolean {
    if (!state) return false;
    if (String(state.listingId || '').trim() !== String(listingId || '').trim()) {
      return false;
    }
    const createdAt = Number(state.createdAt || 0);
    if (!Number.isFinite(createdAt) || createdAt <= 0) {
      return false;
    }
    const ageMs = Date.now() - createdAt;
    return ageMs >= 0 && ageMs <= 1000 * 60 * 60 * 6;
  }

  private async confirmRequest() {
    const listing = this.listing;
    if (!listing) return;
    if (this.borrowing) return;

    this.borrowing = true;
    this.actionError = null;
    this.render();
    try {
      if (listing.type === ListingType.GIVE) {
        await this.api.borrowListing(listing.id, { paymentMethod: 'GIFT', durationHours: 0, durationValue: 0 });
        await this.sendOwnerBorrowMessage(listing);
        this.navigateBackWithNotice(listing.autoApprove ? this.i18n.t('listing.success.gift_claimed') : this.i18n.t('listing.success.gift_request'));
        return;
      }

      if (this.isPartnerListing) {
        await this.api.borrowListing(listing.id, {
          paymentMethod: 'PARTNER',
          durationHours: this.isTimeBased ? this.bookingDuration : 0,
          durationValue: this.isTimeBased ? this.bookingDuration : 0,
          durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined,
          borrowerPath: 'PARTNER'
        });
        this.navigateBackWithNotice(this.i18n.t('listing.success.request_sent'));
        return;
      }

      if (this.finalTotalWithInsurance <= 0) {
        await this.api.borrowListing(listing.id, { paymentMethod: 'FREE', durationHours: this.isTimeBased ? this.bookingDuration : 0, durationValue: this.isTimeBased ? this.bookingDuration : 0, durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'PAYPAL') {
        await this.api.borrowListing(listing.id, { paymentMethod: 'PAYPAL', durationHours: this.isTimeBased ? this.bookingDuration : 0, durationValue: this.isTimeBased ? this.bookingDuration : 0, durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'CASH') {
        if (this.selectedPath === 'VERIFIED') {
          this.actionError = this.i18n.t('listing.error.process_failed');
          return;
        }
        await this.api.borrowListing(listing.id, { paymentMethod: 'CASH', durationHours: this.isTimeBased ? this.bookingDuration : 0, durationValue: this.isTimeBased ? this.bookingDuration : 0, durationUnit: this.isTimeBased ? getListingPricingUnit(this.listing) : undefined, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'CARD') {
        await this.submitCardPayment();
        return;
      }
      await this.purchaseInsuranceIfSelected();
      await this.sendOwnerBorrowMessage(listing);
      this.navigateBackWithNotice(listing.autoApprove ? this.i18n.t('listing.success.booked') : this.i18n.t('listing.success.request_sent'));
    } catch (e: any) {
      this.actionError = e?.message || this.i18n.t('listing.error.process_failed');
    } finally {
      this.borrowing = false;
      this.render();
    }
  }

  async loadInsuranceTypes() {
    if (this.insuranceLoading) return;
    this.insuranceLoading = true;
    this.insuranceError = null;
    this.render();
    try {
      this.insuranceTypes = await this.api.getInsuranceTypes();
    } catch (e: any) {
      this.insuranceError = e?.message || 'Failed to load insurance types';
      this.insuranceTypes = [];
    } finally {
      this.insuranceLoading = false;
      this.render();
    }
  }

  async selectInsuranceType(type: string) {
    const listing = this.listing;
    if (!listing) return;
    this.selectedInsuranceType = type;
    this.insuranceQuote = null;
    this.insuranceError = null;
    this.insuranceLoading = true;
    this.render();
    try {
      const quote = await this.api.quoteInsurance({
        productId: listing.id,
        productBasePrice: this.baseTotal,
        insuranceType: type,
        customerZipCode: this.insuranceZipCode ? this.insuranceZipCode : null
      });
      this.insuranceQuote = quote;
    } catch (e: any) {
      this.insuranceError = e?.message || 'Failed to calculate insurance';
      this.insuranceQuote = null;
    } finally {
      this.insuranceLoading = false;
      this.render();
    }
  }

  private async purchaseInsuranceIfSelected() {
    const quoteId = this.insuranceQuote?.quoteId;
    if (!quoteId) return;
    try {
      await this.api.purchaseInsurance(quoteId);
    } catch { }
  }

  private async sendOwnerBorrowMessage(listing: Listing) {
    if (!listing || this.isPartnerListing) return;
    const ownerId = String(listing.ownerId || '').trim();
    if (!ownerId) return;
    const currentUserId = String(this.currentUser?.id || '').trim();
    if (currentUserId && ownerId === currentUserId) return;
    const content = this.buildOwnerBorrowMessage(listing);
    if (!content) return;
    try {
      await this.api.sendMessage(ownerId, content);
    } catch { }
  }

  private buildOwnerBorrowMessage(listing: Listing): string {
    const borrowerName = String(this.currentUser?.name || this.currentUserEmail || 'A user').trim();
    const listingTitle = String(listing.title || 'your item').trim();
    if (listing.type === ListingType.GIVE) {
      return listing.autoApprove
        ? `${borrowerName} claimed "${listingTitle}".`
        : `${borrowerName} requested to claim "${listingTitle}".`;
    }
    if (listing.type === ListingType.SELL) {
      return listing.autoApprove
        ? `${borrowerName} purchased "${listingTitle}".`
        : `${borrowerName} requested to purchase "${listingTitle}".`;
    }
    return listing.autoApprove
      ? `${borrowerName} borrowed "${listingTitle}".`
      : `${borrowerName} requested to borrow "${listingTitle}".`;
  }
}
