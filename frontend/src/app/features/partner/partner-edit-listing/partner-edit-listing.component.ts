import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule, ArrowLeft, Trash2, Save } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { Listing, ListingType } from '../../../core/models/types';

@Component({
  selector: 'app-partner-edit-listing',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './partner-edit-listing.component.html',
  styleUrl: './partner-edit-listing.component.css'
})
export class PartnerEditListingComponent implements OnInit {
  route = inject(ActivatedRoute);
  router = inject(Router);
  fb = inject(FormBuilder);
  partnerApi = inject(PartnerService);

  readonly ArrowLeft = ArrowLeft;
  readonly Trash2 = Trash2;
  readonly Save = Save;
  readonly ListingType = ListingType;

  listing: Listing | null = null;
  loading = true;
  saving = false;
  deleting = false;
  error = '';

  form = this.fb.group({
    title: ['', Validators.required],
    description: ['', Validators.required],
    category: ['', Validators.required],
    type: [ListingType.GOODS, Validators.required],
    hourlyRate: [0, [Validators.min(0)]],
    imageUrl: [''],
    autoApprove: [false],
    insuranceRequired: [false],
    x: [0],
    y: [0],
    pickupLocationCity: [''],
    pickupLocationZip: [''],
    pickupLocationStreet: [''],
    pickupLocationHouseNumber: [''],
    pickupLocationCustom: [''],
  });

  async ngOnInit() {
    this.loading = true;
    this.error = '';
    try {
      const id = String(this.route.snapshot.paramMap.get('id') || '');
      const all = await this.partnerApi.getListings();
      this.listing = all.find(l => String(l.id) === id) || null;
      if (!this.listing) {
        this.error = 'listing_not_found';
        return;
      }
      this.form.patchValue({
        title: this.listing.title,
        description: this.listing.description,
        category: this.listing.category,
        type: this.listing.type,
        hourlyRate: this.listing.hourlyRate ?? 0,
        imageUrl: this.listing.imageUrl || '',
        autoApprove: !!this.listing.autoApprove,
        insuranceRequired: !!this.listing.insuranceRequired,
        x: Number(this.listing.location?.x ?? 0),
        y: Number(this.listing.location?.y ?? 0),
        pickupLocationCity: this.listing.pickupLocationCity || '',
        pickupLocationZip: this.listing.pickupLocationZip || '',
        pickupLocationStreet: this.listing.pickupLocationStreet || '',
        pickupLocationHouseNumber: this.listing.pickupLocationHouseNumber || '',
        pickupLocationCustom: this.listing.pickupLocationCustom || '',
      });
      this.applyHourlyRateRules(this.form.get('type')?.value);
      this.form.get('type')?.valueChanges.subscribe(t => this.applyHourlyRateRules(t));
    } catch (e: any) {
      this.error = e?.message || 'failed_to_load';
    } finally {
      this.loading = false;
    }
  }

  async save() {
    if (!this.listing || this.form.invalid) return;
    this.saving = true;
    this.error = '';
    try {
      const v = this.form.getRawValue();
      const payload: any = {
        title: v.title,
        description: v.description,
        category: v.category,
        type: v.type,
        hourlyRate: Number(v.hourlyRate ?? 0),
        imageUrl: v.imageUrl,
        autoApprove: !!v.autoApprove,
        insuranceRequired: !!v.insuranceRequired,
        x: Number(v.x ?? 0),
        y: Number(v.y ?? 0),
        pickupLocationCity: v.pickupLocationCity,
        pickupLocationZip: v.pickupLocationZip,
        pickupLocationStreet: v.pickupLocationStreet,
        pickupLocationHouseNumber: v.pickupLocationHouseNumber,
        pickupLocationCustom: v.pickupLocationCustom,
      };
      await this.partnerApi.updateListing(this.listing.id, payload);
      this.router.navigate(['/partner/dashboard']);
    } catch (e: any) {
      this.error = e?.message || 'save_failed';
    } finally {
      this.saving = false;
    }
  }

  async remove() {
    if (!this.listing) return;
    this.deleting = true;
    this.error = '';
    try {
      await this.partnerApi.deleteListing(this.listing.id);
      this.router.navigate(['/partner/dashboard']);
    } catch (e: any) {
      this.error = e?.message || 'delete_failed';
    } finally {
      this.deleting = false;
    }
  }

  private applyHourlyRateRules(type: any) {
    const hourlyRate = this.form.get('hourlyRate');
    if (!hourlyRate) return;
    if (type === ListingType.GIVE) {
      hourlyRate.disable({ emitEvent: false });
      hourlyRate.setValue(0, { emitEvent: false });
      hourlyRate.clearValidators();
    } else {
      hourlyRate.enable({ emitEvent: false });
      hourlyRate.setValidators([Validators.min(0)]);
    }
    hourlyRate.updateValueAndValidity({ emitEvent: false });
  }
}
