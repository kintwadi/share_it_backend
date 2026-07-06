import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { AuthService } from '../../core/services/auth.service';
import { FahrradFuchsToGoApiService } from '../fahrrad-fuchs-to-go-api.service';
import {
  extractManufacturer,
  extractModelLine,
  resolveBrandTheme,
  resolveReadinessKey
} from '../fahrrad-fuchs-to-go-branding';
import { FahrradFuchsToGoListingDetail } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-bike-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe, TPipe],
  templateUrl: './fahrrad-fuchs-to-go-bike-detail.component.html',
  styleUrl: './fahrrad-fuchs-to-go-bike-detail.component.css'
})
export class FahrradFuchsToGoBikeDetailComponent implements OnInit {
  readonly trialDepositAmount = 100;
  readonly trialWindowDays = 2;

  private readonly api = inject(FahrradFuchsToGoApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  bike: FahrradFuchsToGoListingDetail | null = null;
  loading = true;
  errorKey = '';
  activeImageIndex = 0;
  showSpecs = false;
  selectedFrameSize = '';
  startDate = this.formatDateOffset(1);
  endDate = this.formatDateOffset(2);

  get manufacturer(): string {
    return extractManufacturer(this.bike?.title);
  }

  get modelLine(): string {
    return extractModelLine(this.bike?.title) || this.bike?.title || '';
  }

  get heroSpecs() {
    return (this.bike?.technicalSpecs || []).slice(0, 3);
  }

  get readinessKey(): string {
    return resolveReadinessKey(this.bike?.category);
  }

  get theme() {
    return resolveBrandTheme(this.bike?.title);
  }

  get requiresLoginForReservation(): boolean {
    return !this.auth.isAuthenticated();
  }

  get remainingPurchaseBalance(): number {
    return Math.max((this.bike?.retailPrice || 0) - this.trialDepositAmount, 0);
  }

  async ngOnInit(): Promise<void> {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.errorKey = 'errors.listingNotFound';
      this.loading = false;
      this.render();
      return;
    }

    try {
      this.bike = await this.api.getBike(slug);
      this.selectedFrameSize = this.bike.frameOptions[0]?.value || '';
    } catch {
      this.errorKey = 'errors.bikeLoad';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  selectImage(index: number): void {
    this.activeImageIndex = index;
  }

  toggleSpecs(): void {
    this.showSpecs = !this.showSpecs;
  }

  reservationLink(slug: string): string[] {
    return this.auth.isAuthenticated() ? ['/fahrad-fuchs/checkout', slug] : ['/login'];
  }

  reservationQueryParams(slug: string): Record<string, string> {
    if (this.auth.isAuthenticated()) {
      return {
        startDate: this.startDate,
        endDate: this.endDate,
        frameSize: this.selectedFrameSize
      };
    }

    const redirectParams = new URLSearchParams({
      startDate: this.startDate,
      endDate: this.endDate,
      frameSize: this.selectedFrameSize
    });

    return {
      redirect: `/fahrad-fuchs/checkout/${slug}?${redirectParams.toString()}`
    };
  }

  private formatDateOffset(offsetDays: number): string {
    const value = new Date();
    value.setDate(value.getDate() + offsetDays);
    return value.toISOString().slice(0, 10);
  }

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }
}
