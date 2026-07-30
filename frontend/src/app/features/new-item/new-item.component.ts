import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Package, Upload, Image as ImageIcon, Loader2, Sparkles, ChevronDown, X, Zap, ShieldCheck, Camera, CalendarDays, Infinity, Plus, CheckCircle2, CreditCard, Info, Search } from 'lucide-angular';
import { Subject, debounceTime } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { SubscriptionFeatureService } from '../../core/services/subscription-feature.service';
import { ListingType, Category, ExchangeLocation, ListingRecommendationResult, Listing, ListingPricingUnit } from '../../core/models/types';
import { LocationApiService, LocationResponse } from '../../core/services/location-api.service';
import { PlatformGeolocationService } from '../../core/services/platform-geolocation.service';
import { LayoutModeService } from '../../core/services/layout-mode.service';

@Component({
  selector: 'app-new-item',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './new-item.component.html',
  styleUrl: './new-item.component.css'
})
export class NewItemComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private fb = inject(FormBuilder);
  private locationApi = inject(LocationApiService);
  private platformGeolocation = inject(PlatformGeolocationService);
  i18n = inject(I18nService);
  settingsConfig = inject(SettingsConfigService);
  subscriptionFeature = inject(SubscriptionFeatureService);
  layoutMode = inject(LayoutModeService);

  readonly Package = Package;
  readonly Upload = Upload;
  readonly ImageIcon = ImageIcon;
  readonly Loader2 = Loader2;
  readonly Sparkles = Sparkles;
  readonly ChevronDown = ChevronDown;
  readonly X = X;
  readonly Zap = Zap;
  readonly ShieldCheck = ShieldCheck;
  readonly Camera = Camera;
  readonly CalendarDays = CalendarDays;
  readonly Infinity = Infinity;
  readonly Plus = Plus;
  readonly CheckCircle2 = CheckCircle2;
  readonly CreditCard = CreditCard;
  readonly Info = Info;
  readonly Search = Search;

  readonly ListingType = ListingType;
  readonly ListingPricingUnit = ListingPricingUnit;

  loading = true;
  saving = false;
  uploadingCover = false;
  uploadingGallery = false;
  error: string | null = null;

  editId: string | null = null;
  categories: Category[] = [];
  pickupLocations: ExchangeLocation[] = [];
  subscription: any | null = null;
  subscriptionConfig = { starter: true, plus: true, pro: true };

  title = '';
  category = '';
  type: ListingType = ListingType.GIVE;
  description = '';
  hourlyRate: number = 0;
  dailyRate: number = 0;
  monthlyRate: number = 0;
  pricingUnit: ListingPricingUnit = ListingPricingUnit.HOURLY;
  showAdvancedRates = false;
  imageUrl = '';
  gallery: string[] = [];
  x: number | null = null;
  y: number | null = null;
  addressForm = this.fb.group({
    streetAddress: [''],
    city: [''],
    postalCode: [''],
    country: ['PT'],
  });
  locationLookupLoading = false;
  locationLookupError: string | null = null;
  locationPermissionHintVisible = false;
  readonly countryOptions = [
    { code: 'PT', label: 'Portugal' },
    { code: 'DE', label: 'Germany' },
    { code: 'FR', label: 'France' },
    { code: 'BE', label: 'Belgium' },
    { code: 'NL', label: 'Netherlands' },
    { code: 'ES', label: 'Spain' },
    { code: 'IT', label: 'Italy' },
    { code: 'AT', label: 'Austria' },
    { code: 'CH', label: 'Switzerland' },
    { code: 'LU', label: 'Luxembourg' },
  ];
  autoApprove = false;
  insuranceRequired = false;
  pickupLocationId: string | null = null;

  pickupOption: 'exchange' | 'custom' = 'exchange';
  pickupLocationStreet = '';
  pickupLocationHouseNumber = '';
  pickupLocationCity = '';
  pickupLocationZip = '';
  pickupCustomQuery = '';
  pickupCustomSuggestions: LocationResponse[] = [];
  pickupCustomLoading = false;
  pickupCustomError: string | null = null;
  availableFromDate = '';
  availableFromTime = '10:00';
  availableUnlimited = false;
  availabilityError: string | null = null;

  recommendation: ListingRecommendationResult | null = null;
  recommendationLoading = false;
  recommendationError: string | null = null;
  private recommendationSubject = new Subject<void>();
  private pickupCustomQuerySubject = new Subject<string>();

  requiredPlan: 'plus' | 'pro' = 'pro';
  payoutSetupLoading = false;
  private connectStatus: any | null = null;
  showLendPayoutNotice = false;
  isSaving = false;

  readonly timeOptions = [
    '08:00','08:30','09:00','09:30','10:00','10:30',
    '11:00','11:30','12:00','12:30','13:00','13:30',
    '14:00','14:30','15:00','15:30','16:00','16:30',
    '17:00','17:30','18:00','18:30','19:00','19:30','20:00'
  ];
  private readonly allowedImageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get plan(): string {
    if (!this.subscriptionFeature.enabled()) return 'pro';
    return String(this.subscription?.planType ?? 'starter').toLowerCase();
  }

  get isPremiumLender(): boolean {
    const p = this.plan;
    return p === 'plus' || p === 'pro' || p.includes('premium');
  }

  get isPro(): boolean {
    const p = this.plan;
    return p === 'pro' || p.includes('premium');
  }

  get showTypeGoods(): boolean {
    return this.type === ListingType.GOODS;
  }

  get showTypeSkill(): boolean {
    return this.type === ListingType.SKILL;
  }

  get canConfigurePricing(): boolean {
    return this.type === ListingType.LEND;
  }

  get pricingUnitLabel(): string {
    if (this.pricingUnit === ListingPricingUnit.DAILY) return 'Daily';
    if (this.pricingUnit === ListingPricingUnit.MONTHLY) return 'Monthly';
    return 'Hourly';
  }

  get pricingUnitSuffix(): string {
    if (this.type === ListingType.SELL || this.type === ListingType.GIVE) return '';
    if (this.pricingUnit === ListingPricingUnit.DAILY) return '/day';
    if (this.pricingUnit === ListingPricingUnit.MONTHLY) return '/mo';
    return '/hr';
  }

  get pricingPreviewLabel(): string {
    if (this.type === ListingType.SELL) return 'total';
    if (this.type === ListingType.GIVE) return '';
    if (this.pricingUnit === ListingPricingUnit.DAILY) return 'per day';
    if (this.pricingUnit === ListingPricingUnit.MONTHLY) return 'per month';
    return 'per hour';
  }

  get pricingInputLabel(): string {
    if (this.type === ListingType.SELL) return 'Price';
    if (this.type === ListingType.GIVE) return 'Value';
    if (this.pricingUnit === ListingPricingUnit.DAILY) return 'Daily rate';
    if (this.pricingUnit === ListingPricingUnit.MONTHLY) return 'Monthly rate';
    return 'Hourly rate';
  }

  get selectedPricingRate(): number {
    if (this.type !== ListingType.LEND) return this.hourlyRate || 0;
    if (this.pricingUnit === ListingPricingUnit.DAILY) return this.dailyRate || 0;
    if (this.pricingUnit === ListingPricingUnit.MONTHLY) return this.monthlyRate || 0;
    return this.hourlyRate || 0;
  }

  get pricingSummaryPrimary(): string {
    return `${this.i18n.formatPrice(this.selectedPricingRate)}${this.pricingUnitSuffix}`;
  }

  get pricingSummaryDaily(): string {
    return this.dailyRate > 0 ? `${this.i18n.formatPrice(this.dailyRate)}/day` : '—';
  }

  get pricingSummaryMonthly(): string {
    return this.monthlyRate > 0 ? `${this.i18n.formatPrice(this.monthlyRate)}/mo` : '—';
  }

  get showPricingSummaryExtras(): boolean {
    if (!this.canConfigurePricing) return false;
    if (this.pricingUnit !== ListingPricingUnit.HOURLY && this.hourlyRate > 0) return true;
    if (this.pricingUnit !== ListingPricingUnit.DAILY && this.dailyRate > 0) return true;
    if (this.pricingUnit !== ListingPricingUnit.MONTHLY && this.monthlyRate > 0) return true;
    return false;
  }

  get sellEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('enable', 'sell');
  }

  get selectedExchangeLocation(): ExchangeLocation | null {
    if (this.pickupOption !== 'exchange') return null;
    const id = String(this.pickupLocationId || '');
    if (!id) return null;
    return this.pickupLocations.find(p => String(p.id) === id) ?? null;
  }

  selectExchangeLocation(loc: ExchangeLocation) {
    this.pickupOption = 'exchange';
    this.pickupLocationId = loc?.id ? String(loc.id) : null;
    this.pickupLocationStreet = '';
    this.pickupLocationHouseNumber = '';
    this.pickupLocationCity = '';
    this.pickupLocationZip = '';
    this.pickupCustomQuery = '';
    this.pickupCustomSuggestions = [];
    this.pickupCustomError = null;
    this.render();
  }

  selectCustomPickup() {
    this.pickupOption = 'custom';
    this.pickupLocationId = null;
    this.pickupCustomSuggestions = [];
    this.pickupCustomError = null;
    this.render();
  }

  onPickupCustomQueryChange(v: string) {
    this.pickupCustomQuery = v;
    this.pickupCustomQuerySubject.next(v);
  }

  private async loadPickupCustomSuggestions(q: string) {
    const query = String(q || '').trim();
    if (query.length < 2) {
      this.pickupCustomSuggestions = [];
      this.pickupCustomLoading = false;
      this.render();
      return;
    }
    this.pickupCustomLoading = true;
    this.pickupCustomError = null;
    this.render();
    try {
      this.pickupCustomSuggestions = await this.locationApi.autocomplete(query, undefined, 6);
    } catch {
      this.pickupCustomSuggestions = [];
      this.pickupCustomError = this.i18n.t('new_item.pickup_custom_error');
    } finally {
      this.pickupCustomLoading = false;
      this.render();
    }
  }

  selectPickupCustomSuggestion(s: LocationResponse) {
    this.pickupCustomSuggestions = [];
    this.pickupCustomError = null;
    this.pickupCustomQuery = String(s.displayName || s.streetAddress || '').trim();

    const streetAddress = String(s.streetAddress || '').trim();
    const split = this.splitStreetAndHouse(streetAddress);
    if (split.street) this.pickupLocationStreet = split.street;
    if (split.house) this.pickupLocationHouseNumber = split.house;
    this.pickupLocationCity = String(s.city || this.pickupLocationCity || '').trim();
    this.pickupLocationZip = String(s.postalCode || this.pickupLocationZip || '').trim();
    this.render();
  }

  private splitStreetAndHouse(streetAddress: string): { street: string; house: string } {
    const s = String(streetAddress || '').trim();
    if (!s) return { street: '', house: '' };

    const startMatch = s.match(/^(\d+[a-zA-Z]?)\s+(.+)$/);
    if (startMatch) return { house: startMatch[1], street: startMatch[2].trim() };

    const endMatch = s.match(/^(.+?)\s+(\d+[a-zA-Z]?)$/);
    if (endMatch) return { street: endMatch[1].trim(), house: endMatch[2] };

    return { street: s, house: '' };
  }

  setUnlimited(val: boolean) {
    this.availableUnlimited = val;
    this.availabilityError = null;
    this.render();
  }

  ngOnInit() {
    this.recommendationSubject.pipe(debounceTime(450)).subscribe(() => {
      this.runRecommendation();
    });
    this.pickupCustomQuerySubject.pipe(debounceTime(250)).subscribe(q => {
      this.loadPickupCustomSuggestions(q);
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      this.editId = id;
      this.init();
    });
  }

  async init() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      const u = await this.api.getCurrentUser();
      if (!u) {
        this.router.navigate(['/connect']);
        return;
      }

      await this.settingsConfig.ensureLoaded();
      const subscriptionEnabled = this.subscriptionFeature.enabled();
      const [cats, locs, subCfg, sub] = await Promise.all([
        this.api.getCategories(),
        this.api.getExchangeLocations(),
        subscriptionEnabled ? this.api.getSubscriptionConfig().catch(() => ({ starter: true, plus: true, pro: true })) : Promise.resolve({ starter: true, plus: true, pro: true }),
        subscriptionEnabled ? this.api.getCurrentSubscription().catch(() => null) : Promise.resolve(null),
      ]);

      this.categories = cats;
      this.pickupLocations = locs;
      this.subscriptionConfig = subCfg;
      this.subscription = sub;

      if (!this.category) {
        const first = this.categories[0]?.name || this.i18n.t('new_item.tools');
        this.category = first;
      }

      if (this.isPremiumLender) {
        this.autoApprove = true;
      }

      if (this.editId) {
        const listing = await this.api.getListingById(this.editId);
        if (!listing) {
          this.error = this.i18n.t('new_item.error.not_found');
        } else {
          this.prefillFromListing(listing);
        }
      }

      if (this.pickupLocationId) {
        this.pickupOption = 'exchange';
      } else if (this.pickupLocationCity || this.pickupLocationZip) {
        this.pickupOption = 'custom';
      } else if (this.pickupLocations.length > 0) {
        this.pickupOption = 'exchange';
        this.pickupLocationId = String(this.pickupLocations[0].id);
      } else {
        this.pickupOption = 'custom';
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('new_item.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private prefillFromListing(listing: Listing) {
    this.title = listing.title ?? '';
    this.category = listing.category ?? this.category;
    this.type = listing.type ?? ListingType.GOODS;
    this.description = listing.description ?? '';
    const parsedPricingUnit = (() => {
      const raw = String((listing as any).pricingUnit || '').trim().toUpperCase();
      if (raw === ListingPricingUnit.DAILY) return ListingPricingUnit.DAILY;
      if (raw === ListingPricingUnit.MONTHLY) return ListingPricingUnit.MONTHLY;
      return ListingPricingUnit.HOURLY;
    })();
    const rawHourly = typeof listing.hourlyRate === 'number' ? listing.hourlyRate : Number(listing.hourlyRate ?? 0);
    const rawDaily = typeof (listing as any).dailyRate === 'number' ? (listing as any).dailyRate : Number((listing as any).dailyRate ?? 0);
    const rawMonthly = typeof (listing as any).monthlyRate === 'number' ? (listing as any).monthlyRate : Number((listing as any).monthlyRate ?? 0);
    this.pricingUnit = parsedPricingUnit;
    this.hourlyRate = parsedPricingUnit === ListingPricingUnit.DAILY || parsedPricingUnit === ListingPricingUnit.MONTHLY
      ? (rawDaily > 0 || rawMonthly > 0 ? rawHourly : 0)
      : rawHourly;
    this.dailyRate = rawDaily > 0 ? rawDaily : (parsedPricingUnit === ListingPricingUnit.DAILY ? rawHourly : 0);
    this.monthlyRate = rawMonthly > 0 ? rawMonthly : (parsedPricingUnit === ListingPricingUnit.MONTHLY ? rawHourly : 0);
    this.imageUrl = listing.imageUrl ?? '';
    this.gallery = Array.isArray(listing.gallery) ? listing.gallery : [];
    this.autoApprove = !!listing.autoApprove;
    this.insuranceRequired = !!(listing as any).insuranceRequired;
    this.pickupLocationId = listing.pickupLocation?.id ? String(listing.pickupLocation.id) : null;
    this.pickupLocationStreet = String((listing as any).pickupLocationStreet || '');
    this.pickupLocationHouseNumber = String((listing as any).pickupLocationHouseNumber || '');
    this.pickupLocationCity = String((listing as any).pickupLocationCity || '');
    this.pickupLocationZip = String((listing as any).pickupLocationZip || '');
    if (this.pickupLocationId) this.pickupOption = 'exchange';
    else if (this.pickupLocationStreet || this.pickupLocationHouseNumber || this.pickupLocationCity || this.pickupLocationZip) this.pickupOption = 'custom';
    const availableUnlimited = !!(listing as any).availableUnlimited;
    const availableFrom = (listing as any).availableFrom ? String((listing as any).availableFrom) : '';
    this.availableUnlimited = availableUnlimited;
    this.x = typeof (listing as any)?.location?.x === 'number' ? (listing as any).location.x : null;
    this.y = typeof (listing as any)?.location?.y === 'number' ? (listing as any).location.y : null;
    if (!availableUnlimited && availableFrom && availableFrom.length >= 16) {
      this.availableFromDate = availableFrom.slice(0, 10);
      this.availableFromTime = availableFrom.slice(11, 16) || this.availableFromTime;
    } else {
      this.availableFromDate = '';
      this.availableFromTime = '10:00';
    }
  }

  async onTypeSelect(type: ListingType, rate?: number) {
    if (type === ListingType.SELL && !this.sellEnabled) return;
    const subscriptionEnabled = this.subscriptionFeature.enabled();
    if (type !== ListingType.LEND) {
      this.showLendPayoutNotice = false;
    }
    if (type === ListingType.LEND) {
      if (subscriptionEnabled && this.plan === 'starter') {
        this.requiredPlan = 'plus';
        this.goToUpgrade();
        return;
      }
      if (!this.editId) {
        const ready = await this.ensurePayoutsReadyForLending();
        if (!ready) {
          this.render();
          return;
        }
      }
    }
    if (type === ListingType.SELL) {
      if (subscriptionEnabled && !this.subscriptionConfig.pro) return;
      if (subscriptionEnabled && !this.isPro) {
        this.requiredPlan = 'pro';
        this.goToUpgrade();
        return;
      }
    }
    this.showLendPayoutNotice = false;
    this.type = type;
    if (typeof rate === 'number' && rate > 0 && type !== ListingType.GIVE) {
      this.hourlyRate = rate;
    } else if (type === ListingType.GIVE) {
      this.hourlyRate = 0;
      this.pricingUnit = ListingPricingUnit.HOURLY;
    }
    if (type !== ListingType.LEND) {
      this.pricingUnit = ListingPricingUnit.HOURLY;
    }
    if (this.isPremiumLender) this.autoApprove = true;
    this.triggerRecommendation();
    this.render();
  }

  setPricingUnit(unit: ListingPricingUnit) {
    if (!this.canConfigurePricing) return;
    this.pricingUnit = unit;
    this.render();
  }

  toggleAdvancedRates() {
    if (!this.canConfigurePricing) return;
    this.showAdvancedRates = !this.showAdvancedRates;
    this.render();
  }

  onPrimaryRateChange(value: number | string) {
    const rate = this.normalizeRateValue(value);
    if (!this.canConfigurePricing) {
      this.hourlyRate = rate;
    } else if (this.pricingUnit === ListingPricingUnit.DAILY) {
      this.dailyRate = rate;
    } else if (this.pricingUnit === ListingPricingUnit.MONTHLY) {
      this.monthlyRate = rate;
    } else {
      this.hourlyRate = rate;
    }
    this.triggerRecommendation();
    this.render();
  }

  onAdvancedRateChange(unit: ListingPricingUnit, value: number | string) {
    const rate = this.normalizeRateValue(value);
    if (unit === ListingPricingUnit.DAILY) this.dailyRate = rate;
    else if (unit === ListingPricingUnit.MONTHLY) this.monthlyRate = rate;
    else this.hourlyRate = rate;
    this.triggerRecommendation();
    this.render();
  }

  private normalizeRateValue(value: number | string): number {
    const parsed = Number(value ?? 0);
    if (!Number.isFinite(parsed) || parsed < 0) return 0;
    return parsed;
  }

  private async ensurePayoutsReadyForLending(): Promise<boolean> {
    if (this.editId) return true;
    if (this.payoutSetupLoading) return false;
    this.payoutSetupLoading = true;
    this.render();
    try {
      this.connectStatus = await this.api.getConnectStatus();
      const ready = !!(this.connectStatus?.connected && this.connectStatus?.payoutsEnabled);
      if (!ready) {
        this.showLendPayoutNotice = true;
        return false;
      }
      this.showLendPayoutNotice = false;
      this.type = ListingType.LEND;
      if (this.isPremiumLender) this.autoApprove = true;
      this.triggerRecommendation();
      return true;
    } catch {
      this.showLendPayoutNotice = true;
      return false;
    } finally {
      this.payoutSetupLoading = false;
      this.render();
    }
  }

  triggerRecommendation() {
    this.recommendationSubject.next();
  }

  private async runRecommendation() {
    const title = String(this.title || '').trim();
    const category = String(this.category || '').trim();

    if (this.type !== ListingType.GIVE || title.length < 3 || !category) {
      this.recommendation = null;
      this.recommendationError = null;
      this.recommendationLoading = false;
      this.render();
      return;
    }

    this.recommendationLoading = true;
    this.recommendationError = null;
    this.render();

    try {
      const res = await this.api.evaluateListingRecommendation({
        title,
        category,
        description: String(this.description || ''),
        estimatedValue: typeof this.selectedPricingRate === 'number' ? this.selectedPricingRate : undefined,
      });
      this.recommendation = res;
    } catch (e: any) {
      this.recommendation = null;
      this.recommendationError = e?.message || this.i18n.t('new_item.error.evaluate_failed');
    } finally {
      this.recommendationLoading = false;
      this.render();
    }
  }

  async onCoverSelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (this.uploadingCover) return;
    const fileError = this.validateSelectedImageFile(file);
    if (fileError) {
      this.error = fileError;
      input.value = '';
      this.render();
      return;
    }
    this.error = null;
    this.uploadingCover = true;
    this.render();
    const localUrl = URL.createObjectURL(file);
    this.imageUrl = localUrl;
    try {
      const url = await this.api.uploadListingImage(file);
      if (url) this.imageUrl = url;
    } catch (e: any) {
      this.imageUrl = '';
      this.error = this.mapImageUploadError(e, file);
    }
    this.uploadingCover = false;
    input.value = '';
    this.render();
  }

  async onGallerySelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    if (files.length === 0) return;
    if (this.uploadingGallery) return;
    for (const file of files) {
      const fileError = this.validateSelectedImageFile(file);
      if (fileError) {
        this.error = fileError;
        input.value = '';
        this.render();
        return;
      }
    }
    this.error = null;
    this.uploadingGallery = true;
    this.render();

    for (const file of files) {
      const localUrl = URL.createObjectURL(file);
      this.gallery = [...this.gallery, localUrl];
      this.render();
      try {
        const url = await this.api.uploadListingImage(file);
        if (url) {
          const idx = this.gallery.lastIndexOf(localUrl);
          if (idx !== -1) {
            const next = [...this.gallery];
            next[idx] = url;
            this.gallery = next;
          }
        }
      } catch (e: any) {
        this.gallery = this.gallery.filter(g => g !== localUrl);
        this.error = this.mapImageUploadError(e, file);
      }
    }

    this.uploadingGallery = false;
    input.value = '';
    this.render();
  }

  removeGalleryImage(i: number) {
    this.gallery = this.gallery.filter((_, idx) => idx !== i);
    this.render();
  }

  private validateSelectedImageFile(file: File): string | null {
    const ext = this.extractFileExtension(file?.name || '');
    if (!ext || !this.allowedImageExtensions.includes(ext)) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    const type = String(file?.type || '').toLowerCase();
    if (type && !type.startsWith('image/')) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    return null;
  }

  private mapImageUploadError(error: any, file: File): string {
    const localValidation = this.validateSelectedImageFile(file);
    if (localValidation) return localValidation;
    const raw = String(
      error?.error?.message ||
      error?.error?.error ||
      error?.message ||
      ''
    ).toLowerCase();
    if (raw.includes('file_type_not_allowed')) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    if (raw.includes('file_too_large')) {
      return this.i18n.t('new_item.error.file_too_large');
    }
    return this.i18n.t('new_item.error.upload_failed');
  }

  private extractFileExtension(filename: string): string {
    const value = String(filename || '').trim().toLowerCase();
    const dot = value.lastIndexOf('.');
    if (dot < 0 || dot >= value.length - 1) return '';
    return value.slice(dot + 1);
  }

  private mapListingSaveError(error: any): string {
    const raw = String(
      error?.error?.error ||
      error?.error?.message ||
      error?.message ||
      ''
    ).toLowerCase();
    if (raw.includes('subscription_required_for_lending')) {
      return this.i18n.t('new_item.error.subscription_required_for_lending');
    }
    if (raw.includes('subscription_required_for_selling')) {
      return this.i18n.t('new_item.error.subscription_required_for_selling');
    }
    if (raw.includes('selling_disabled')) {
      return this.i18n.t('new_item.error.selling_disabled');
    }
    if (raw.includes('pickup_location_not_found')) {
      return this.i18n.t('new_item.error.pickup_location_not_found');
    }
    if (raw.includes('validation_error')) {
      return this.i18n.t('new_item.error.required_fields');
    }
    if (raw.includes('invalid_request_body')) {
      return this.i18n.t('new_item.error.invalid_request_body');
    }
    if (raw.includes('file_type_not_allowed')) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    if (raw.includes('file_too_large')) {
      return this.i18n.t('new_item.error.file_too_large');
    }
    if (raw.includes('forbidden')) {
      return this.i18n.t('new_item.error.forbidden');
    }
    return this.i18n.t('new_item.error.save_failed');
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  goToUpgrade() {
    this.router.navigate(['/subscription'], {
      queryParams: { fromUpgrade: true, requiredPlan: this.requiredPlan, from: '/new-item' },
      state: { fromUpgrade: true, requiredPlan: this.requiredPlan, from: '/new-item' } as any
    });
  }

  goToPaymentsManage() {
    this.router.navigate(['/settings'], {
      queryParams: { tab: 'payments', from: 'new-item-lend' }
    });
  }

  async handleSave() {
    this.error = null;
    this.availabilityError = null;
    this.locationLookupError = null;
    if (this.uploadingCover || this.uploadingGallery) {
      this.error = this.i18n.t('new_item.error.wait_upload');
      this.render();
      return;
    }

    if (!this.availableUnlimited && !this.availableFromDate) {
      this.availabilityError = this.i18n.t('new_item.error_available_from_required');
      this.render();
      return;
    }
    if (!this.title.trim() || !this.category || !this.type) {
      this.error = this.i18n.t('new_item.error.required_fields');
      this.render();
      return;
    }
    if (!this.imageUrl.trim()) {
      this.error = this.i18n.t('new_item.error.cover_required');
      this.render();
      return;
    }
    if (String(this.imageUrl || '').startsWith('blob:') || (this.gallery || []).some(g => String(g || '').startsWith('blob:'))) {
      this.error = this.i18n.t('new_item.error.wait_upload');
      this.render();
      return;
    }

    const address = this.addressForm.getRawValue();
    const streetAddress = String(address.streetAddress || '').trim();
    const city = String(address.city || '').trim();
    const postalCode = String(address.postalCode || '').trim();
    const country = String(address.country || '').trim();
    if (streetAddress.length < 2 || city.length < 2 || postalCode.length < 3 || country.length < 2) {
      this.error = this.i18n.t('new_item.error.address_required');
      this.render();
      return;
    }

    if (this.pickupOption === 'custom') {
      const street = String(this.pickupLocationStreet || '').trim();
      const house = String(this.pickupLocationHouseNumber || '').trim();
      const city = String(this.pickupLocationCity || '').trim();
      const zip = String(this.pickupLocationZip || '').trim();
      const zipOk = zip.length >= 3 && zip.length <= 20 && /[0-9]/.test(zip);
      if (street.length < 2 || house.length < 1 || city.length < 2 || !zipOk) {
        this.error = 'Please enter a coarse pickup location (street + house number + city + ZIP code).';
        this.render();
        return;
      }
    }

    if (this.subscriptionFeature.enabled() && this.type === ListingType.LEND && this.plan === 'starter') {
      this.requiredPlan = 'plus';
      this.goToUpgrade();
      return;
    }
    if (!this.editId && this.type === ListingType.LEND) {
      const ok = await this.ensurePayoutsReadyForLending();
      if (!ok) return;
    }

    this.saving = true;
    this.render();
    try {
      const availableUnlimited = !!this.availableUnlimited;
      const availableFrom = availableUnlimited || !this.availableFromDate ? null : `${this.availableFromDate}T${this.availableFromTime}:00`;
      const payload = {
        title: this.title.trim(),
        description: this.description ?? '',
        category: this.category,
        type: this.type,
        hourlyRate: this.type === ListingType.GIVE ? 0 : (this.hourlyRate ?? 0),
        dailyRate: this.type === ListingType.LEND ? (this.dailyRate ?? 0) : 0,
        monthlyRate: this.type === ListingType.LEND ? (this.monthlyRate ?? 0) : 0,
        pricingUnit: this.type === ListingType.LEND ? this.pricingUnit : ListingPricingUnit.HOURLY,
        imageUrl: this.imageUrl,
        gallery: this.gallery,
        autoApprove: this.isPremiumLender ? true : !!this.autoApprove,
        insuranceRequired: !!this.insuranceRequired,
        x: this.x ?? undefined,
        y: this.y ?? undefined,
        streetAddress,
        city,
        postalCode,
        country,
        availableUnlimited,
        availableFrom,
        availableTo: null,
        pickupLocationId: this.pickupOption === 'custom' ? null : this.pickupLocationId,
        pickupLocationCustom: null,
        pickupLocationStreet: this.pickupOption === 'custom' ? (this.pickupLocationStreet || null) : null,
        pickupLocationHouseNumber: this.pickupOption === 'custom' ? (this.pickupLocationHouseNumber || null) : null,
        pickupLocationCity: this.pickupOption === 'custom' ? (this.pickupLocationCity || null) : null,
        pickupLocationZip: this.pickupOption === 'custom' ? (this.pickupLocationZip || null) : null,
      };

      if (this.editId) {
        await this.api.updateListing(this.editId, payload);
      } else {
        await this.api.createListing(payload);
      }
      this.router.navigate(['/dashboard']);
    } catch (e: any) {
      this.error = this.mapListingSaveError(e);
    } finally {
      this.saving = false;
      this.render();
    }
  }

  async useMyCurrentLocation() {
    this.locationLookupError = null;
    this.locationPermissionHintVisible = true;
    this.locationLookupLoading = true;
    this.render();
    try {
      await new Promise<void>(resolve => setTimeout(resolve, 50));
      const pos = await this.platformGeolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000
      });

      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      this.x = lat;
      this.y = lng;

      const loc = await this.locationApi.reverseGeocode(lat, lng);
      const cc = String(loc.countryCode || '').toUpperCase();
      const selectedCountry = this.countryOptions.some(c => c.code === cc) ? cc : String(this.addressForm.get('country')?.value || 'PT');
      this.addressForm.patchValue({
        streetAddress: String(loc.streetAddress || ''),
        city: String(loc.city || ''),
        postalCode: String(loc.postalCode || ''),
        country: selectedCountry,
      });
    } catch (err: any) {
      const code = typeof err?.code === 'number' ? Number(err.code) : null;
      if (String(err?.message || '').toLowerCase().includes('not supported')) {
        this.locationLookupError = this.i18n.t('new_item.error.geo_not_supported');
      } else if (code === 1) {
        this.locationLookupError = this.i18n.t('new_item.error.geo_denied');
      } else if (code === 3) {
        this.locationLookupError = this.i18n.t('new_item.error.geo_timeout');
      } else {
        this.locationLookupError = this.i18n.t('new_item.error.geo_failed');
      }
    } finally {
      this.locationLookupLoading = false;
      this.locationPermissionHintVisible = false;
      this.render();
    }
  }
}
