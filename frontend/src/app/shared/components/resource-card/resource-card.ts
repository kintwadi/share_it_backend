import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Heart, BadgeCheck, ShieldCheck, MapPin, Gift } from 'lucide-angular';
import { Listing, ListingType, AvailabilityStatus } from '../../../core/models/types';
import { I18nService } from '../../../core/services/i18n.service';
import { UserPreferencesService } from '../../../core/services/user-preferences.service';

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
    return this.listing?.status === AvailabilityStatus.AVAILABLE;
  }

  get isFree(): boolean {
    return !this.listing?.hourlyRate || this.listing.hourlyRate === 0;
  }

  onImageError(event: Event) {
    if (this.listing?.owner?.id) {
      (event.target as HTMLImageElement).src = `https://picsum.photos/seed/${this.listing.owner.id}/80/80`;
    }
  }
}
