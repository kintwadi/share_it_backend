import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Heart, BadgeCheck, ShieldCheck, MapPin, Gift } from 'lucide-angular';
import { Listing, ListingType, AvailabilityStatus } from '../../../core/models/types';
import { I18nService } from '../../../core/services/i18n.service';
import { UserPreferencesService } from '../../../core/services/user-preferences.service';
import { getListingPrimaryRate, getListingPriceSuffix, isListingFree } from '../../../core/utils/listing-pricing';

@Component({
  selector: 'app-resource-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './resource-card.html',
  styleUrl: './resource-card.css'
})
export class ResourceCardComponent {
  @Input() listing!: Listing;
  @Output() cardClick = new EventEmitter<void>();

  i18n = inject(I18nService);
  prefs = inject(UserPreferencesService);

  readonly Heart = Heart;
  readonly BadgeCheck = BadgeCheck;
  readonly ShieldCheck = ShieldCheck;
  readonly MapPin = MapPin;
  readonly Gift = Gift;

  get isSkill(): boolean {
    return this.listing?.type === ListingType.SKILL;
  }

  get isAvailable(): boolean {
    const l = this.listing;
    if (!l) return false;
    if (l.status === AvailabilityStatus.AVAILABLE) return true;
    if (l.partnerId && l.status === AvailabilityStatus.PARTNER_ACTIVE && !l.borrowerId) return true;
    return false;
  }

  get isFree(): boolean {
    return isListingFree(this.listing);
  }

  get priceLabel(): string {
    if (this.isFree) return this.i18n.t('listing.free');
    return `${this.i18n.formatPrice(getListingPrimaryRate(this.listing))}${getListingPriceSuffix(this.listing)}`;
  }

  get isPartner(): boolean {
    return !!this.listing?.partnerId;
  }

  get displayOwnerName(): string {
    return this.isPartner ? (this.listing.partnerName || 'Partner') : (this.listing.owner?.name || '');
  }

  get displayAvatarSeed(): string {
    return String(this.isPartner ? (this.listing.partnerId || 'partner') : (this.listing.owner?.id || 'user'));
  }

  onImageError(event: Event) {
    (event.target as HTMLImageElement).src = `https://picsum.photos/seed/${this.displayAvatarSeed}/80/80`;
  }
}
