import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Package, Upload, Image as ImageIcon, Loader2, ChevronDown, X, Plus } from 'lucide-angular';
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
  readonly Loader2 = Loader2;
  readonly ChevronDown = ChevronDown;
  readonly X = X;
  readonly Plus = Plus;

  readonly ListingType = ListingType;

  loading = true;
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
  type: ListingType = ListingType.GOODS;
  description = '';
  hourlyRate = 0;
  imageUrl = '';
  gallery: string[] = [];
  insuranceRequired = false;

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

  private async init() {
    this.loading = true;
    this.error = null;
    this.success = null;
    try {
      const [partners, categories, pickups] = await Promise.all([
        this.partnerApi.getMyPartners(),
        this.api.getCategories().catch(() => []),
        this.api.getPickupLocations().catch(() => [])
      ]);
      this.partners = Array.isArray(partners) ? partners : [];
      this.categories = Array.isArray(categories) ? categories : [];
      this.pickupLocations = Array.isArray(pickups) ? pickups : [];

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
      setTimeout(() => {
        this.loading = false;
      }, 0);
    }
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
      this.pickupLocationCustom = '';
    }
  }

  onTypeSelect(type: ListingType) {
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
        insuranceRequired: !!this.insuranceRequired,
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
      this.insuranceRequired = false;
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
