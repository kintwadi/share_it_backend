import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Plus, Loader2, Package, Users, ShieldAlert, Search, Clock, MapPin, Calendar, Shield, Trash2, Edit2, Eye, EyeOff, X, Upload, Image as ImageIcon, BadgeCheck, ChevronRight, BellRing, Check, X as XIcon, MessageSquare, Zap, Ban, RefreshCcw, Mail, Phone, FileCheck, DollarSign, Gift, History, Star, AlertTriangle, CheckCircle2, Lock, Settings, Flag } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { User, Listing, AvailabilityStatus, ListingType, BorrowHistoryItem, VerificationStatus } from '../../core/models/types';
import { TransactionOverviewModalComponent } from '../../shared/components/transaction-overview-modal/transaction-overview-modal';
import { ReturnModalComponent } from '../../shared/components/return-modal/return-modal';
import { ReviewModalComponent } from '../../shared/components/review-modal/review-modal';
import { ConfirmationModalComponent } from '../../shared/components/confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, TransactionOverviewModalComponent, ReturnModalComponent, ReviewModalComponent, ConfirmationModalComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  api = inject(ApiService);
  i18n = inject(I18nService);
  settingsConfig = inject(SettingsConfigService);
  router = inject(Router);
  route = inject(ActivatedRoute);
  cdr = inject(ChangeDetectorRef);

  readonly Plus = Plus;
  readonly Loader2 = Loader2;
  readonly Package = Package;
  readonly Shield = Shield;
  readonly ShieldAlert = ShieldAlert;
  readonly Zap = Zap;
  readonly BadgeCheck = BadgeCheck;
  readonly Calendar = Calendar;
  readonly BellRing = BellRing;
  readonly Check = Check;
  readonly XIcon = XIcon;
  readonly MessageSquare = MessageSquare;
  readonly Clock = Clock;
  readonly History = History;
  readonly Gift = Gift;
  readonly DollarSign = DollarSign;
  readonly Edit2 = Edit2;
  readonly Trash2 = Trash2;
  readonly EyeOff = EyeOff;
  readonly Eye = Eye;
  readonly MapPin = MapPin;
  readonly ChevronRight = ChevronRight;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;
  readonly Ban = Ban;
  readonly FileCheck = FileCheck;
  readonly Flag = Flag;
  readonly X = X;

  readonly AvailabilityStatus = AvailabilityStatus;
  readonly ListingType = ListingType;
  readonly VerificationStatus = VerificationStatus;

  user: User | null = null;
  myListings: Listing[] = [];
  myBorrows: Listing[] = [];
  history: BorrowHistoryItem[] = [];
  recommendations: Listing[] = [];
  currentSub: { planType: string; status: string } | null = null;
  today = new Date();
  overviewItem: Listing | { listing: Listing; borrowedDate?: string; returnedDate?: string } | null = null;
  returningItem: Listing | null = null;
  reviewingItem: Listing | null = null;
  deleteTarget: Listing | null = null;
  deleteConfirmOpen = false;
  reportTarget: Listing | null = null;
  showReportModal = false;
  showReportSuccess = false;
  reportReason = '';
  reportDetails = '';
  reporting = false;
  reportError: string | null = null;
  subscriptionGateOpen = false;
  userPreviewOpen = false;
  userPreview: User | null = null;

  isLoadingListings = false;
  isLoadingBorrows = false;
  isLoadingHistory = false;
  isLoadingRecs = false;
  actionLoading: string | null = null;
  paymentSuccess = false;
  upgradeSuccess = false;
  error: string | null = null;
  private listingsIntervalId: any = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get deleteConfirmMessage(): string {
    const title = this.deleteTarget?.title || '';
    return `${this.i18n.t('dashboard.delete_listing_msg')} "${title}"? ${this.i18n.t('dashboard.delete_listing_cannot_undo')}`;
  }

  get returnsEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('returns', 'qr') ||
      this.settingsConfig.isSectionEnabled('returns', 'manual') ||
      this.settingsConfig.isSectionEnabled('returns', 'dispute');
  }

  ngOnInit() {
    this.settingsConfig.ensureLoaded();
    this.api.getCurrentUser().then(u => {
      if (!u) {
        this.router.navigate(['/connect']);
        return;
      }
      const role = String((u as any).role ?? '').toUpperCase();
      if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
        this.router.navigate(['/admin']);
        return;
      }
      this.user = u;
      this.render();
      this.upgradeSuccess = !!(history.state && (history.state as any).upgradeSuccess);
      if (this.upgradeSuccess) {
        setTimeout(() => {
          this.upgradeSuccess = false;
          this.render();
        }, 5000);
      }
      this.fetchListings();
      this.fetchHistory();
      this.api.getCurrentSubscription()
        .then(sub => {
          if (!sub) {
            this.currentSub = null;
            return;
          }
          this.currentSub = { planType: String((sub as any).planType || ''), status: String((sub as any).status || '') };
        })
        .catch(() => {
          this.currentSub = null;
        })
        .finally(() => this.render());

      this.listingsIntervalId = setInterval(() => this.fetchListings(true), 3000);

      this.route.queryParams.subscribe(async params => {
        const sessionId = String(params['session_id'] || '');
        if (sessionId) {
          this.paymentSuccess = true;
          this.render();
          try {
            await this.api.syncSubscriptionFromSession(sessionId);
          } catch { }
          try {
            const sub = await this.api.getCurrentSubscription();
            if (sub) {
              this.currentSub = { planType: String((sub as any).planType || ''), status: String((sub as any).status || '') };
            } else {
              this.currentSub = null;
            }
          } catch {
            this.currentSub = null;
          }
          try {
            this.router.navigate([], { relativeTo: this.route, queryParams: { session_id: null }, queryParamsHandling: 'merge', replaceUrl: true });
          } catch { }
          this.render();
          setTimeout(() => {
            this.paymentSuccess = false;
            this.render();
          }, 5000);
          return;
        }

        const action = String(params['action'] || '');
        if (action === 'profile') {
          this.router.navigate(['/settings'], { queryParams: { tab: 'profile' } });
          return;
        }
        if (action === 'security') {
          this.router.navigate(['/settings'], { queryParams: { tab: 'security' } });
          return;
        }
        if (action === 'payments') {
          this.router.navigate(['/settings'], { queryParams: { tab: 'payments' } });
          return;
        }
      });
    });
  }

  ngOnDestroy(): void {
    if (this.listingsIntervalId) {
      clearInterval(this.listingsIntervalId);
      this.listingsIntervalId = null;
    }
  }

  fetchListings(silent = false) {
    if (!this.user) return;
    if (!silent) {
      this.isLoadingListings = true;
      this.isLoadingBorrows = true;
      this.isLoadingRecs = true;
      this.render();
    }
    this.api.getListings().then(all => {
      this.myListings = all.filter(l => l.ownerId === this.user?.id);
      this.myBorrows = all.filter(l => l.borrowerId === this.user?.id);
      this.recommendations = all
        .filter(l => l.ownerId !== this.user?.id && l.status === AvailabilityStatus.AVAILABLE)
        .slice(0, 4);
      this.isLoadingListings = false;
      this.isLoadingBorrows = false;
      this.isLoadingRecs = false;
      this.render();
    });
  }

  fetchHistory() {
    this.isLoadingHistory = true;
    this.render();
    this.api.getBorrowingHistory().then(h => {
      this.history = h;
      this.isLoadingHistory = false;
      this.render();
    });
  }

  get pendingRequests() {
    return this.myListings.filter(l => l.status === AvailabilityStatus.PENDING);
  }

  get activeBorrows() {
    return this.myBorrows.filter(i =>
      (i.status === AvailabilityStatus.PENDING ||
        i.status === AvailabilityStatus.APPROVED ||
        i.status === AvailabilityStatus.BORROWED ||
        i.status === AvailabilityStatus.DISPUTED) &&
      i.type !== ListingType.GIVE &&
      i.type !== ListingType.SELL
    );
  }

  get borrowedHistory() {
    return this.history.filter(i =>
      !!i.listing &&
      i.listing.type !== ListingType.GIVE &&
      i.listing.type !== ListingType.SELL &&
      !this.myBorrows.some(active => active.id === i.listing!.id)
    );
  }

  get giftActive() {
    return this.myBorrows.filter(i => i.type === ListingType.GIVE);
  }

  get giftHistory() {
    return this.history.filter(i =>
      !!i.listing &&
      i.listing.type === ListingType.GIVE &&
      !this.myBorrows.some(active => active.id === i.listing!.id)
    );
  }

  get purchaseActive() {
    return this.myBorrows.filter(i => i.type === ListingType.SELL);
  }

  get purchaseHistory() {
    return this.history.filter(i =>
      !!i.listing &&
      i.listing.type === ListingType.SELL &&
      !this.myBorrows.some(active => active.id === i.listing!.id)
    );
  }

  listingIsHidden(item: Listing): boolean {
    return item.status === AvailabilityStatus.HIDDEN;
  }

  listingIsBorrowed(item: Listing): boolean {
    return item.status === AvailabilityStatus.BORROWED;
  }

  listingIsApproved(item: Listing): boolean {
    return item.status === AvailabilityStatus.APPROVED;
  }

  listingIsActiveLoan(item: Listing): boolean {
    return this.listingIsBorrowed(item) || this.listingIsApproved(item);
  }

  listingIsGifted(item: Listing): boolean {
    return item.status === AvailabilityStatus.GIFTED || (this.listingIsBorrowed(item) && item.type === ListingType.GIVE);
  }

  listingIsSold(item: Listing): boolean {
    return item.status === AvailabilityStatus.SOLD;
  }

  listingIsPending(item: Listing): boolean {
    return item.status === AvailabilityStatus.PENDING;
  }

  listingIsAvailable(item: Listing): boolean {
    return item.status === AvailabilityStatus.AVAILABLE;
  }

  listingIsBlocked(item: Listing): boolean {
    return item.status === AvailabilityStatus.BLOCKED;
  }

  listingShowOverview(item: Listing): boolean {
    return this.listingIsGifted(item) || this.listingIsActiveLoan(item) || this.listingIsSold(item);
  }

  listingBadgeClass(item: Listing): string {
    if (this.listingIsAvailable(item)) return 'bg-emerald-100 text-emerald-700';
    if (this.listingIsGifted(item)) return 'bg-pink-100 text-pink-700';
    if (this.listingIsActiveLoan(item)) return 'bg-amber-100 text-amber-700';
    if (this.listingIsPending(item)) return 'bg-indigo-100 text-indigo-700 animate-pulse';
    if (this.listingIsHidden(item)) return 'bg-slate-100 text-slate-500';
    if (this.listingIsBlocked(item)) return 'bg-red-100 text-red-700';
    return 'bg-gray-100 text-gray-600';
  }

  listingStatusIcon(item: Listing): any {
    if (this.listingIsAvailable(item)) return this.CheckCircle2;
    if (this.listingIsGifted(item)) return this.Gift;
    if (this.listingIsActiveLoan(item)) return this.Clock;
    if (this.listingIsPending(item)) return this.BellRing;
    if (this.listingIsHidden(item)) return this.EyeOff;
    if (this.listingIsBlocked(item)) return this.Ban;
    return this.EyeOff;
  }

  listingStatusLabel(item: Listing): string {
    return this.listingIsGifted(item) ? 'GIFTED' : String(item.status || '');
  }

  listingRowClass(item: Listing): string {
    return this.listingIsHidden(item) ? 'bg-gray-50/80' : 'hover:bg-gray-50';
  }

  listingImageClass(item: Listing): string {
    return (this.listingIsHidden(item) || this.listingIsBlocked(item)) ? 'grayscale opacity-60' : '';
  }

  listingTitleClass(item: Listing): string {
    return (this.listingIsHidden(item) || this.listingIsBlocked(item)) ? 'text-gray-500' : 'text-gray-900';
  }

  listingActionsClass(item: Listing): string {
    return this.listingIsHidden(item) ? 'opacity-60 hover:opacity-100 transition-opacity' : '';
  }

  async handleAddNew() {
    if (this.actionLoading === 'add_new') return;
    this.actionLoading = 'add_new';
    this.render();
    try {
      const sub = await this.api.getCurrentSubscription().catch(() => null);
      if (sub) {
        this.currentSub = { planType: String((sub as any).planType || ''), status: String((sub as any).status || '') };
      } else {
        this.currentSub = null;
      }

      const status = String(this.currentSub?.status || '').toLowerCase();
      const subscribed = !!this.currentSub && status !== 'canceled' && status !== 'cancelled';
      if (!subscribed) {
        this.subscriptionGateOpen = true;
        return;
      }

      this.router.navigate(['/new-item']);
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  closeSubscriptionGate() {
    this.subscriptionGateOpen = false;
    this.render();
  }

  confirmSubscriptionGate() {
    this.subscriptionGateOpen = false;
    this.router.navigate(['/subscription']);
    this.render();
  }

  handleEdit(item: Listing) {
    this.router.navigate(['/edit', item.id]);
  }

  handleDeleteClick(item: Listing) {
    if (item.status === AvailabilityStatus.PENDING || item.status === AvailabilityStatus.APPROVED || item.status === AvailabilityStatus.BORROWED) {
      this.error = "You can’t delete this listing while it’s borrowed or in an active request. Return it first.";
      setTimeout(() => {
        this.error = null;
        this.render();
      }, 5000);
      this.render();
      return;
    }
    this.deleteTarget = item;
    this.deleteConfirmOpen = true;
    this.render();
  }

  handleToggleStatus(item: Listing) {
    const newStatus = item.status === AvailabilityStatus.HIDDEN 
      ? AvailabilityStatus.AVAILABLE 
      : AvailabilityStatus.HIDDEN;
    
    if (item.status === AvailabilityStatus.BORROWED) {
      this.error = "Cannot disable an item that is currently borrowed.";
      setTimeout(() => {
        this.error = null;
        this.render();
      }, 5000);
      this.render();
      return;
    }

    this.api.updateListing(item.id, {
      title: item.title,
      description: item.description || '',
      category: item.category || '',
      type: item.type,
      hourlyRate: (item as any).hourlyRate ?? 0,
      imageUrl: item.imageUrl,
      gallery: (item as any).gallery ?? [],
      autoApprove: !!(item as any).autoApprove,
      x: (item as any).location?.x ?? (item as any).x ?? 0,
      y: (item as any).location?.y ?? (item as any).y ?? 0,
      pickupLocationId: (item as any).pickupLocation?.id ?? null,
    }).then(() => {
      this.fetchListings();
    });
  }

  closeDeleteConfirm() {
    this.deleteConfirmOpen = false;
    this.deleteTarget = null;
    this.render();
  }

  openReportModal(listing: Listing) {
    if (!this.user) return;
    if (!listing) return;
    if (listing.ownerId === this.user.id) return;
    if (listing.borrowerId !== this.user.id) return;
    this.reportTarget = listing;
    this.reportReason = '';
    this.reportDetails = '';
    this.reportError = null;
    this.showReportModal = true;
    this.render();
  }

  closeReportModal() {
    this.showReportModal = false;
    this.reportTarget = null;
    this.reportReason = '';
    this.reportDetails = '';
    this.reporting = false;
    this.reportError = null;
    this.render();
  }

  closeReportSuccess() {
    this.showReportSuccess = false;
    this.render();
  }

  async submitReport() {
    const listing = this.reportTarget;
    if (!listing) return;
    if (!this.user) return;
    if (!this.reportReason) return;
    if (listing.ownerId === this.user.id) return;
    if (listing.borrowerId !== this.user.id) return;

    this.reporting = true;
    this.reportError = null;
    this.render();
    try {
      await this.api.reportListing(listing.id, this.reportReason, this.reportDetails);
      this.showReportModal = false;
      this.showReportSuccess = true;
      this.reportTarget = null;
      this.reportReason = '';
      this.reportDetails = '';
    } catch (e: any) {
      const msg = e?.message || this.i18n.t('listing.error.report_failed');
      this.showReportModal = false;
      this.reportError = msg.includes('already_reported_for_reason')
        ? this.i18n.t('listing.report.already_reported')
        : msg;
    } finally {
      this.reporting = false;
      this.render();
    }
  }

  confirmDeleteListing() {
    const item = this.deleteTarget;
    if (!item) return;
    this.actionLoading = item.id;
    this.deleteConfirmOpen = false;
    this.render();
    this.api.deleteListing(item.id)
      .then(() => {
        this.fetchListings();
      })
      .finally(() => {
        this.actionLoading = null;
        this.deleteTarget = null;
        this.render();
      });
  }

  handleApprove(id: string) {
    this.actionLoading = id;
    this.render();
    this.api.approveRequest(id)
      .then(() => this.fetchListings())
      .finally(() => {
        this.actionLoading = null;
        this.render();
      });
  }

  handleDeny(id: string) {
    this.actionLoading = id;
    this.render();
    this.api.denyRequest(id)
      .then(() => this.fetchListings())
      .finally(() => {
        this.actionLoading = null;
        this.render();
      });
  }

  handleMessageUser(u?: User) {
    if (!u) return;
    const email = String(u.email || '').trim();
    if (email) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverEmail: email } });
      return;
    }
    const id = String(u.id || '').trim();
    if (id) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverId: id } });
    }
  }

  openUserPreview(u?: User) {
    if (!u) return;
    this.userPreview = u;
    this.userPreviewOpen = true;
    this.render();
  }

  closeUserPreview() {
    this.userPreviewOpen = false;
    this.userPreview = null;
    this.render();
  }

  navigateToListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  handleReturnClick(item: Listing) {
    if (!this.returnsEnabled) return;
    this.returningItem = item;
    this.render();
  }

  setOverviewItem(item: any) {
    if (!item) return;
    if ((item as any).listing) {
      this.overviewItem = item;
    } else {
      this.overviewItem = item as Listing;
    }
    this.render();
  }

  closeOverview() {
    this.overviewItem = null;
    this.render();
  }

  closeReturn() {
    this.returningItem = null;
    this.render();
  }

  handleReturnComplete() {
    const item = this.returningItem;
    this.returningItem = null;
    this.reviewingItem = item;
    this.fetchListings();
    this.fetchHistory();
    this.render();
  }

  closeReview() {
    this.reviewingItem = null;
    this.render();
  }
}
