import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Check, X } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { PartnerBorrowRequest } from '../../../core/models/types';

@Component({
  selector: 'app-partner-requests',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './partner-requests.component.html',
  styleUrl: './partner-requests.component.css'
})
export class PartnerRequestsComponent implements OnInit {
  router = inject(Router);
  route = inject(ActivatedRoute);
  partnerApi = inject(PartnerService);

  readonly ArrowLeft = ArrowLeft;
  readonly Check = Check;
  readonly X = X;

  loading = true;
  error = '';
  requests: PartnerBorrowRequest[] = [];
  busyIds = new Set<string>();
  view: 'requests' | 'history' = 'requests';

  async ngOnInit() {
    const v = this.route.snapshot.queryParamMap.get('view');
    this.view = v === 'history' ? 'history' : 'requests';
    if (this.view === 'requests') {
      await this.load();
    } else {
      this.loading = false;
    }
  }

  async load() {
    this.loading = true;
    this.error = '';
    try {
      this.requests = await this.partnerApi.getRequests();
    } catch (e: any) {
      this.error = e?.message || 'failed_to_load';
    } finally {
      this.loading = false;
    }
  }

  async approve(id: string) {
    if (this.view !== 'requests') return;
    if (this.busyIds.has(id)) return;
    this.busyIds.add(id);
    try {
      await this.partnerApi.approveRequest(id);
      await this.load();
    } catch (e: any) {
      this.error = e?.message || 'approve_failed';
    } finally {
      this.busyIds.delete(id);
    }
  }

  async reject(id: string) {
    if (this.view !== 'requests') return;
    if (this.busyIds.has(id)) return;
    this.busyIds.add(id);
    try {
      await this.partnerApi.rejectRequest(id);
      await this.load();
    } catch (e: any) {
      this.error = e?.message || 'reject_failed';
    } finally {
      this.busyIds.delete(id);
    }
  }
}
