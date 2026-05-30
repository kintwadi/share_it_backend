import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, MapPin, Calendar, Clock, Gift, Package, User as UserIcon } from 'lucide-angular';
import { Listing, ListingType, User } from '../../core/models/types';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-transaction-overview',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './transaction-overview.component.html',
  styleUrl: './transaction-overview.component.css'
})
export class TransactionOverviewComponent {
  private router = inject(Router);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly MapPin = MapPin;
  readonly Calendar = Calendar;
  readonly Clock = Clock;
  readonly Gift = Gift;
  readonly Package = Package;
  readonly UserIcon = UserIcon;

  item: Listing | { listing: Listing; borrowedDate?: string; returnedDate?: string } | null =
    (history.state && (history.state as any).item) ? ((history.state as any).item as any) : null;
  currentUser: User | null = (history.state && (history.state as any).currentUser) ? ((history.state as any).currentUser as User) : null;
  returnTo: string = (history.state && typeof (history.state as any).returnTo === 'string') ? String((history.state as any).returnTo) : '/dashboard';

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
    return this.isOwner ? (l as any).borrower : (l as any).owner;
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
    return (this.listing as any)?.pickupLocation?.name || this.i18n.t('modal.standard_pickup');
  }

  get locationAddress(): string {
    const l: any = this.listing as any;
    if (!l) return '';
    return l.pickupLocation?.address || (l.location ? this.i18n.t('modal.location_shared_on_approval') : this.i18n.t('modal.contact_owner_for_location'));
  }

  back() {
    this.router.navigateByUrl(this.returnTo);
  }
}

