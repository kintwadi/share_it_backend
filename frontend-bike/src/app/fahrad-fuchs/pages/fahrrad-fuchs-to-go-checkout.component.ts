import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { AuthService } from '../../core/services/auth.service';
import { FahrradFuchsToGoApiService } from '../fahrrad-fuchs-to-go-api.service';
import {
  extractManufacturer,
  resolveBookingStatusKey,
  resolveBrandTheme,
  resolveReadinessKey
} from '../fahrrad-fuchs-to-go-branding';
import { FahrradFuchsToGoCheckoutResponse, FahrradFuchsToGoListingDetail } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe, TPipe],
  templateUrl: './fahrrad-fuchs-to-go-checkout.component.html',
  styleUrl: './fahrrad-fuchs-to-go-checkout.component.css'
})
export class FahrradFuchsToGoCheckoutComponent implements OnInit {
  readonly trialDepositAmount = 100;
  readonly trialWindowDays = 2;

  private readonly api = inject(FahrradFuchsToGoApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  bike: FahrradFuchsToGoListingDetail | null = null;
  confirmation: FahrradFuchsToGoCheckoutResponse | null = null;
  loading = true;
  submitting = false;
  errorKey = '';

  startDate = '';
  endDate = '';
  frameSizeOption = '';

  readonly signedInUser = computed(() => this.auth.user());

  get manufacturer(): string {
    return extractManufacturer(this.bike?.title);
  }

  get readinessKey(): string {
    return resolveReadinessKey(this.bike?.category);
  }

  get confirmationStatusKey(): string {
    return resolveBookingStatusKey(this.confirmation?.status);
  }

  get theme() {
    return resolveBrandTheme(this.bike?.title);
  }

  get remainingPurchaseBalance(): number {
    return Math.max((this.bike?.retailPrice || 0) - this.trialDepositAmount, 0);
  }

  async ngOnInit(): Promise<void> {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.errorKey = 'errors.checkoutItemNotFound';
      this.loading = false;
      this.render();
      return;
    }

    try {
      this.bike = await this.api.getBike(slug);
      this.startDate = this.route.snapshot.queryParamMap.get('startDate') || this.formatDateOffset(1);
      this.endDate = this.route.snapshot.queryParamMap.get('endDate') || this.formatDateOffset(2);
      this.frameSizeOption = this.route.snapshot.queryParamMap.get('frameSize') || this.bike.frameOptions[0]?.value || '';
    } catch {
      this.errorKey = 'errors.checkoutItemLoad';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async reserve(): Promise<void> {
    if (!this.bike) {
      return;
    }

    this.submitting = true;
    this.errorKey = '';

    try {
      this.confirmation = await this.api.checkout(this.bike.slug, {
        startDate: this.startDate,
        endDate: this.endDate,
        frameSizeOption: this.frameSizeOption,
        paymentMethod: 'PAYPAL',
        paymentToken: `fahrrad-fuchs-to-go-demo-${Date.now()}`
      });
    } catch {
      this.confirmation = null;
      this.errorKey = 'errors.checkoutFailed';
    } finally {
      this.submitting = false;
      this.render();
    }
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
