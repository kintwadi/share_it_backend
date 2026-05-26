import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Listing, AvailabilityStatus } from '../../core/models/types';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-enterprise-listing-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './enterprise-listing-card.component.html',
  styleUrl: './enterprise-listing-card.component.css'
})
export class EnterpriseListingCardComponent {
  @Input() listing!: Listing;
  @Output() cardClick = new EventEmitter<void>();

  i18n = inject(I18nService);

  get isAvailable(): boolean {
    const l = this.listing;
    if (!l) return false;
    if (l.status === AvailabilityStatus.AVAILABLE) return true;
    if (l.partnerId && l.status === AvailabilityStatus.PARTNER_ACTIVE && !l.borrowerId) return true;
    return false;
  }

  get rateLabel(): string {
    const r = Number(this.listing?.hourlyRate || 0);
    if (!r) return '-';
    return this.i18n.formatPrice(r);
  }

  onImgError(e: Event) {
    (e.target as HTMLImageElement).src = `https://picsum.photos/seed/${this.listing?.id || 'enterprise'}/320/240`;
  }
}

