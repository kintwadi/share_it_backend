import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { FahrradFuchsToGoApiService } from '../fahrrad-fuchs-to-go-api.service';
import {
  extractManufacturer,
  resolveBookingStatusKey,
  resolveBrandTheme
} from '../fahrrad-fuchs-to-go-branding';
import { FahrradFuchsToGoBooking, FahrradFuchsToGoStore } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, TPipe],
  templateUrl: './fahrrad-fuchs-to-go-bookings.component.html',
  styleUrl: './fahrrad-fuchs-to-go-bookings.component.css'
})
export class FahrradFuchsToGoBookingsComponent implements OnInit {
  private readonly api = inject(FahrradFuchsToGoApiService);
  private readonly cdr = inject(ChangeDetectorRef);

  bookings: FahrradFuchsToGoBooking[] = [];
  store: FahrradFuchsToGoStore | null = null;
  loading = true;
  errorKey = '';

  manufacturer(booking: FahrradFuchsToGoBooking): string {
    return extractManufacturer(booking.bikeTitle);
  }

  statusKey(booking: FahrradFuchsToGoBooking): string {
    return resolveBookingStatusKey(booking.status);
  }

  theme(booking: FahrradFuchsToGoBooking) {
    return resolveBrandTheme(booking.bikeTitle);
  }

  async ngOnInit(): Promise<void> {
    try {
      const storefront = await this.api.getStorefront();
      this.store = storefront.store;
      this.bookings = await this.api.getBookings();
    } catch {
      this.errorKey = 'errors.bookingsLoad';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }
}
