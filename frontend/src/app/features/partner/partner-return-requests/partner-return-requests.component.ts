import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, RefreshCw, Check, X } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { PartnerReturnRequest } from '../../../core/models/types';

@Component({
  selector: 'app-partner-return-requests',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './partner-return-requests.component.html'
})
export class PartnerReturnRequestsComponent implements OnInit {
  partnerApi = inject(PartnerService);

  readonly RefreshCw = RefreshCw;
  readonly Check = Check;
  readonly X = X;

  loading = true;
  error = '';
  rows: PartnerReturnRequest[] = [];
  busyIds = new Set<string>();

  async ngOnInit() {
    await this.load();
  }

  async load() {
    this.loading = true;
    this.error = '';
    try {
      this.rows = await this.partnerApi.getPendingManualReturns();
    } catch (e: any) {
      this.rows = [];
      this.error = e?.message || 'failed_to_load';
    } finally {
      this.loading = false;
    }
  }

  async accept(listingId: string) {
    if (this.busyIds.has(listingId)) return;
    this.busyIds.add(listingId);
    try {
      await this.partnerApi.acceptManualReturn(listingId);
      await this.load();
    } catch (e: any) {
      this.error = e?.message || 'accept_failed';
    } finally {
      this.busyIds.delete(listingId);
    }
  }

  async deny(listingId: string) {
    if (this.busyIds.has(listingId)) return;
    this.busyIds.add(listingId);
    try {
      await this.partnerApi.denyManualReturn(listingId, 'manual_return_denied_by_partner');
      await this.load();
    } catch (e: any) {
      this.error = e?.message || 'deny_failed';
    } finally {
      this.busyIds.delete(listingId);
    }
  }
}

