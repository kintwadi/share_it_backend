import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Package, Upload, Image as ImageIcon, Loader2, Sparkles, ChevronDown, X, Zap, ShieldCheck, Camera, CalendarDays, Infinity, Plus, CheckCircle2, CreditCard } from 'lucide-angular';
import { Subject, debounceTime } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { ListingType, Category, PickupLocation, ListingRecommendationResult, Listing } from '../../core/models/types';
import { ButtonComponent } from '../../shared/components/button/button';

@Component({
  selector: 'app-new-item',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ButtonComponent],
  templateUrl: './new-item.component.html',
  styleUrl: './new-item.component.css'
})
export class NewItemComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

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

  readonly ListingType = ListingType;

  loading = true;
  saving = false;
  uploadingCover = false;
  uploadingGallery = false;
  error: string | null = null;

  editId: string | null = null;
  categories: Category[] = [];
  pickupLocations: PickupLocation[] = [];
  subscription: any | null = null;
  subscriptionConfig = { starter: true, plus: true, pro: true };

  title = '';
  category = '';
  type: ListingType = ListingType.GIVE;
  description = '';
  hourlyRate: number = 0;
  imageUrl = '';
  gallery: string[] = [];
  autoApprove = false;
  insuranceRequired = false;
  pickupLocationId: string | null = null;

  pickupOption: 'concierge' | 'bakery' | 'public' | 'custom' = 'concierge';
  pickupLocationStreet = '';
  pickupLocationHouseNumber = '';
  pickupLocationCity = '';
  pickupLocationZip = '';
  startTime = '17:00';
  endTime = '19:00';
  timeError: string | null = null;
  availableFromDate = '';
  availableFromTime = '10:00';
  availableToDate = '';
  availableToTime = '18:00';
  availableUnlimited = false;
  availabilityError: string | null = null;

  recommendation: ListingRecommendationResult | null = null;
  recommendationLoading = false;
  recommendationError: string | null = null;
  private recommendationSubject = new Subject<void>();

  requiredPlan: 'plus' | 'pro' = 'pro';
  showPremiumModal = false;
  showPayoutSetupModal = false;
  payoutSetupLoading = false;
  private connectStatus: any | null = null;
  isSaving = false;

  readonly timeOptions = [
    '08:00','08:30','09:00','09:30','10:00','10:30',
    '11:00','11:30','12:00','12:30','13:00','13:30',
    '14:00','14:30','15:00','15:30','16:00','16:30',
    '17:00','17:30','18:00','18:30','19:00','19:30','20:00'
  ];

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get plan(): string {
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

  get pickupConcierge(): PickupLocation | null {
    return this.findPickupByKeyword('concierge') || this.pickupLocations[0] || null;
  }

  get pickupBakery(): PickupLocation | null {
    return this.findPickupByKeyword('bakery') || this.pickupLocations[0] || null;
  }

  get pickupPublic(): PickupLocation | null {
    return this.findPickupByKeyword('public') || this.pickupLocations[0] || null;
  }

  private findPickupByKeyword(keyword: string): PickupLocation | null {
    const k = keyword.toLowerCase();
    return this.pickupLocations.find(p => String(p.name || '').toLowerCase().includes(k)) || null;
  }

  selectPickupOption(opt: 'concierge' | 'bakery' | 'public' | 'custom') {
    this.pickupOption = opt;
    let selected: PickupLocation | null = null;
    if (opt === 'concierge') selected = this.pickupConcierge;
    if (opt === 'bakery') selected = this.pickupBakery;
    if (opt === 'public') selected = this.pickupPublic;
    this.pickupLocationId = opt === 'custom' ? null : (selected ? String(selected.id) : null);
    if (opt !== 'custom') {
      this.pickupLocationStreet = '';
      this.pickupLocationHouseNumber = '';
      this.pickupLocationCity = '';
      this.pickupLocationZip = '';
    }
    this.render();
  }

  handleStartChange(value: string) {
    this.startTime = value;
    if (this.endTime && value && this.endTime <= value) {
      this.timeError = this.i18n.t('new_item.error_time_order');
    } else {
      this.timeError = null;
    }
    this.render();
  }

  handleEndChange(value: string) {
    this.endTime = value;
    if (this.startTime && value && value <= this.startTime) {
      this.timeError = this.i18n.t('new_item.error_time_order');
    } else {
      this.timeError = null;
    }
    this.render();
  }

  setUnlimited(val: boolean) {
    this.availableUnlimited = val;
    this.availabilityError = null;
    if (val) {
      this.availableToDate = '';
      this.availableToTime = '';
    }
    this.render();
  }

  ngOnInit() {
    this.recommendationSubject.pipe(debounceTime(450)).subscribe(() => {
      this.runRecommendation();
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

      const [cats, locs, subCfg, sub] = await Promise.all([
        this.api.getCategories(),
        this.api.getPickupLocations(),
        this.api.getSubscriptionConfig().catch(() => ({ starter: true, plus: true, pro: true })),
        this.api.getCurrentSubscription().catch(() => null),
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

      if (!this.pickupLocationId && this.pickupLocations.length > 0) {
        this.selectPickupOption(this.pickupOption);
      } else if (this.pickupLocationId) {
        const selected = this.pickupLocations.find(p => String(p.id) === String(this.pickupLocationId));
        const name = String(selected?.name || '').toLowerCase();
        if (name.includes('bakery')) this.pickupOption = 'bakery';
        else if (name.includes('public')) this.pickupOption = 'public';
        else this.pickupOption = 'concierge';
      } else if (this.pickupLocationCity || this.pickupLocationZip) {
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
    this.hourlyRate = typeof listing.hourlyRate === 'number' ? listing.hourlyRate : Number(listing.hourlyRate ?? 0);
    this.imageUrl = listing.imageUrl ?? '';
    this.gallery = Array.isArray(listing.gallery) ? listing.gallery : [];
    this.autoApprove = !!listing.autoApprove;
    this.insuranceRequired = !!(listing as any).insuranceRequired;
    this.pickupLocationId = listing.pickupLocation?.id ? String(listing.pickupLocation.id) : null;
    this.pickupLocationStreet = String((listing as any).pickupLocationStreet || '');
    this.pickupLocationHouseNumber = String((listing as any).pickupLocationHouseNumber || '');
    this.pickupLocationCity = String((listing as any).pickupLocationCity || '');
    this.pickupLocationZip = String((listing as any).pickupLocationZip || '');
    if (!this.pickupLocationId && (this.pickupLocationStreet || this.pickupLocationHouseNumber || this.pickupLocationCity || this.pickupLocationZip)) {
      this.pickupOption = 'custom';
    }
  }

  onTypeSelect(type: ListingType, rate?: number) {
    if (type === ListingType.LEND) {
      if (this.plan === 'starter') {
        this.requiredPlan = 'plus';
        this.showPremiumModal = true;
        this.render();
        return;
      }
      if (!this.editId && this.plan === 'plus') {
        this.ensurePayoutsReadyForPaidLending();
        return;
      }
    }
    if (type === ListingType.SELL) {
      if (!this.subscriptionConfig.pro) return;
      if (!this.isPro) {
        this.requiredPlan = 'pro';
        this.showPremiumModal = true;
        this.render();
        return;
      }
    }
    this.type = type;
    if (typeof rate === 'number' && rate > 0 && type !== ListingType.GIVE) {
      this.hourlyRate = rate;
    } else if (type === ListingType.GIVE) {
      this.hourlyRate = 0;
    }
    if (this.isPremiumLender) this.autoApprove = true;
    this.triggerRecommendation();
    this.render();
  }

  private async ensurePayoutsReadyForPaidLending() {
    if (this.payoutSetupLoading) return;
    this.payoutSetupLoading = true;
    this.render();
    try {
      if (!this.connectStatus) {
        this.connectStatus = await this.api.getConnectStatus();
      }
      const ready = !!(this.connectStatus?.connected && this.connectStatus?.payoutsEnabled);
      if (!ready) {
        this.showPayoutSetupModal = true;
        return;
      }
      this.type = ListingType.LEND;
      if (this.isPremiumLender) this.autoApprove = true;
      this.triggerRecommendation();
    } catch {
      this.showPayoutSetupModal = true;
    } finally {
      this.payoutSetupLoading = false;
      this.render();
    }
  }

  goToManagePayouts() {
    this.showPayoutSetupModal = false;
    this.router.navigate(['/settings'], { queryParams: { tab: 'payments' } });
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
        estimatedValue: typeof this.hourlyRate === 'number' ? this.hourlyRate : undefined,
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
    this.uploadingCover = true;
    this.render();
    const localUrl = URL.createObjectURL(file);
    this.imageUrl = localUrl;
    try {
      const url = await this.api.uploadListingImage(file);
      if (url) this.imageUrl = url;
    } catch { }
    this.uploadingCover = false;
    input.value = '';
    this.render();
  }

  async onGallerySelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    if (files.length === 0) return;
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
      } catch { }
    }

    this.uploadingGallery = false;
    input.value = '';
    this.render();
  }

  removeGalleryImage(i: number) {
    this.gallery = this.gallery.filter((_, idx) => idx !== i);
    this.render();
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  goToUpgrade() {
    this.showPremiumModal = false;
    this.router.navigate(['/subscription'], {
      queryParams: { fromUpgrade: true, requiredPlan: this.requiredPlan, from: '/new-item' },
      state: { fromUpgrade: true, requiredPlan: this.requiredPlan, from: '/new-item' } as any
    });
  }

  async handleSave() {
    this.error = null;
    this.timeError = null;
    this.availabilityError = null;

    if (
      this.availableFromDate &&
      this.availableToDate &&
      (
        this.availableToDate < this.availableFromDate ||
        (this.availableToDate === this.availableFromDate && this.availableToTime <= this.availableFromTime)
      )
    ) {
      this.availabilityError = this.i18n.t('new_item.error_time_order');
      this.render();
      return;
    }
    if (!this.startTime || !this.endTime || this.endTime <= this.startTime) {
      this.error = this.i18n.t('new_item.error_time_window');
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

    if (this.type === ListingType.LEND && this.plan === 'starter') {
      this.requiredPlan = 'plus';
      this.showPremiumModal = true;
      this.render();
      return;
    }
    if (this.type === ListingType.LEND && this.plan === 'plus' && Number(this.hourlyRate || 0) > 0) {
      await this.ensurePayoutsReadyForPaidLending();
      if (this.showPayoutSetupModal) return;
    }

    this.saving = true;
    this.render();
    try {
      const payload = {
        title: this.title.trim(),
        description: this.description ?? '',
        category: this.category,
        type: this.type,
        hourlyRate: this.type === ListingType.GIVE ? 0 : (this.hourlyRate ?? 0),
        imageUrl: this.imageUrl,
        gallery: this.gallery,
        autoApprove: this.isPremiumLender ? true : !!this.autoApprove,
        insuranceRequired: !!this.insuranceRequired,
        x: 0,
        y: 0,
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
      this.error = e?.message || this.i18n.t('new_item.error.save_failed');
    } finally {
      this.saving = false;
      this.render();
    }
  }
}
