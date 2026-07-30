import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, X, MapPin, Calendar, Clock, Gift, Package, User as UserIcon } from 'lucide-angular';
import { Listing, ListingType, User } from '../../../core/models/types';
import { I18nService } from '../../../core/services/i18n.service';
import { LayoutModeService } from '../../../core/services/layout-mode.service';

@Component({
  selector: 'app-transaction-overview-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './transaction-overview-modal.html',
  styleUrl: './transaction-overview-modal.css'
})
export class TransactionOverviewModalComponent {
  @Input() item: Listing | { listing: Listing; borrowedDate?: string; returnedDate?: string } | null = null;
  @Input() isOpen = false;
  @Input() currentUser: User | null = null;
  @Output() close = new EventEmitter<void>();

  readonly X = X;
  readonly MapPin = MapPin;
  readonly Calendar = Calendar;
  readonly Clock = Clock;
  readonly Gift = Gift;
  readonly Package = Package;
  readonly UserIcon = UserIcon;
  i18n = inject(I18nService);
  layoutMode = inject(LayoutModeService);

  get listing(): Listing | null {
    if (!this.item) return null;
    const anyItem: any = this.item as any;
    return anyItem.listing ? (anyItem.listing as Listing) : (this.item as Listing);
  }

  get isHistory(): boolean {
    return !!(this.item && (this.item as any).listing);
  }

  get isOwner(): boolean {
    const l = this.listing;
    if (!l || !this.currentUser) return false;
    return this.currentUser.id === l.ownerId;
  }

  get otherUser(): User | undefined {
    const l = this.listing;
    if (!l) return undefined;
    return this.isOwner ? l.borrower : l.owner;
  }

  get userLabel(): string {
    return this.isOwner ? 'Handed to' : 'Owner';
  }

  get dateObj(): Date {
    if (this.isHistory) {
      const raw = (this.item as any)?.borrowedDate;
      const d = raw ? new Date(raw) : new Date();
      return Number.isNaN(d.getTime()) ? new Date() : d;
    }
    return new Date();
  }

  get dateStr(): string {
    return this.dateObj.toLocaleDateString();
  }

  get timeStr(): string {
    return this.dateObj.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  get isGift(): boolean {
    return this.listing?.type === ListingType.GIVE;
  }

  get isSold(): boolean {
    return this.listing?.type === ListingType.SELL;
  }

  get statusLabel(): string {
    if (this.isGift) return this.i18n.t('modal.status_gifted');
    if (this.isSold) return this.i18n.t('modal.status_sold');
    return this.i18n.t('modal.status_borrowed');
  }

  get locationName(): string {
    return this.listing?.pickupLocation?.name || this.i18n.t('modal.standard_pickup');
  }

  get locationAddress(): string {
    const l = this.listing;
    if (!l) return '';
    return l.pickupLocation?.address || (l.location ? this.i18n.t('modal.location_shared_on_approval') : this.i18n.t('modal.contact_owner_for_location'));
  }

  onClose() {
    this.close.emit();
  }
}
