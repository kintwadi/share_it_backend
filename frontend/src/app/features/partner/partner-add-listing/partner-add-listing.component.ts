import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Upload } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { ApiService } from '../../../core/services/api.service';
import { ListingType, Partner } from '../../../core/models/types';

@Component({
  selector: 'app-partner-add-listing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  templateUrl: './partner-add-listing.component.html',
  styleUrl: './partner-add-listing.component.css'
})
export class PartnerAddListingComponent implements OnInit {
  fb = inject(FormBuilder);
  router = inject(Router);
  partnerApi = inject(PartnerService);
  api = inject(ApiService);

  readonly ArrowLeft = ArrowLeft;
  readonly Upload = Upload;
  readonly ListingType = ListingType;

  partners: Partner[] = [];
  loading = true;
  saving = false;
  error = '';

  form = this.fb.group({
    partnerId: ['', Validators.required],
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

  private async withTimeout<T>(p: Promise<T>, ms: number): Promise<T> {
    return Promise.race([
      p,
      new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
    ]);
  }

  ngOnInit() {
    setTimeout(() => {
      void this.loadPartners();
    }, 0);
    this.applyHourlyRateRules(this.form.get('type')?.value);
    this.form.get('type')?.valueChanges.subscribe(t => this.applyHourlyRateRules(t));
  }

  async loadPartners() {
    this.loading = true;
    this.error = '';
    try {
      const list = await this.withTimeout(this.partnerApi.getMyPartners(), 12000);
      this.partners = Array.isArray(list) ? list : [];
      if (this.partners.length === 1) {
        this.form.patchValue({ partnerId: this.partners[0].id });
      }
    } catch (e: any) {
      const msg = String(e?.message || 'failed_to_load');
      this.error = msg === 'timeout' ? 'backend_timeout' : msg;
      this.partners = [];
    } finally {
      setTimeout(() => {
        this.loading = false;
      }, 0);
    }
  }

  async onUpload(file: File | null) {
    if (!file) return;
    try {
      const url = await this.api.uploadListingImage(file);
      this.form.patchValue({ imageUrl: url });
    } catch (e: any) {
      this.error = e?.message || 'upload_failed';
    }
  }

  async submit() {
    if (this.form.invalid) return;
    this.saving = true;
    this.error = '';
    try {
      const v = this.form.getRawValue();
      const payload: any = {
        partnerId: v.partnerId,
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
      await this.partnerApi.addListing(payload);
      this.router.navigate(['/partner/dashboard']);
    } catch (e: any) {
      this.error = e?.message || 'save_failed';
    } finally {
      this.saving = false;
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
