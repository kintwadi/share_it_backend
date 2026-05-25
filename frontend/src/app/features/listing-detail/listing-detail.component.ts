import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, MapPin, ShieldCheck, ArrowLeft, Calendar, CheckCircle2, AlertCircle, Loader2, Share2, BadgeCheck, Flag, DollarSign, Gift, ChevronLeft, ChevronRight, Star, X, Minus, Plus, Clock, CreditCard, Wallet, AlertTriangle, BellRing, Check, X as XIcon, Zap, ThumbsUp, Trash2, Lock as LockIcon } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User, Listing, AvailabilityStatus, ListingType, InsuranceTypeInfo, InsuranceQuoteResponse } from '../../core/models/types';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { StripeClientService } from '../../core/services/stripe-client.service';
import { Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { ConfirmationModalComponent } from '../../shared/components/confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ConfirmationModalComponent],
  templateUrl: './listing-detail.component.html',
  styleUrl: './listing-detail.component.css'
})
export class ListingDetailComponent implements OnInit, OnDestroy {
  route = inject(ActivatedRoute);
  router = inject(Router);
  api = inject(ApiService);
  i18n = inject(I18nService);
  settingsConfig = inject(SettingsConfigService);
  stripeClient = inject(StripeClientService);
  cdr = inject(ChangeDetectorRef);

  readonly MapPin = MapPin;
  readonly ShieldCheck = ShieldCheck;
  readonly ArrowLeft = ArrowLeft;
  readonly Calendar = Calendar;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertCircle = AlertCircle;
  readonly Loader2 = Loader2;
  readonly Share2 = Share2;
  readonly BadgeCheck = BadgeCheck;
  readonly Flag = Flag;
  readonly DollarSign = DollarSign;
  readonly Gift = Gift;
  readonly Clock = Clock;
  readonly Zap = Zap;
  readonly X = X;
  readonly Wallet = Wallet;
  readonly CreditCard = CreditCard;
  readonly AlertTriangle = AlertTriangle;
  readonly BellRing = BellRing;
  readonly Check = Check;
  readonly XIcon = XIcon;
  readonly Trash2 = Trash2;
  readonly ThumbsUp = ThumbsUp;
  readonly Star = Star;
  readonly LockIcon = LockIcon;

  readonly ChevronLeft = ChevronLeft;
  readonly ChevronRight = ChevronRight;
  readonly Minus = Minus;
  readonly Plus = Plus;

  listing: Listing | null = null;
  currentUser: User | null = null;
  today = new Date();
  loading = true;
  borrowing = false;
  activeImage = '';
  error: string | null = null;

  showReportModal = false;
  showReportSuccess = false;

  get canOpenOwnerProfile(): boolean {
    return true;
  }

  get canShowOwnerReviews(): boolean {
    return true;
  }
  reportReason = '';
  reportDetails = '';
  reporting = false;
  reportError: string | null = null;

  showProfileModal = false;
  reviews: any[] = [];
  loadingReviews = false;
  vouching = false;
  hasVouched = false;

  get isPartnerListing(): boolean {
    return !!this.listing?.partnerId;
  }

  get displayOwnerName(): string {
    if (this.isPartnerListing) return this.listing?.partnerName || 'Partner';
    return this.listing?.owner?.name || '';
  }

  get displayAvatarSeed(): string {
    return String(this.isPartnerListing ? (this.listing?.partnerId || 'partner') : (this.listing?.owner?.id || 'user'));
  }

  actionLoading: 'APPROVE' | 'DENY' | null = null;
  showDeleteModal = false;
  deleting = false;

  showBookingModal = false;
  bookingStep: 'PATH_SELECTION' | 'DURATION' | 'INSURANCE' | 'PAYMENT' | 'CARD_FORM' = 'PATH_SELECTION';
  selectedPath: 'DEPOSIT' | 'VERIFIED' | 'FEE' = 'VERIFIED';
  bookingDuration = 1;
  paymentMethod: 'CARD' | 'PAYPAL' | 'CASH' = 'CARD';
  wasAutoApproved = false;
  showSuccess = false;
  successMessage = '';
  showActionError = false;
  actionErrorMessage = '';
  shareNotice: string | null = null;
  private successTimer: any = null;
  private actionErrorTimer: any = null;
  private shareTimer: any = null;
  private statusPollTimer: any = null;

  stripe: Stripe | null = null;
  elements: StripeElements | null = null;
  card: StripeCardElement | null = null;
  stripeReady = false;
  stripeError: string | null = null;
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

  @ViewChild('cardPayMount') cardPayMount?: ElementRef<HTMLDivElement>;

  AvailabilityStatus = AvailabilityStatus;
  ListingType = ListingType;

  get isAvailable(): boolean {
    const l = this.listing;
    if (!l) return false;
    if (l.status === AvailabilityStatus.AVAILABLE) return true;
    if (l.partnerId && l.status === AvailabilityStatus.PARTNER_ACTIVE && !l.borrowerId) return true;
    return false;
  }

  get isPending(): boolean {
    return this.listing?.status === AvailabilityStatus.PENDING || this.listing?.status === AvailabilityStatus.PARTNER_BORROW_REQUESTED;
  }

  get isApproved(): boolean {
    if (this.isPartnerListing) return false;
    return this.listing?.status === AvailabilityStatus.APPROVED;
  }

  get isPartnerBorrowRequested(): boolean {
    return this.listing?.status === AvailabilityStatus.PARTNER_BORROW_REQUESTED;
  }

  get isBorrowed(): boolean {
    return this.listing?.status === AvailabilityStatus.BORROWED;
  }

  get isGifted(): boolean {
    return this.listing?.status === AvailabilityStatus.GIFTED;
  }

  get isSold(): boolean {
    return this.listing?.status === AvailabilityStatus.SOLD;
  }

  get isBlocked(): boolean {
    return this.listing?.status === AvailabilityStatus.BLOCKED;
  }

  get isAutoApprove(): boolean {
    return !!this.listing?.autoApprove;
  }

  get canOwnerDelete(): boolean {
    return this.isOwner && !this.isBorrowed && !this.isPending && !this.isApproved;
  }

  get deleteConfirmMessage(): string {
    const title = this.listing?.title || '';
    return `${this.i18n.t('dashboard.delete_listing_msg')} "${title}"? ${this.i18n.t('dashboard.delete_listing_cannot_undo')}`;
  }

  get ownerJoinedYear(): number {
    const joined = this.listing?.owner?.joinedDate;
    if (joined) {
      const dt = new Date(joined);
      if (!isNaN(dt.getTime())) return dt.getFullYear();
    }
    return new Date().getFullYear();
  }

  get galleryImages(): string[] {
    const listing = this.listing;
    if (!listing) return [];
    const images = [listing.imageUrl, ...(listing.gallery || [])].filter(Boolean);
    return Array.from(new Set(images));
  }

  get pickupAreaLabel(): string {
    const listing = this.listing;
    if (!listing) return 'Nearby';

    const city = String((listing as any).pickupLocationCity || '').trim();
    const zip = String((listing as any).pickupLocationZip || '').trim();
    if (city || zip) return `${city} ${zip}`.trim();

    const pickupAddress = String(listing.pickupLocation?.address || '').trim();
    const maskedPickup = this.maskAddress(pickupAddress);
    if (maskedPickup) return maskedPickup;

    const ownerAddress = String(listing.owner?.address || '').trim();
    const maskedOwner = this.maskAddress(ownerAddress);
    if (maskedOwner) return maskedOwner;

    return 'Nearby';
  }

  private maskAddress(address: string): string {
    const s = String(address || '').replace(/\s+/g, ' ').trim();
    if (!s) return '';

    const parts = s.split(',').map(p => p.trim()).filter(Boolean);
    if (parts.length >= 2) {
      const cityZip = parts[parts.length - 1];
      return cityZip;
    }
    return s;
  }

  get activeImageIndex(): number {
    const images = this.galleryImages;
    const idx = images.findIndex(i => i === this.activeImage);
    return idx >= 0 ? idx : 0;
  }

  prevImage() {
    const images = this.galleryImages;
    if (images.length <= 1) return;
    const nextIdx = (this.activeImageIndex - 1 + images.length) % images.length;
    this.activeImage = images[nextIdx];
  }

  nextImage() {
    const images = this.galleryImages;
    if (images.length <= 1) return;
    const nextIdx = (this.activeImageIndex + 1) % images.length;
    this.activeImage = images[nextIdx];
  }

  decBookingDuration() {
    this.bookingDuration = Math.max(1, this.bookingDuration - 1);
  }

  incBookingDuration() {
    this.bookingDuration = Math.min(24, this.bookingDuration + 1);
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  private notifySuccess(message: string) {
    this.successMessage = message || '';
    this.showSuccess = true;
    if (this.successTimer) clearTimeout(this.successTimer);
    this.successTimer = setTimeout(() => {
      this.showSuccess = false;
      this.render();
    }, 5000);
    this.render();
  }

  private notifyError(message: string) {
    this.actionErrorMessage = message || '';
    this.showActionError = true;
    if (this.actionErrorTimer) clearTimeout(this.actionErrorTimer);
    this.actionErrorTimer = setTimeout(() => {
      this.showActionError = false;
      this.render();
    }, 5000);
    this.render();
  }

  private notifyShare(message: string) {
    this.shareNotice = message || '';
    if (this.shareTimer) clearTimeout(this.shareTimer);
    this.shareTimer = setTimeout(() => {
      this.shareNotice = null;
      this.render();
    }, 3000);
    this.render();
  }

  ngOnInit() {
    this.settingsConfig.ensureLoaded();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadData(id);
      }
    });
  }

  ngOnDestroy() {
    if (this.successTimer) clearTimeout(this.successTimer);
    if (this.actionErrorTimer) clearTimeout(this.actionErrorTimer);
    if (this.shareTimer) clearTimeout(this.shareTimer);
    if (this.statusPollTimer) clearInterval(this.statusPollTimer);
  }

  async loadData(id: string) {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      const direct = await this.api.getListingById(id);
      if (direct) {
        this.listing = direct;
      } else {
        const allListings = await this.api.getListings();
        this.listing = allListings.find(l => String((l as any).id) === String(id)) || null;
      }
      if (this.listing) {
        this.activeImage = this.listing.imageUrl || (this.listing.gallery && this.listing.gallery[0]) || '';
      }
      this.currentUser = await this.api.getCurrentUser();
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
      this.startStatusPolling();
      this.render();
    } catch (e) {
      this.error = e instanceof Error ? e.message : this.i18n.t('listing.error.load_failed');
      this.render();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private startStatusPolling() {
    if (this.statusPollTimer) clearInterval(this.statusPollTimer);
    const listing = this.listing;
    if (!listing) return;
    if (listing.status !== AvailabilityStatus.PENDING && listing.status !== AvailabilityStatus.APPROVED && listing.status !== AvailabilityStatus.PARTNER_BORROW_REQUESTED) return;

    this.statusPollTimer = setInterval(async () => {
      const current = this.listing;
      if (!current) return;
      if (current.status !== AvailabilityStatus.PENDING && current.status !== AvailabilityStatus.APPROVED && current.status !== AvailabilityStatus.PARTNER_BORROW_REQUESTED) {
        if (this.statusPollTimer) clearInterval(this.statusPollTimer);
        this.statusPollTimer = null;
        return;
      }
      try {
        const updated = await this.api.getListingById(current.id);
        if (updated && updated.status !== current.status) {
          this.listing = updated;
          if (updated.imageUrl) this.activeImage = updated.imageUrl;
          this.render();
        }
      } catch { }
    }, 2000);
  }

  get isOwner(): boolean {
    return !!(this.currentUser && this.listing && this.currentUser.id === this.listing.ownerId);
  }

  get isFree() {
    return this.finalTotalWithInsurance <= 0;
  }

  get borrowTierEnabled() {
    if (this.isPartnerListing) {
      return { deposit: false, verified: false, fee: false };
    }
    return {
      deposit: this.settingsConfig.isSectionEnabled('borrowing', 'deposit'),
      verified: this.settingsConfig.isSectionEnabled('borrowing', 'verified'),
      fee: this.settingsConfig.isSectionEnabled('borrowing', 'fee')
    };
  }

  get paymentOptions() {
    if (this.isPartnerListing) {
      return { card: false, paypal: false, cash: false };
    }
    return {
      card: this.settingsConfig.isSectionEnabled('borrowing', 'payments.card'),
      paypal: this.settingsConfig.isSectionEnabled('borrowing', 'payments.paypal'),
      cash: this.settingsConfig.isSectionEnabled('borrowing', 'payments.cash')
    };
  }

  get isGiveaway() {
    return this.listing?.type === ListingType.GIVE;
  }

  get isTimeBased() {
    if (!this.listing) return true;
    return this.listing.type !== ListingType.GIVE && this.listing.type !== ListingType.SELL;
  }

  get baseTotal() {
    if (!this.listing) return 0;
    const rate = this.listing.hourlyRate || 0;
    const duration = this.isTimeBased ? this.bookingDuration : 1;
    return rate * duration;
  }

  get serviceFee() {
    if (this.isPartnerListing) return 0;
    if (this.selectedPath !== 'FEE') return 0;
    return Math.round(this.baseTotal * 0.08 * 100) / 100;
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

  async initStripe() {
    this.stripe = await this.stripeClient.getStripe();
    this.stripeReady = !!this.stripe;
    if (this.stripeReady) {
      this.paymentMethods = await this.api.getPaymentMethods();
    } else {
      this.paymentMethods = [];
    }
    this.render();
  }

  handleRequest() {
    this.handleInitialRequestClick();
  }

  handleInitialRequestClick() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    if (this.isOwner) {
      this.notifyError(this.i18n.t('listing.error.borrow_own'));
      return;
    }

    if (this.isPartnerListing) {
      this.bookingDuration = 1;
      this.selectedPath = 'VERIFIED';
      this.paymentMethod = 'CASH';
      this.selectedSavedPaymentMethodId = null;
      this.cardError = null;
      this.stripeError = null;
      this.insuranceError = null;
      this.selectedInsuranceType = null;
      this.insuranceZipCode = '';
      this.insuranceQuote = null;
      this.insuranceTypes = [];
      this.showBookingModal = true;
      this.bookingStep = 'DURATION';
      this.render();
      return;
    }

    if (listing.type === ListingType.GIVE) {
      this.handleConfirmRequest();
      return;
    }

    this.selectedPath = this.borrowTierEnabled.verified ? 'VERIFIED' : (this.borrowTierEnabled.deposit ? 'DEPOSIT' : 'FEE');
    this.bookingDuration = 1;
    this.paymentMethod = this.paymentOptions.card ? 'CARD' : (this.paymentOptions.paypal ? 'PAYPAL' : 'CASH');
    this.selectedSavedPaymentMethodId = null;
    this.cardError = null;
    this.stripeError = null;
    this.insuranceError = null;
    this.selectedInsuranceType = null;
    this.insuranceZipCode = '';
    this.insuranceQuote = null;
    this.insuranceTypes = [];
    this.showBookingModal = true;
    this.bookingStep = 'PATH_SELECTION';
    if (this.requiresInsurance) {
      this.loadInsuranceTypes();
    }
    this.render();
  }

  closeBookingModal() {
    this.showBookingModal = false;
    this.bookingStep = 'PATH_SELECTION';
    this.cardError = null;
    this.stripeError = null;
    this.insuranceError = null;
    this.cleanupCardElement();
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
      this.handleConfirmRequest();
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
    await this.handleConfirmRequest();
  }

  mountPaymentCard() {
    if (!this.showBookingModal) return;
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

      this.wasAutoApproved = !!listing.autoApprove;
      this.closeBookingModal();
      await this.reloadListing();
      this.notifySuccess(this.wasAutoApproved ? this.i18n.t('listing.success.booked') : this.i18n.t('listing.success.request_sent'));
    } catch (e: any) {
      this.cardError = e?.message || this.i18n.t('listing.error.payment_failed');
    } finally {
      this.cardProcessing = false;
      this.render();
    }
  }

  async handleConfirmRequest() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }

    this.borrowing = true;
    this.render();
    try {
      if (listing.type === ListingType.GIVE) {
        await this.api.borrowListing(listing.id, { paymentMethod: 'GIFT', durationHours: 0 });
        this.wasAutoApproved = !!listing.autoApprove;
        await this.reloadListing();
        this.notifySuccess(this.wasAutoApproved ? this.i18n.t('listing.success.gift_claimed') : this.i18n.t('listing.success.gift_request'));
        return;
      }

      if (this.isPartnerListing) {
        await this.api.borrowListing(listing.id, {
          paymentMethod: 'PARTNER',
          durationHours: this.isTimeBased ? this.bookingDuration : 0,
          borrowerPath: 'PARTNER'
        });
        this.wasAutoApproved = false;
        this.closeBookingModal();
        await this.reloadListing();
        this.notifySuccess(this.i18n.t('listing.success.request_sent'));
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

      this.wasAutoApproved = !!listing.autoApprove;
      this.closeBookingModal();
      await this.reloadListing();
      this.notifySuccess(this.wasAutoApproved ? this.i18n.t('listing.success.booked') : this.i18n.t('listing.success.request_sent'));
    } catch (e: any) {
      this.notifyError(e?.message || this.i18n.t('listing.error.process_failed'));
    } finally {
      this.borrowing = false;
      this.render();
    }
  }

  get requiresInsurance(): boolean {
    if (this.isPartnerListing) return false;
    return !!(this.listing as any)?.insuranceRequired;
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

  async reloadListing() {
    const listing = this.listing;
    if (!listing) return;
    try {
      const updated = await this.api.getListingById(listing.id);
      if (updated) {
        this.listing = updated;
        if (updated.imageUrl) this.activeImage = updated.imageUrl;
      }
    } catch { }
    this.startStatusPolling();
    this.render();
  }

  openReportModal() {
    if (this.isOwner) {
      this.notifyError(this.i18n.t('listing.report.own_not_allowed'));
      return;
    }
    this.reportError = null;
    this.showReportModal = true;
    this.render();
  }

  closeReportModal() {
    this.showReportModal = false;
    this.reportError = null;
    this.reportDetails = '';
    this.reportReason = '';
    this.render();
  }

  closeReportSuccess() {
    this.showReportSuccess = false;
    this.render();
  }

  async submitReport() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    if (this.isOwner) {
      this.closeReportModal();
      this.notifyError(this.i18n.t('listing.report.own_not_allowed'));
      return;
    }
    this.reporting = true;
    this.reportError = null;
    this.render();
    try {
      await this.api.reportListing(listing.id, this.reportReason, this.reportDetails);
      this.showReportModal = false;
      this.showReportSuccess = true;
    } catch (e: any) {
      const msg = e?.message || this.i18n.t('listing.error.report_failed');
      this.showReportModal = false;
      this.reportError = msg.includes('already_reported_for_reason')
        ? this.i18n.t('listing.report.already_reported')
        : msg;
    } finally {
      this.reporting = false;
      this.render();
    }
  }

  async openUserProfile() {
    const listing = this.listing;
    if (!this.canOpenOwnerProfile) return;
    if (this.isPartnerListing) {
      this.showProfileModal = true;
      this.loadingReviews = false;
      this.reviews = [];
      this.render();
      return;
    }
    if (!listing?.ownerId) return;
    this.showProfileModal = true;
    this.loadingReviews = true;
    this.reviews = [];
    this.render();
    try {
      if (this.canShowOwnerReviews) {
        this.reviews = await this.api.getReviews(listing.ownerId);
      } else {
        this.reviews = [];
      }
    } catch {
      this.reviews = [];
    } finally {
      this.loadingReviews = false;
      this.render();
    }
  }

  closeUserProfile() {
    this.showProfileModal = false;
    this.render();
  }

  async handleApprove() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.actionLoading) return;
    this.actionLoading = 'APPROVE';
    this.render();
    try {
      await this.api.approveRequest(listing.id);
      await this.reloadListing();
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to approve');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  async handleDeny() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.actionLoading) return;
    this.actionLoading = 'DENY';
    this.render();
    try {
      await this.api.denyRequest(listing.id);
      await this.reloadListing();
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to deny');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  handleContact() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    const targetId = this.isOwner ? listing.borrowerId : listing.ownerId;
    const targetEmail = this.isOwner ? listing.borrower?.email : listing.owner?.email;
    if (targetEmail) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverEmail: targetEmail } });
      return;
    }
    if (targetId) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverId: targetId } });
      return;
    }
    this.router.navigate(['/mailbox']);
  }

  openDeleteModal() {
    if (!this.canOwnerDelete) return;
    this.showDeleteModal = true;
    this.render();
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
    this.render();
  }

  async confirmDeleteListing() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.deleting) return;
    this.deleting = true;
    this.render();
    try {
      await this.api.deleteListing(listing.id);
      this.closeDeleteModal();
      this.router.navigate(['/']);
    } catch (e: any) {
      this.closeDeleteModal();
      this.notifyError(e?.message || 'Failed to delete listing');
    } finally {
      this.deleting = false;
      this.render();
    }
  }

  contactOwner() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    const targetId = listing.ownerId || listing.borrowerId;
    const targetEmail = listing.owner?.email || listing.borrower?.email;
    if (targetEmail) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverEmail: targetEmail } });
      return;
    }
    if (targetId) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverId: targetId } });
      return;
    }
    this.router.navigate(['/mailbox']);
  }

  async handleVouch() {
    const listing = this.listing;
    if (!listing?.ownerId || !this.currentUser) return;
    if (this.isOwner) return;
    if (this.vouching || this.hasVouched) return;
    this.vouching = true;
    this.render();
    try {
      const updated = await this.api.vouchForUser(listing.ownerId);
      if (this.listing?.owner) {
        this.listing = { ...this.listing, owner: updated };
      }
      this.hasVouched = true;
    } catch {
      this.notifyError(this.i18n.t('listing.error.vouch_failed'));
    } finally {
      this.vouching = false;
      this.render();
    }
  }

  async share() {
    const url = window.location.href;
    try {
      await navigator.clipboard.writeText(url);
      this.notifyShare(this.i18n.t('listing.share.link_copied'));
    } catch {
      this.notifyError(`${this.i18n.t('listing.share.copy_prompt')} ${url}`);
    }
  }
}
