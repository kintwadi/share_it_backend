import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Save } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { Partner, PartnerSettings } from '../../../core/models/types';

@Component({
  selector: 'app-partner-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  templateUrl: './partner-settings.component.html',
  styleUrl: './partner-settings.component.css'
})
export class PartnerSettingsComponent implements OnInit {
  fb = inject(FormBuilder);
  router = inject(Router);
  partnerApi = inject(PartnerService);

  readonly ArrowLeft = ArrowLeft;
  readonly Save = Save;

  partners: Partner[] = [];
  loading = true;
  saving = false;
  error = '';

  form = this.fb.group({
    partnerId: [''],
    maxLendingDays: [14],
    depositCents: [0],
    autoApproval: [false],
  });

  async ngOnInit() {
    this.loading = true;
    try {
      this.partners = await this.partnerApi.getMyPartners();
      if (this.partners.length === 1) {
        this.form.patchValue({ partnerId: this.partners[0].id });
      }
      await this.loadSettings();
    } catch (e: any) {
      this.error = e?.message || 'failed_to_load';
    } finally {
      this.loading = false;
    }
  }

  async loadSettings() {
    const pid = String(this.form.get('partnerId')?.value || '');
    if (!pid) return;
    const s = await this.partnerApi.getSettings(pid);
    this.form.patchValue({
      maxLendingDays: s.maxLendingDays ?? 14,
      depositCents: s.depositCents ?? 0,
      autoApproval: !!s.autoApproval,
    });
  }

  async submit() {
    const pid = String(this.form.get('partnerId')?.value || '');
    if (!pid) {
      this.error = 'partner_id_required';
      return;
    }
    this.saving = true;
    this.error = '';
    try {
      const v = this.form.getRawValue();
      const payload: PartnerSettings = {
        partnerId: pid,
        maxLendingDays: Number(v.maxLendingDays ?? 14),
        depositCents: Number(v.depositCents ?? 0),
        autoApproval: !!v.autoApproval,
      };
      await this.partnerApi.updateSettings(payload);
      this.router.navigate(['/partner/dashboard']);
    } catch (e: any) {
      this.error = e?.message || 'save_failed';
    } finally {
      this.saving = false;
    }
  }
}
