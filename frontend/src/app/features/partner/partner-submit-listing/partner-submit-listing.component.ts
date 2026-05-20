import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Package, Upload, Image as ImageIcon, ChevronDown, X, Plus } from 'lucide-angular';
import { ApiService } from '../../../core/services/api.service';
import { PartnerService } from '../../../core/services/partner.service';
import { ListingType, Partner, Category, PickupLocation } from '../../../core/models/types';

@Component({
  selector: 'app-partner-submit-listing',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './partner-submit-listing.component.html'
})
export class PartnerSubmitListingComponent implements OnInit {
  @Output() submitted = new EventEmitter<void>();

  partnerApi = inject(PartnerService);
  api = inject(ApiService);

  readonly Package = Package;
  readonly Upload = Upload;
  readonly ImageIcon = ImageIcon;
  readonly ChevronDown = ChevronDown;
  readonly X = X;
  readonly Plus = Plus;

  readonly ListingType = ListingType;

  loading = false;
  saving = false;
  uploadingCover = false;
  uploadingGallery = false;
  error: string | null = null;
  success: string | null = null;

  partners: Partner[] = [];
  categories: Category[] = [];
  pickupLocations: PickupLocation[] = [];

  partnerId = '';
  title = '';
  category = '';
  type: ListingType = ListingType.GIVE;
  description = '';
  hourlyRate = 0;
  imageUrl = '';
  gallery: string[] = [];

  pickupOption: 'concierge' | 'bakery' | 'public' | 'custom' = 'concierge';
  pickupLocationId: string | null = null;
  pickupLocationStreet = '';
  pickupLocationHouseNumber = '';
  pickupLocationCity = '';
  pickupLocationZip = '';
  pickupLocationCustom = '';

  ngOnInit() {
    setTimeout(() => {
      void this.init();
    }, 0);
  }

  private async withTimeout<T>(p: Promise<T>, ms: number): Promise<T> {
    return Promise.race([
      p,
      new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
    ]);
  }

  private async init() {
    this.error = null;
    this.success = null;
    try {
      const [partnersRes, categoriesRes, pickupsRes] = await Promise.allSettled([
        this.withTimeout(this.partnerApi.getMyPartners(), 12000),
        this.withTimeout(this.api.getCategories().catch(() => []), 12000),
        this.withTimeout(this.api.getPickupLocations().catch(() => []), 12000)
      ]);

      if (partnersRes.status === 'fulfilled') {
        this.partners = Array.isArray(partnersRes.value) ? partnersRes.value : [];
      } else {
        this.partners = [];
        this.error = (partnersRes.reason as any)?.message || 'failed_to_load';
      }

      if (categoriesRes.status === 'fulfilled') {
        this.categories = Array.isArray(categoriesRes.value) ? categoriesRes.value : [];
      } else {
        this.categories = [];
        if (!this.error) this.error = (categoriesRes.reason as any)?.message || 'failed_to_load';
      }

      if (pickupsRes.status === 'fulfilled') {
        this.pickupLocations = Array.isArray(pickupsRes.value) ? pickupsRes.value : [];
      } else {
        this.pickupLocations = [];
        if (!this.error) this.error = (pickupsRes.reason as any)?.message || 'failed_to_load';
      }

      if (!this.partnerId && this.partners.length === 1) {
        this.partnerId = String(this.partners[0].id || '');
      }
      if (!this.category) {
        this.category = String(this.categories[0]?.name || 'Tools');
      }
      if (!this.pickupLocationId && this.pickupLocations.length > 0) {
        this.selectPickupOption(this.pickupOption);
      }
    } catch (e: any) {
      this.error = e?.message || 'failed_to_load';
      this.partners = [];
    } finally {
      this.loading = false;
    }
  }

  get pickupConcierge(): PickupLocation | null {
    return this.findPickupByKeyword('concierge');
  }

  get pickupBakery(): PickupLocation | null {
    return this.findPickupByKeyword('bakery');
  }

  get pickupPublic(): PickupLocation | null {
    return this.findPickupByKeyword('public');
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
    if (opt === 'custom') {
      this.pickupLocationId = null;
    } else if (selected) {
      this.pickupLocationId = String(selected.id);
    } else if (this.pickupLocations.length > 0) {
      this.pickupLocationId = String(this.pickupLocations[0]?.id || '');
    } else {
      this.pickupLocationId = null;
    }
    if (opt !== 'custom') {
      this.pickupLocationStreet = '';
      this.pickupLocationHouseNumber = '';
      this.pickupLocationCity = '';
      this.pickupLocationZip = '';
      this.pickupLocationCustom = '';
    }
  }

  onTypeSelect(type: ListingType) {
    if (type !== ListingType.GIVE && type !== ListingType.LEND) return;
    this.type = type;
    if (type === ListingType.GIVE) {
      this.hourlyRate = 0;
    }
  }

  async onCoverSelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadingCover = true;
    this.error = null;
    this.success = null;
    const localUrl = URL.createObjectURL(file);
    this.imageUrl = localUrl;
    try {
      const url = await this.api.uploadListingImage(file);
      if (url) this.imageUrl = url;
    } catch (e: any) {
      this.error = e?.message || 'upload_failed';
    } finally {
      this.uploadingCover = false;
      input.value = '';
    }
  }

  async onGallerySelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    if (files.length === 0) return;
    this.uploadingGallery = true;
    this.error = null;
    this.success = null;
    try {
      for (const file of files) {
        const localUrl = URL.createObjectURL(file);
        this.gallery = [...this.gallery, localUrl];
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
    } finally {
      this.uploadingGallery = false;
      input.value = '';
    }
  }

  removeGalleryImage(i: number) {
    this.gallery = this.gallery.filter((_, idx) => idx !== i);
  }

  async submit() {
    this.error = null;
    this.success = null;

    if (!this.partnerId) {
      this.error = 'partner_required';
      return;
    }
    if (!String(this.title || '').trim() || !String(this.category || '').trim() || !this.type) {
      this.error = 'required_fields';
      return;
    }
    if (!String(this.imageUrl || '').trim()) {
      this.error = 'cover_required';
      return;
    }
    if (this.pickupOption !== 'custom' && !this.pickupLocationId) {
      this.error = 'pickup_point_required';
      return;
    }
    if (this.pickupOption === 'custom') {
      const street = String(this.pickupLocationStreet || '').trim();
      const house = String(this.pickupLocationHouseNumber || '').trim();
      const city = String(this.pickupLocationCity || '').trim();
      const zip = String(this.pickupLocationZip || '').trim();
      const zipOk = zip.length >= 3 && zip.length <= 20 && /[0-9]/.test(zip);
      if (street.length < 2 || house.length < 1 || city.length < 2 || !zipOk) {
        this.error = 'pickup_location_required';
        return;
      }
    }

    this.saving = true;
    try {
      const payload: any = {
        partnerId: this.partnerId,
        title: this.title.trim(),
        description: this.description ?? '',
        category: this.category,
        type: this.type,
        hourlyRate: this.type === ListingType.GIVE ? 0 : Number(this.hourlyRate || 0),
        imageUrl: this.imageUrl,
        gallery: this.gallery,
        autoApprove: false,
        insuranceRequired: false,
        x: 0,
        y: 0,
        pickupLocationId: this.pickupOption === 'custom' ? null : this.pickupLocationId,
        pickupLocationStreet: this.pickupOption === 'custom' ? (this.pickupLocationStreet || null) : null,
        pickupLocationHouseNumber: this.pickupOption === 'custom' ? (this.pickupLocationHouseNumber || null) : null,
        pickupLocationCity: this.pickupOption === 'custom' ? (this.pickupLocationCity || null) : null,
        pickupLocationZip: this.pickupOption === 'custom' ? (this.pickupLocationZip || null) : null,
        pickupLocationCustom: this.pickupOption === 'custom' ? (this.pickupLocationCustom || null) : null
      };
      await this.partnerApi.addListing(payload);
      this.success = 'submitted_for_review';
      this.title = '';
      this.description = '';
      this.hourlyRate = 0;
      this.imageUrl = '';
      this.gallery = [];
      this.pickupLocationStreet = '';
      this.pickupLocationHouseNumber = '';
      this.pickupLocationCity = '';
      this.pickupLocationZip = '';
      this.pickupLocationCustom = '';
      if (this.categories.length > 0) {
        this.category = String(this.categories[0]?.name || this.category);
      }
      this.selectPickupOption(this.pickupOption);
      this.submitted.emit();
    } catch (e: any) {
      this.error = e?.message || 'save_failed';
    } finally {
      this.saving = false;
    }
  }
}
