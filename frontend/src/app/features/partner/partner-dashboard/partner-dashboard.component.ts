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

  async ngOnInit() {
    this.loading = true;
    this.error = '';
    try {
      this.partners = await this.partnerApi.getMyPartners();
      this.listings = await this.partnerApi.getListings();
    } catch (e: any) {
      this.error = e?.message || 'failed_to_load';
    } finally {
      this.loading = false;
    }
  }

  goAdd() {
    this.router.navigate(['/partner/listings/add']);
  }
}
