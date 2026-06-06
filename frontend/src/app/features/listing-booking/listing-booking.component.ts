import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, ShieldCheck, CheckCircle2, AlertTriangle, Loader2, Zap, Lock as LockIcon, DollarSign, ChevronLeft, Plus, Minus, CreditCard, Wallet } from 'lucide-angular';
import { Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, ListingType, InsuranceTypeInfo, InsuranceQuoteResponse, AvailabilityStatus } from '../../core/models/types';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { StripeClientService } from '../../core/services/stripe-client.service';

@Component({
  selector: 'app-listing-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-booking.component.html',
  styleUrl: './listing-booking.component.css'
})
export class ListingBookingComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  i18n = inject(I18nService);
  private settingsConfig = inject(SettingsConfigService);
  private stripeClient = inject(StripeClientService);
  private cdr = inject(ChangeDetectorRef);

  readonly ArrowLeft = ArrowLeft;
  readonly ShieldCheck = ShieldCheck;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;
  readonly Loader2 = Loader2;
  readonly Zap = Zap;
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

  private backTo = '/';

  bookingStep: 'PATH_SELECTION' | 'DURATION' | 'INSURANCE' | 'PAYMENT' | 'CARD_FORM' = 'PATH_SELECTION';
  selectedPath: 'DEPOSIT' | 'VERIFIED' | 'FEE' = 'VERIFIED';
  bookingDuration = 1;
  paymentMethod: 'CARD' | 'PAYPAL' | 'CASH' = 'CARD';

  borrowing = false;
  actionError: string | null = null;

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

      try {
        const cfg = await this.api.getPublicConfig();
        const sub = cfg?.subscription || {};
        const td = Number(sub?.plusTrialDays);
        const cents = Number(sub?.plusMonthlyAmountCents);
        const curr = String(sub?.currency || '');
        if (!Number.isNaN(td) && td > 0) this.plusTrialDays = td;
        if (!Number.isNaN(cents) && cents >= 0) this.plusMonthlyAmountCents = cents;
        if (curr) this.subscriptionCurrency = curr;
      } catch { }

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

    this.selectedPath = this.borrowTierEnabled.verified ? 'VERIFIED' : (this.borrowTierEnabled.deposit ? 'DEPOSIT' : 'FEE');
    this.bookingDuration = 1;
    this.paymentMethod = this.paymentOptions.card ? 'CARD' : (this.paymentOptions.paypal ? 'PAYPAL' : 'CASH');
    this.selectedSavedPaymentMethodId = null;
    this.cardError = null;
    this.insuranceError = null;
    this.selectedInsuranceType = null;
    this.insuranceZipCode = '';
    this.insuranceQuote = null;
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
      verified: this.settingsConfig.isSectionEnabled('borrowing', 'verified'),
      fee: this.settingsConfig.isSectionEnabled('borrowing', 'fee')
    };
  }

  get paymentOptions() {
    if (this.isPartnerListing) return { card: false, paypal: false, cash: false };
    return {
      card: this.settingsConfig.isSectionEnabled('borrowing', 'payments.card'),
      paypal: this.settingsConfig.isSectionEnabled('borrowing', 'payments.paypal'),
      cash: this.settingsConfig.isSectionEnabled('borrowing', 'payments.cash')
    };
  }

  get baseTotal() {
    if (!this.listing) return 0;
    const rate = this.listing.hourlyRate || 0;
    const duration = this.isTimeBased ? this.bookingDuration : 1;
    return rate * duration;
  }

  get serviceFee() {
    if (this.isPartnerListing) return 0;
    if (this.subscriptionDisabledLendFee > 0) return this.subscriptionDisabledLendFee;
    if (this.selectedPath !== 'FEE') return 0;
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

  get verifiedTrialCaption() {
    return `Free for ${this.plusTrialDays} days, then ${this.plusMonthlyLabel} / month`;
  }

  decBookingDuration() {
    this.bookingDuration = Math.max(1, this.bookingDuration - 1);
    this.render();
  }

  incBookingDuration() {
    this.bookingDuration = Math.min(24, this.bookingDuration + 1);
    this.render();
  }

  continueFromPath() {
    this.bookingStep = 'DURATION';
    this.render();
  }

  backToPath() {
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
        borrowerPath: this.selectedPath
      });
      await this.purchaseInsuranceIfSelected();
      this.navigateBackWithNotice(listing.autoApprove ? this.i18n.t('listing.success.booked') : this.i18n.t('listing.success.request_sent'));
    } catch (e: any) {
      this.cardError = e?.message || this.i18n.t('listing.error.payment_failed');
    } finally {
      this.cardProcessing = false;
      this.render();
    }
  }

  private navigateBackWithNotice(message: string) {
    this.router.navigateByUrl(this.backTo || '/', { state: { noticeSuccess: message } as any });
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
        await this.api.borrowListing(listing.id, { paymentMethod: 'GIFT', durationHours: 0 });
        this.navigateBackWithNotice(listing.autoApprove ? this.i18n.t('listing.success.gift_claimed') : this.i18n.t('listing.success.gift_request'));
        return;
      }

      if (this.isPartnerListing) {
        await this.api.borrowListing(listing.id, {
          paymentMethod: 'PARTNER',
          durationHours: this.isTimeBased ? this.bookingDuration : 0,
          borrowerPath: 'PARTNER'
        });
        this.navigateBackWithNotice(this.i18n.t('listing.success.request_sent'));
        return;
      }

      if (this.finalTotalWithInsurance <= 0) {
        await this.api.borrowListing(listing.id, { paymentMethod: 'FREE', durationHours: this.isTimeBased ? this.bookingDuration : 0, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'PAYPAL') {
        await this.api.borrowListing(listing.id, { paymentMethod: 'PAYPAL', durationHours: this.isTimeBased ? this.bookingDuration : 0, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'CASH') {
        await this.api.borrowListing(listing.id, { paymentMethod: 'CASH', durationHours: this.isTimeBased ? this.bookingDuration : 0, borrowerPath: this.selectedPath });
      } else if (this.paymentMethod === 'CARD') {
        await this.submitCardPayment();
        return;
      }
      await this.purchaseInsuranceIfSelected();
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
}
