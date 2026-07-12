import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Plus, Loader2, Package, Users, ShieldAlert, Search, Clock, MapPin, Calendar, Shield, Trash2, Edit2, Eye, EyeOff, X, Upload, Image as ImageIcon, BadgeCheck, ChevronRight, BellRing, Check, X as XIcon, MessageSquare, Zap, Ban, RefreshCcw, Mail, Phone, FileCheck, DollarSign, Gift, History, Star, AlertTriangle, CheckCircle2, Lock, Settings, Flag } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { User, Listing, AvailabilityStatus, ListingType, BorrowHistoryItem, VerificationStatus } from '../../core/models/types';
import { getListingPrimaryRate, getListingPriceSuffix, isListingFree } from '../../core/utils/listing-pricing';
import { LayoutModeService } from '../../core/services/layout-mode.service';

const DEFAULT_USER_AVATAR = 'assets/images/default-user-photo.png';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
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
  layoutMode = inject(LayoutModeService);

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
  readonly Search = Search;
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
  // PLATFORM SUBSCRIPTION ONLY:
  // `currentSub` is the legacy platform/lender subscription state shown on the dashboard.
  // Borrower subscription state is handled separately in the borrowing flow/settings.
  currentSub: { planType: string; status: string } | null = null;
  today = new Date();

  isLoadingListings = false;
  isLoadingBorrows = false;
  isLoadingHistory = false;
  isLoadingRecs = false;
  actionLoading: string | null = null;
  ownerReturnSessionReady: Record<string, boolean> = {};
  borrowerReturnRequestSubmitted: Record<string, boolean> = {};
  paymentSuccess = false;
  upgradeSuccess = false;
  error: string | null = null;
  private listingsIntervalId: any = null;
  incomingRequestsSearchQuery = '';
  incomingRequestsTab: 'all' | 'lend' | 'give' | 'sell' = 'all';
  lendingSearchQuery = '';
  lendingTab: 'all' | 'available' | 'requests' | 'active' = 'all';
  borrowingSearchQuery = '';
  borrowingTab: 'all' | 'pending' | 'pickup' | 'return' = 'all';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get returnsEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('returns', 'qr') ||
      this.settingsConfig.isSectionEnabled('returns', 'manual') ||
      this.settingsConfig.isSectionEnabled('returns', 'dispute');
  }

  listingPriceLabel(item: Listing): string {
    if (isListingFree(item)) return 'Free';
    return `${this.i18n.formatPrice(getListingPrimaryRate(item))}${getListingPriceSuffix(item)}`;
  }

  listingHasVisiblePrice(item: Listing): boolean {
    return !isListingFree(item) && getListingPrimaryRate(item) > 0;
  }

  get subscriptionEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('enable', 'subscription');
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
        const borrowerSubscription = String(params['borrower_subscription'] || '').toLowerCase();
        if (sessionId) {
          this.paymentSuccess = true;
          this.render();
          try {
            if (borrowerSubscription === '1' || borrowerSubscription === 'true') {
              await this.api.syncBorrowingSubscriptionFromSession(sessionId);
            } else {
              await this.api.syncSubscriptionFromSession(sessionId);
            }
          } catch { }
          try {
            const sub = (borrowerSubscription === '1' || borrowerSubscription === 'true')
              ? await this.api.getCurrentBorrowingSubscription()
              : await this.api.getCurrentSubscription();
            if (sub) {
              this.currentSub = { planType: String((sub as any).planType || ''), status: String((sub as any).status || '') };
            } else {
              this.currentSub = null;
            }
          } catch {
            this.currentSub = null;
          }
          try {
            this.router.navigate([], { relativeTo: this.route, queryParams: { session_id: null, borrower_subscription: null }, queryParamsHandling: 'merge', replaceUrl: true });
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
    Promise.all([
      this.api.getMyListings(),
      this.api.getMyBorrowedListings(),
      this.api.getListings()
    ]).then(([owned, borrowed, all]) => {
      this.myListings = owned;
      this.myBorrows = borrowed;
      this.recommendations = all
        .filter(l => l.ownerId !== this.user?.id && l.status === AvailabilityStatus.AVAILABLE)
        .slice(0, 4);
      this.isLoadingListings = false;
      this.isLoadingBorrows = false;
      this.isLoadingRecs = false;
      this.render();
      void this.refreshOwnerReturnSessionState();
    });
  }

  private async refreshOwnerReturnSessionState() {
    const ownerCandidates = this.myListings.filter(item =>
      item.status === AvailabilityStatus.BORROWED ||
      item.status === AvailabilityStatus.WAITING_FOR_RETURN ||
      item.status === AvailabilityStatus.DISPUTED
    );
    const borrowerCandidates = this.myBorrows.filter(item =>
      item.status === AvailabilityStatus.BORROWED ||
      item.status === AvailabilityStatus.WAITING_FOR_RETURN ||
      item.status === AvailabilityStatus.DISPUTED
    );
    const combined = [...ownerCandidates, ...borrowerCandidates];
    if (!combined.length) {
      this.ownerReturnSessionReady = {};
      this.borrowerReturnRequestSubmitted = {};
      this.render();
      return;
    }

    // In the simplified return flow, WAITING_FOR_RETURN is the reliable source of truth:
    // the borrower has already submitted the request and the lender can review it.
    this.ownerReturnSessionReady = Object.fromEntries(
      ownerCandidates.map(item => [item.id, item.status === AvailabilityStatus.WAITING_FOR_RETURN])
    );
    this.borrowerReturnRequestSubmitted = Object.fromEntries(
      borrowerCandidates.map(item => [item.id, item.status === AvailabilityStatus.WAITING_FOR_RETURN])
    );
    this.render();
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

  get filteredPendingRequests() {
    const query = this.normalizeDashboardSearch(this.incomingRequestsSearchQuery);
    return this.pendingRequests.filter(item => {
      if (this.incomingRequestsTab === 'lend' && item.type !== ListingType.LEND) return false;
      if (this.incomingRequestsTab === 'give' && item.type !== ListingType.GIVE) return false;
      if (this.incomingRequestsTab === 'sell' && item.type !== ListingType.SELL) return false;
      if (!query) return true;
      const borrowerName = String(item.borrower?.name || '').toLowerCase();
      const title = String(item.title || '').toLowerCase();
      const ref = String(item.itemReference || '').toLowerCase();
      const category = String(item.category || '').toLowerCase();
      return borrowerName.includes(query) || title.includes(query) || ref.includes(query) || category.includes(query);
    });
  }

  get activeBorrows() {
    return this.myBorrows.filter(i =>
      (i.status === AvailabilityStatus.PENDING ||
        i.status === AvailabilityStatus.APPROVED ||
        i.status === AvailabilityStatus.READY_FOR_PICKUP ||
        i.status === AvailabilityStatus.WAITING_FOR_RETURN ||
        i.status === AvailabilityStatus.PARTNER_ACTIVE ||
        i.status === AvailabilityStatus.BORROWED ||
        i.status === AvailabilityStatus.DISPUTED) &&
      i.type !== ListingType.GIVE &&
      i.type !== ListingType.SELL
    );
  }

  get filteredLendingItems() {
    const query = this.normalizeDashboardSearch(this.lendingSearchQuery);
    return this.myListings.filter(item => {
      if (this.lendingTab === 'available' && item.status !== AvailabilityStatus.AVAILABLE) return false;
      if (this.lendingTab === 'requests' && item.status !== AvailabilityStatus.PENDING) return false;
      if (this.lendingTab === 'active' && !this.listingIsActiveLoan(item)) return false;
      if (!query) return true;
      const borrowerName = String(item.borrower?.name || '').toLowerCase();
      const title = String(item.title || '').toLowerCase();
      const ref = String(item.itemReference || '').toLowerCase();
      const pickup = this.pickupLocationText(item).toLowerCase();
      const category = String(item.category || '').toLowerCase();
      return title.includes(query) || ref.includes(query) || pickup.includes(query) || borrowerName.includes(query) || category.includes(query);
    });
  }

  get filteredBorrowingItems() {
    const query = this.normalizeDashboardSearch(this.borrowingSearchQuery);
    return this.activeBorrows.filter(item => {
      if (this.borrowingTab === 'pending' && item.status !== AvailabilityStatus.PENDING) return false;
      if (this.borrowingTab === 'pickup' && item.status !== AvailabilityStatus.READY_FOR_PICKUP) return false;
      if (this.borrowingTab === 'return' && item.status !== AvailabilityStatus.WAITING_FOR_RETURN && item.status !== AvailabilityStatus.BORROWED && item.status !== AvailabilityStatus.DISPUTED) return false;
      if (!query) return true;
      const ownerName = String(item.owner?.name || '').toLowerCase();
      const title = String(item.title || '').toLowerCase();
      const ref = String(item.itemReference || '').toLowerCase();
      const pickup = this.pickupLocationText(item).toLowerCase();
      return title.includes(query) || ref.includes(query) || ownerName.includes(query) || pickup.includes(query);
    });
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
    return item.status === AvailabilityStatus.BORROWED || item.status === AvailabilityStatus.WAITING_FOR_RETURN;
  }

  listingIsApproved(item: Listing): boolean {
    if (item.partnerId) {
      return item.status === AvailabilityStatus.PARTNER_ACTIVE;
    }
    return item.status === AvailabilityStatus.APPROVED;
  }

  listingIsReadyForPickup(item: Listing): boolean {
    return item.status === AvailabilityStatus.READY_FOR_PICKUP;
  }

  listingIsActiveLoan(item: Listing): boolean {
    return this.listingIsBorrowed(item) || this.listingIsApproved(item) || this.listingIsReadyForPickup(item);
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
    if (!item) return '';
    if (this.listingIsGifted(item)) return 'GIFTED';
    switch (item.status) {
      case AvailabilityStatus.PENDING:
        return this.i18n.t('dash.request_pending');
      case AvailabilityStatus.APPROVED:
        return this.i18n.t('dash.approved');
      case AvailabilityStatus.READY_FOR_PICKUP:
        return this.i18n.t('dash.ready_for_pickup');
      case AvailabilityStatus.WAITING_FOR_RETURN:
        return this.i18n.t('dash.waiting_for_return');
      case AvailabilityStatus.DISPUTED:
        return this.i18n.t('dash.disputed');
      default:
        return String(item.status || '');
    }
  }

  pickupLocationText(item: Listing): string {
    if (!item) return '';
    const addr = String((item as any).pickupLocation?.address || '').trim();
    if (addr) return addr;
    const custom = String((item as any).pickupLocationCustom || '').trim();
    if (custom) return custom;
    const street = String((item as any).pickupLocationStreet || '').trim();
    const house = String((item as any).pickupLocationHouseNumber || '').trim();
    const city = String((item as any).pickupLocationCity || '').trim();
    const zip = String((item as any).pickupLocationZip || '').trim();
    const line1 = `${street} ${house}`.trim();
    const line2 = `${city} ${zip}`.trim();
    return (line1 && line2) ? `${line1}, ${line2}` : (line1 || line2);
  }

  lendingCounterLabel(tab: 'all' | 'available' | 'requests' | 'active'): number {
    if (tab === 'available') return this.myListings.filter(item => item.status === AvailabilityStatus.AVAILABLE).length;
    if (tab === 'requests') return this.myListings.filter(item => item.status === AvailabilityStatus.PENDING).length;
    if (tab === 'active') return this.myListings.filter(item => this.listingIsActiveLoan(item)).length;
    return this.myListings.length;
  }

  incomingRequestsCounterLabel(tab: 'all' | 'lend' | 'give' | 'sell'): number {
    if (tab === 'lend') return this.pendingRequests.filter(item => item.type === ListingType.LEND).length;
    if (tab === 'give') return this.pendingRequests.filter(item => item.type === ListingType.GIVE).length;
    if (tab === 'sell') return this.pendingRequests.filter(item => item.type === ListingType.SELL).length;
    return this.pendingRequests.length;
  }

  borrowingCounterLabel(tab: 'all' | 'pending' | 'pickup' | 'return'): number {
    if (tab === 'pending') return this.activeBorrows.filter(item => item.status === AvailabilityStatus.PENDING).length;
    if (tab === 'pickup') return this.activeBorrows.filter(item => item.status === AvailabilityStatus.READY_FOR_PICKUP).length;
    if (tab === 'return') return this.activeBorrows.filter(item => item.status === AvailabilityStatus.WAITING_FOR_RETURN || item.status === AvailabilityStatus.BORROWED || item.status === AvailabilityStatus.DISPUTED).length;
    return this.activeBorrows.length;
  }

  lendingBorrowerLabel(item: Listing): string {
    if (item.borrower?.name) return item.borrower.name;
    if (item.status === AvailabilityStatus.PENDING) return this.i18n.t('dash.borrower_requested');
    return this.i18n.t('dash.no_borrower_yet');
  }

  requestIntentLabel(item: Listing): string {
    if (item.type === ListingType.GIVE) return this.i18n.t('dash.request_claim');
    if (item.type === ListingType.SELL) return this.i18n.t('dash.request_purchase');
    return this.i18n.t('dash.request_borrow');
  }

  userAvatarUrl(user: User | null | undefined, seed: string, size = 80): string {
    const avatar = String(user?.avatarUrl || '').trim();
    if (avatar) return avatar;
    return DEFAULT_USER_AVATAR;
  }

  onAvatarError(event: Event, seed: string, size = 80) {
    const img = event.target as HTMLImageElement | null;
    if (!img) return;
    const fallback = DEFAULT_USER_AVATAR;
    if (img.src !== fallback) {
      img.src = fallback;
    }
  }

  statusPillClass(item: Listing): string {
    if (item.status === AvailabilityStatus.READY_FOR_PICKUP) return 'status-pill status-pill-warning';
    if (item.status === AvailabilityStatus.AVAILABLE || item.status === AvailabilityStatus.APPROVED || item.status === AvailabilityStatus.PARTNER_ACTIVE) return 'status-pill status-pill-success';
    if (item.status === AvailabilityStatus.PENDING) return 'status-pill status-pill-info';
    if (item.status === AvailabilityStatus.BORROWED || item.status === AvailabilityStatus.WAITING_FOR_RETURN) return 'status-pill status-pill-warning';
    if (item.status === AvailabilityStatus.DISPUTED || item.status === AvailabilityStatus.BLOCKED) return 'status-pill status-pill-danger';
    if (item.status === AvailabilityStatus.HIDDEN) return 'status-pill status-pill-muted';
    return 'status-pill status-pill-muted';
  }

  private normalizeDashboardSearch(value: string): string {
    return String(value || '').trim().toLowerCase();
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

  ownerCanStartReturn(item: Listing): boolean {
    if (item.status === AvailabilityStatus.WAITING_FOR_RETURN) {
      return true;
    }
    return !!this.ownerReturnSessionReady[item.id];
  }

  ownerReturnLabel(item: Listing): string {
    return this.ownerCanStartReturn(item) ? this.i18n.t('dash.view_return_request') : this.i18n.t('dash.waiting_for_return');
  }

  ownerCanRequestAdminReturn(item: Listing): boolean {
    const st = item.status;
    return st === AvailabilityStatus.BORROWED || st === AvailabilityStatus.WAITING_FOR_RETURN || st === AvailabilityStatus.DISPUTED;
  }

  ownerAdminReturnLabel(item: Listing): string {
    return item.adminReturnRequestedAt ? this.i18n.t('return.admin_review_requested') : this.i18n.t('return.request_admin_unlock');
  }

  borrowerHasSubmittedReturn(item: Listing): boolean {
    return !!this.borrowerReturnRequestSubmitted[item.id];
  }

  borrowerReturnLabel(item: Listing): string {
    return this.borrowerHasSubmittedReturn(item) ? this.i18n.t('dash.return') : this.i18n.t('dash.start_return_process');
  }

  async handleAddNew() {
    if (this.actionLoading === 'add_new') return;
    this.actionLoading = 'add_new';
    this.render();
    try {
      await this.settingsConfig.ensureLoaded();
      if (!this.settingsConfig.isSectionEnabled('enable', 'subscription')) {
        this.router.navigate(['/new-item']);
        return;
      }
      const sub = await this.api.getCurrentSubscription().catch(() => null);
      if (sub) {
        this.currentSub = { planType: String((sub as any).planType || ''), status: String((sub as any).status || '') };
      } else {
        this.currentSub = null;
      }

      const status = String(this.currentSub?.status || '').toLowerCase();
      const subscribed = !!this.currentSub && status !== 'canceled' && status !== 'cancelled';
      if (!subscribed) {
        this.router.navigate(['/subscription/required'], { queryParams: { from: '/dashboard' } });
        return;
      }

      this.router.navigate(['/new-item']);
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  handleEdit(item: Listing) {
    this.router.navigate(['/edit', item.id]);
  }

  handleDeleteClick(item: Listing) {
    if (item.status === AvailabilityStatus.PENDING || item.status === AvailabilityStatus.APPROVED || item.status === AvailabilityStatus.READY_FOR_PICKUP || item.status === AvailabilityStatus.WAITING_FOR_RETURN || item.status === AvailabilityStatus.BORROWED) {
      this.error = "You can’t delete this listing while it’s borrowed or in an active request. Return it first.";
      setTimeout(() => {
        this.error = null;
        this.render();
      }, 5000);
      this.render();
      return;
    }
    this.router.navigate(['/listing', item.id, 'delete'], { queryParams: { from: '/dashboard' } });
  }

  handleToggleStatus(item: Listing) {
    const newStatus = item.status === AvailabilityStatus.HIDDEN 
      ? AvailabilityStatus.AVAILABLE 
      : AvailabilityStatus.HIDDEN;
    
    if (item.status === AvailabilityStatus.BORROWED || item.status === AvailabilityStatus.READY_FOR_PICKUP || item.status === AvailabilityStatus.WAITING_FOR_RETURN) {
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
      dailyRate: (item as any).dailyRate ?? 0,
      monthlyRate: (item as any).monthlyRate ?? 0,
      pricingUnit: String((item as any).pricingUnit || 'HOURLY'),
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

  openReportModal(listing: Listing) {
    if (!this.user) return;
    if (!listing) return;
    if (listing.ownerId === this.user.id) return;
    if (listing.borrowerId !== this.user.id) return;
    this.router.navigate(['/listing', listing.id, 'report'], { queryParams: { from: '/dashboard' } });
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

  handleMarkReadyForPickup(id: string) {
    this.actionLoading = id;
    this.render();
    this.api.markReadyForPickup(id)
      .then(() => this.fetchListings())
      .finally(() => {
        this.actionLoading = null;
        this.render();
      });
  }

  handleMarkPickedUp(id: string) {
    this.actionLoading = id;
    this.render();
    this.api.markPickedUp(id)
      .then(() => this.router.navigate(['/listing', id, 'return'], { queryParams: { from: '/dashboard' } }))
      .finally(() => {
        this.actionLoading = null;
        this.render();
      });
  }

  handleMessageUser(u?: User) {
    if (!u) return;
    const email = String(u.email || '').trim();
    const id = String(u.id || '').trim();
    const qp: any = {};
    if (email) qp.receiverEmail = email;
    else if (id) qp.receiverId = id;
    this.router.navigate(['/mailbox/compose'], { queryParams: qp, state: { returnTo: '/dashboard' } as any });
  }

  openUserPreview(u?: User) {
    if (!u) return;
    this.router.navigate(['/user/preview'], { state: { user: u, returnTo: '/dashboard' } as any });
  }

  navigateToListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  handleReturnClick(item: Listing) {
    if (!this.returnsEnabled) return;
    if (item.ownerId === this.user?.id) {
      if (!this.ownerCanStartReturn(item)) return;
      this.router.navigate(['/listing', item.id, 'accept-return'], { queryParams: { from: '/dashboard' } });
      return;
    }
    this.router.navigate(['/listing', item.id, 'return'], { queryParams: { from: '/dashboard' } });
  }

  handleRequestAdminReturn(item: Listing) {
    if (!this.ownerCanRequestAdminReturn(item)) return;
    if (item.adminReturnRequestedAt) return;
    this.actionLoading = `admin-return-${item.id}`;
    this.render();
    this.api.requestAdminReturn(item.id)
      .then(() => this.fetchListings())
      .catch((e: any) => {
        this.error = e?.message || 'Failed to request admin return unlock.';
        setTimeout(() => {
          this.error = null;
          this.render();
        }, 5000);
      })
      .finally(() => {
        this.actionLoading = null;
        this.render();
      });
  }

  setOverviewItem(item: any) {
    if (!item) return;
    this.router.navigate(['/dashboard/transaction'], { state: { item, currentUser: this.user, returnTo: '/dashboard' } as any });
  }
}
