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

  ngOnInit() {
    setTimeout(() => {
      void this.reload();
    }, 0);
  }

  async reload() {
    this.loading = true;
    this.error = '';
    let nextPartners: Partner[] = [];
    let nextListings: Listing[] = [];
    let nextError = '';
    try {
      const [partnersRes, listingsRes] = await Promise.allSettled([
        this.withTimeout(this.partnerApi.getMyPartners(), 12000),
        this.withTimeout(this.partnerApi.getListings(), 12000)
      ]);
      if (partnersRes.status === 'fulfilled') {
        nextPartners = Array.isArray(partnersRes.value) ? partnersRes.value : [];
      } else {
        nextPartners = [];
        nextError = (partnersRes.reason as any)?.message || 'failed_to_load';
      }
      if (listingsRes.status === 'fulfilled') {
        nextListings = Array.isArray(listingsRes.value) ? listingsRes.value : [];
      } else if (!nextError) {
        nextListings = [];
        nextError = (listingsRes.reason as any)?.message || 'failed_to_load';
      }
    } catch (e: any) {
      nextError = e?.message || 'failed_to_load';
    }
    if (nextError === 'timeout') {
      nextError = 'backend_timeout';
    }
    setTimeout(() => {
      this.partners = nextPartners;
      this.listings = nextListings;
      this.error = nextError;
      this.loading = false;
    }, 0);
  }

  goAdd() {
    this.router.navigate(['/partner/listings/add']);
  }
}
