import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule, Upload } from 'lucide-angular';
import { ApiService } from '../../../core/services/api.service';
import { PartnerService } from '../../../core/services/partner.service';
import { ListingType, Partner } from '../../../core/models/types';

@Component({
  selector: 'app-partner-submit-listing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './partner-submit-listing.component.html'
})
export class PartnerSubmitListingComponent implements OnInit {
  @Output() submitted = new EventEmitter<void>();

  fb = inject(FormBuilder);
  partnerApi = inject(PartnerService);
  api = inject(ApiService);

  readonly Upload = Upload;
  readonly ListingType = ListingType;

  partners: Partner[] = [];
  loading = true;
  saving = false;
  error = '';
  success = '';

  form = this.fb.group({
    partnerId: ['', Validators.required],
    title: ['', Validators.required],
    description: ['', Validators.required],
    category: ['', Validators.required],
    type: [ListingType.GOODS, Validators.required],
    hourlyRate: [0, [Validators.min(0)]],
    imageUrl: [''],
    insuranceRequired: [false],
    pickupLocationCity: [''],
    pickupLocationZip: [''],
    pickupLocationStreet: [''],
    pickupLocationHouseNumber: [''],
    pickupLocationCustom: ['']
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
    this.success = '';
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
    this.error = '';
    this.success = '';
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
    this.success = '';
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
        autoApprove: false,
        insuranceRequired: !!v.insuranceRequired,
        x: 0,
        y: 0,
        pickupLocationCity: v.pickupLocationCity,
        pickupLocationZip: v.pickupLocationZip,
        pickupLocationStreet: v.pickupLocationStreet,
        pickupLocationHouseNumber: v.pickupLocationHouseNumber,
        pickupLocationCustom: v.pickupLocationCustom
      };
      await this.partnerApi.addListing(payload);
      this.success = 'submitted_for_review';
      this.form.patchValue({
        title: '',
        description: '',
        category: '',
        type: ListingType.GOODS,
        hourlyRate: 0,
        imageUrl: '',
        insuranceRequired: false,
        pickupLocationCity: '',
        pickupLocationZip: '',
        pickupLocationStreet: '',
        pickupLocationHouseNumber: '',
        pickupLocationCustom: ''
      });
      this.submitted.emit();
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

