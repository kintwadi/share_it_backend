import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PartnerListingSummary } from '../../core/models/commerce.models';
import { PartnerListingService } from '../../core/services/partner-listing.service';
import { MetricStripComponent } from '../../shared/components/metric-strip.component';

@Component({
  selector: 'app-list-bike',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, MetricStripComponent],
  templateUrl: './list-bike.component.html',
  styleUrl: './list-bike.component.css'
})
export class ListBikeComponent implements OnInit {
  private readonly listingsService = inject(PartnerListingService);

  form = {
    title: '',
    description: '',
    category: 'Bicycle',
    type: 'LEND' as 'LEND' | 'SELL',
    hourlyRate: 8,
    imageUrl: '',
    autoApprove: false,
    insuranceRequired: false,
    x: 52.52,
    y: 13.405,
    streetAddress: '',
    city: 'Berlin',
    postalCode: '',
    country: 'Germany',
    availableUnlimited: true
  };
  galleryText = '';
  listings: PartnerListingSummary[] = [];
  loading = false;
  listLoading = true;
  error = '';
  success = '';

  get overviewMetrics() {
    return [
      { value: String(this.listings.length), label: 'Total listings' },
      { value: this.form.city || 'Berlin', label: 'Current base city' }
    ];
  }

  async ngOnInit(): Promise<void> {
    await this.loadListings();
  }

  async loadListings(): Promise<void> {
    this.listLoading = true;
    try {
      this.listings = await this.listingsService.listMine();
    } catch {
      this.listings = [];
    } finally {
      this.listLoading = false;
    }
  }

  async submit(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';
    const gallery = this.galleryText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);

    try {
      await this.listingsService.create({
        ...this.form,
        gallery
      });
      this.success = 'Bike listing submitted successfully.';
      this.galleryText = '';
      this.form.title = '';
      this.form.description = '';
      this.form.imageUrl = '';
      await this.loadListings();
    } catch {
      this.error = 'Listing submission failed. Make sure this account is allowed to create partner listings.';
    } finally {
      this.loading = false;
    }
  }
}