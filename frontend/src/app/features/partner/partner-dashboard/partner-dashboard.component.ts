import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule, Plus, Settings, Inbox, Building2, History } from 'lucide-angular';
import { PartnerService } from '../../../core/services/partner.service';
import { Listing, Partner } from '../../../core/models/types';
import { ResourceCardComponent } from '../../../shared/components/resource-card/resource-card';

@Component({
  selector: 'app-partner-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, ResourceCardComponent],
  templateUrl: './partner-dashboard.component.html',
  styleUrl: './partner-dashboard.component.css'
})
export class PartnerDashboardComponent implements OnInit {
  partnerApi = inject(PartnerService);
  router = inject(Router);

  readonly Plus = Plus;
  readonly Settings = Settings;
  readonly Inbox = Inbox;
  readonly Building2 = Building2;
  readonly History = History;

  partners: Partner[] = [];
  listings: Listing[] = [];
  loading = true;
  error = '';

  private async withTimeout<T>(p: Promise<T>, ms: number): Promise<T> {
    return Promise.race([
      p,
      new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
    ]);
  }

  async ngOnInit() {
    await this.reload();
  }

  async reload() {
    this.loading = true;
    this.error = '';
    try {
      const [partnersRes, listingsRes] = await Promise.allSettled([
        this.withTimeout(this.partnerApi.getMyPartners(), 12000),
        this.withTimeout(this.partnerApi.getListings(), 12000)
      ]);
      if (partnersRes.status === 'fulfilled') {
        this.partners = Array.isArray(partnersRes.value) ? partnersRes.value : [];
      } else {
        this.partners = [];
        this.error = (partnersRes.reason as any)?.message || 'failed_to_load';
      }
      if (listingsRes.status === 'fulfilled') {
        this.listings = Array.isArray(listingsRes.value) ? listingsRes.value : [];
      } else if (!this.error) {
        this.listings = [];
        this.error = (listingsRes.reason as any)?.message || 'failed_to_load';
      }
      if (this.error === 'timeout') {
        this.error = 'backend_timeout';
      }
    } finally {
      this.loading = false;
    }
  }

  goAdd() {
    this.router.navigate(['/partner/listings/add']);
  }
}
