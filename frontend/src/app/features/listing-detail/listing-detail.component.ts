import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, MapPin, ShieldCheck, ArrowLeft, Calendar, CheckCircle2, AlertCircle, Loader2, Share2, BadgeCheck, Flag, DollarSign, Gift, ChevronLeft, ChevronRight, Star, X, Minus, Plus, Clock, CreditCard, Wallet, AlertTriangle, BellRing, Check, X as XIcon, Zap, ThumbsUp, Trash2, Lock as LockIcon } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User, Listing, AvailabilityStatus, ListingType, ReturnSessionResponse } from '../../core/models/types';
import { getListingAdditionalRates, getListingPrimaryRate, getListingPricingUnit, getPricingUnitShort, isListingFree } from '../../core/utils/listing-pricing';
import { LayoutModeService } from '../../core/services/layout-mode.service';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-detail.component.html',
  styleUrl: './listing-detail.component.css'
})
export class ListingDetailComponent implements OnInit, OnDestroy {
  route = inject(ActivatedRoute);
  router = inject(Router);
  api = inject(ApiService);
  i18n = inject(I18nService);
  cdr = inject(ChangeDetectorRef);
  layoutMode = inject(LayoutModeService);

  readonly MapPin = MapPin;
  readonly ShieldCheck = ShieldCheck;
  readonly ArrowLeft = ArrowLeft;
  readonly Calendar = Calendar;
  readonly CheckCircle2 = CheckCircle2;
  readonly Loader2 = Loader2;
  readonly Share2 = Share2;
  readonly BadgeCheck = BadgeCheck;
  readonly Gift = Gift;
  readonly Zap = Zap;
  readonly AlertTriangle = AlertTriangle;
  readonly BellRing = BellRing;
  readonly Check = Check;
  readonly XIcon = XIcon;
  readonly Trash2 = Trash2;
  readonly ChevronLeft = ChevronLeft;
  readonly ChevronRight = ChevronRight;
  readonly Clock = Clock;

  listing: Listing | null = null;
  currentUser: User | null = null;
  today = new Date();
  loading = true;
  borrowing = false;
  activeImage = '';
  activeImageFailed = false;
  error: string | null = null;

  get isPartnerListing(): boolean {
    return !!this.listing?.partnerId;
  }

  get displayOwnerName(): string {
    if (this.isPartnerListing) return this.listing?.partnerName || 'Partner';
    return this.listing?.owner?.name || '';
  }

  get displayAvatarSeed(): string {
    return String(this.isPartnerListing ? (this.listing?.partnerId || 'partner') : (this.listing?.owner?.id || 'user'));
  }

  actionLoading: string | null = null;
  wasAutoApproved = false;
  showSuccess = false;
  successMessage = '';
  showActionError = false;
  actionErrorMessage = '';
  shareNotice: string | null = null;
  private pendingNoticeSuccess: string | null = null;
  private successTimer: any = null;
  private actionErrorTimer: any = null;
  private shareTimer: any = null;
  private statusPollTimer: any = null;
  returnSession: ReturnSessionResponse | null = null;
  returnRequestReady = false;
  borrowerReturnSubmitted = false;

  backToUrl = '/';

  AvailabilityStatus = AvailabilityStatus;
  ListingType = ListingType;

  get isAvailable(): boolean {
    const l = this.listing;
    if (!l) return false;
    if (l.status === AvailabilityStatus.AVAILABLE) return true;
    if (l.partnerId && l.status === AvailabilityStatus.PARTNER_ACTIVE && !l.borrowerId) return true;
    return false;
  }

  get isPending(): boolean {
    return this.listing?.status === AvailabilityStatus.PENDING || this.listing?.status === AvailabilityStatus.PARTNER_BORROW_REQUESTED;
  }

  get isApproved(): boolean {
    if (this.isPartnerListing) return false;
    return this.listing?.status === AvailabilityStatus.APPROVED;
  }

  get isReadyForPickup(): boolean {
    return this.listing?.status === AvailabilityStatus.READY_FOR_PICKUP;
  }

  async handleMarkReadyForPickup() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.actionLoading) return;
    this.actionLoading = 'READY';
    this.render();
    try {
      await this.api.markReadyForPickup(listing.id);
      await this.reloadListing();
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to mark ready');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  async handleMarkPickedUp() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) return;
    if (this.actionLoading) return;
    this.actionLoading = 'PICKED_UP';
    this.render();
    try {
      await this.api.markPickedUp(listing.id);
      await this.router.navigate(['/listing', listing.id, 'return'], {
        queryParams: { from: `/listing/${listing.id}` }
      });
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to confirm pickup');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  get isWaitingForReturn(): boolean {
    return this.listing?.status === AvailabilityStatus.WAITING_FOR_RETURN;
  }

  get isPartnerBorrowRequested(): boolean {
    return this.listing?.status === AvailabilityStatus.PARTNER_BORROW_REQUESTED;
  }

  get isBorrowed(): boolean {
    return this.listing?.status === AvailabilityStatus.BORROWED || this.isWaitingForReturn;
  }

  get isGifted(): boolean {
    return this.listing?.status === AvailabilityStatus.GIFTED;
  }

  get isSold(): boolean {
    return this.listing?.status === AvailabilityStatus.SOLD;
  }

  get isBlocked(): boolean {
    return this.listing?.status === AvailabilityStatus.BLOCKED;
  }

  get isAutoApprove(): boolean {
    return !!this.listing?.autoApprove;
  }

  get canOwnerDelete(): boolean {
    return this.isOwner && !this.isBorrowed && !this.isPending && !this.isApproved && !this.isReadyForPickup && !this.isWaitingForReturn;
  }

  get deleteConfirmMessage(): string {
    const title = this.listing?.title || '';
    return `${this.i18n.t('dashboard.delete_listing_msg')} "${title}"? ${this.i18n.t('dashboard.delete_listing_cannot_undo')}`;
  }

  get ownerJoinedYear(): number {
    const joined = this.listing?.owner?.joinedDate;
    if (joined) {
      const dt = new Date(joined);
      if (!isNaN(dt.getTime())) return dt.getFullYear();
    }
    return new Date().getFullYear();
  }

  get galleryImages(): string[] {
    const listing = this.listing;
    if (!listing) return [];
    const images = [listing.imageUrl, ...(listing.gallery || [])].filter(Boolean);
    return Array.from(new Set(images));
  }

  get pickupAreaLabel(): string {
    const listing = this.listing;
    if (!listing) return 'Nearby';

    const city = String((listing as any).pickupLocationCity || '').trim();
    const zip = String((listing as any).pickupLocationZip || '').trim();
    if (city || zip) return `${city} ${zip}`.trim();

    const pickupAddress = String(listing.pickupLocation?.address || '').trim();
    const maskedPickup = this.maskAddress(pickupAddress);
    if (maskedPickup) return maskedPickup;

    const ownerAddress = String(listing.owner?.address || '').trim();
    const maskedOwner = this.maskAddress(ownerAddress);
    if (maskedOwner) return maskedOwner;

    return 'Nearby';
  }

  private maskAddress(address: string): string {
    const s = String(address || '').replace(/\s+/g, ' ').trim();
    if (!s) return '';

    const parts = s.split(',').map(p => p.trim()).filter(Boolean);
    if (parts.length >= 2) {
      const cityZip = parts[parts.length - 1];
      return cityZip;
    }
    return s;
  }

  get activeImageIndex(): number {
    const images = this.galleryImages;
    const idx = images.findIndex(i => i === this.activeImage);
    return idx >= 0 ? idx : 0;
  }

  prevImage() {
    const images = this.galleryImages;
    if (images.length <= 1) return;
    const nextIdx = (this.activeImageIndex - 1 + images.length) % images.length;
    this.setActiveImage(images[nextIdx]);
  }

  nextImage() {
    const images = this.galleryImages;
    if (images.length <= 1) return;
    const nextIdx = (this.activeImageIndex + 1) % images.length;
    this.setActiveImage(images[nextIdx]);
  }

  setActiveImage(img: string) {
    this.activeImage = String(img || '');
    this.activeImageFailed = !this.activeImage;
    this.render();
  }

  onActiveImageError() {
    this.activeImageFailed = true;
    this.render();
  }

  onActiveImageLoad() {
    this.activeImageFailed = false;
    this.render();
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  private notifySuccess(message: string) {
    this.successMessage = message || '';
    this.showSuccess = true;
    if (this.successTimer) clearTimeout(this.successTimer);
    this.successTimer = setTimeout(() => {
      this.showSuccess = false;
      this.render();
    }, 5000);
    this.render();
  }

  private notifyError(message: string) {
    this.actionErrorMessage = message || '';
    this.showActionError = true;
    if (this.actionErrorTimer) clearTimeout(this.actionErrorTimer);
    this.actionErrorTimer = setTimeout(() => {
      this.showActionError = false;
      this.render();
    }, 5000);
    this.render();
  }

  private notifyShare(message: string) {
    this.shareNotice = message || '';
    if (this.shareTimer) clearTimeout(this.shareTimer);
    this.shareTimer = setTimeout(() => {
      this.shareNotice = null;
      this.render();
    }, 3000);
    this.render();
  }

  ngOnInit() {
    const st: any = history.state || {};
    this.pendingNoticeSuccess = typeof st.noticeSuccess === 'string' ? st.noticeSuccess : null;
    this.route.queryParamMap.subscribe(qp => {
      const from = String(qp.get('from') || '').trim();
      this.backToUrl = from.startsWith('/') ? from : '/';
    });
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadData(id);
      }
    });
  }

  ngOnDestroy() {
    if (this.successTimer) clearTimeout(this.successTimer);
    if (this.actionErrorTimer) clearTimeout(this.actionErrorTimer);
    if (this.shareTimer) clearTimeout(this.shareTimer);
    if (this.statusPollTimer) clearInterval(this.statusPollTimer);
  }

  async loadData(id: string) {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      const direct = await this.api.getListingById(id);
      if (direct) {
        this.listing = direct;
      } else {
        const allListings = await this.api.getListings();
        this.listing = allListings.find(l => String((l as any).id) === String(id)) || null;
      }
      if (this.listing) {
        this.setActiveImage(this.listing.imageUrl || (this.listing.gallery && this.listing.gallery[0]) || '');
      }
      this.currentUser = await this.api.getCurrentUser();
      await this.loadReturnState();
      this.startStatusPolling();
      if (this.pendingNoticeSuccess) {
        this.notifySuccess(this.pendingNoticeSuccess);
        this.pendingNoticeSuccess = null;
      }
      this.render();
    } catch (e) {
      this.error = e instanceof Error ? e.message : this.i18n.t('listing.error.load_failed');
      this.render();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private startStatusPolling() {
    if (this.statusPollTimer) clearInterval(this.statusPollTimer);
    const listing = this.listing;
    if (!listing) return;
    if (!this.shouldPollStatus(listing.status)) return;

    this.statusPollTimer = setInterval(async () => {
      const current = this.listing;
      if (!current) return;
      if (!this.shouldPollStatus(current.status)) {
        if (this.statusPollTimer) clearInterval(this.statusPollTimer);
        this.statusPollTimer = null;
        return;
      }
      try {
        const updated = await this.api.getListingById(current.id);
        if (updated && updated.status !== current.status) {
          this.listing = updated;
          if (updated.imageUrl) this.setActiveImage(updated.imageUrl);
          await this.loadReturnState();
          if (!this.shouldPollStatus(updated.status)) {
            if (this.statusPollTimer) clearInterval(this.statusPollTimer);
            this.statusPollTimer = null;
          }
          this.render();
        }
      } catch { }
    }, 2000);
  }

  private shouldPollStatus(status: AvailabilityStatus | null | undefined): boolean {
    return status === AvailabilityStatus.PENDING
      || status === AvailabilityStatus.APPROVED
      || status === AvailabilityStatus.READY_FOR_PICKUP
      || status === AvailabilityStatus.WAITING_FOR_RETURN
      || status === AvailabilityStatus.BORROWED
      || status === AvailabilityStatus.DISPUTED
      || status === AvailabilityStatus.PARTNER_BORROW_REQUESTED;
  }

  get isOwner(): boolean {
    return !!(this.currentUser && this.listing && this.currentUser.id === this.listing.ownerId);
  }

  get canRequestAdminReturn(): boolean {
    if (!this.isOwner || !this.listing) return false;
    return this.listing.status === AvailabilityStatus.BORROWED
      || this.listing.status === AvailabilityStatus.WAITING_FOR_RETURN
      || this.listing.status === AvailabilityStatus.DISPUTED;
  }

  get adminReturnRequested(): boolean {
    return !!this.listing?.adminReturnRequestedAt;
  }

  get isFree() {
    return isListingFree(this.listing);
  }

  get priceAmountLabel(): string {
    return this.i18n.formatPrice(getListingPrimaryRate(this.listing));
  }

  get priceUnitLabel(): string {
    if (this.listing?.type === ListingType.SELL) return '';
    const unit = getPricingUnitShort(getListingPricingUnit(this.listing));
    if (unit === '/day') return '/day';
    if (unit === '/mo') return '/month';
    return '/hour';
  }

  get additionalPriceOptions(): Array<{ label: string; value: string }> {
    return getListingAdditionalRates(this.listing).map(item => ({
      label: item.unit === 'DAILY' ? 'Daily' : item.unit === 'MONTHLY' ? 'Monthly' : 'Hourly',
      value: `${this.i18n.formatPrice(item.rate)}${getPricingUnitShort(item.unit)}`
    }));
  }

  handleInitialRequestClick() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    if (this.isOwner) {
      this.notifyError(this.i18n.t('listing.error.borrow_own'));
      return;
    }
    this.router.navigate(['/listing', listing.id, 'book'], { queryParams: { from: this.router.url } });
  }

  async reloadListing() {
    const listing = this.listing;
    if (!listing) return;
    try {
      const updated = await this.api.getListingById(listing.id);
      if (updated) {
        this.listing = updated;
        if (updated.imageUrl) this.setActiveImage(updated.imageUrl);
      }
    } catch { }
    await this.loadReturnState();
    this.startStatusPolling();
    this.render();
  }

  async handleApprove() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.actionLoading) return;
    this.actionLoading = 'APPROVE';
    this.render();
    try {
      await this.api.approveRequest(listing.id);
      await this.reloadListing();
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to approve');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  async handleDeny() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.isOwner) return;
    if (this.actionLoading) return;
    this.actionLoading = 'DENY';
    this.render();
    try {
      await this.api.denyRequest(listing.id);
      await this.reloadListing();
    } catch (e: any) {
      this.notifyError(e?.message || 'Failed to deny');
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  async handleRequestAdminReturn() {
    const listing = this.listing;
    if (!listing || !this.canRequestAdminReturn || this.adminReturnRequested) return;
    if (this.actionLoading) return;
    this.actionLoading = 'ADMIN_RETURN';
    this.render();
    try {
      await this.api.requestAdminReturn(listing.id);
      await this.reloadListing();
      this.notifySuccess(this.i18n.t('return.request_admin_unlock_sent'));
    } catch (e: any) {
      this.notifyError(e?.message || this.i18n.t('return.request_admin_unlock_failed'));
    } finally {
      this.actionLoading = null;
      this.render();
    }
  }

  backToListings() {
    this.router.navigateByUrl(this.backToUrl || '/');
  }

  get canViewReturnRequest(): boolean {
    return this.isOwner && this.returnRequestReady;
  }

  get returnActionDisabled(): boolean {
    return this.isOwner ? !this.canViewReturnRequest : this.borrowerReturnSubmitted;
  }

  returnActionLabel(): string {
    if (this.isOwner) {
      return this.canViewReturnRequest ? this.i18n.t('dash.view_return_request') : this.i18n.t('dash.waiting_for_return');
    }
    return this.borrowerReturnSubmitted ? this.i18n.t('dash.return_submitted') : this.i18n.t('dash.start_return_process');
  }

  goToReturnPage() {
    const listing = this.listing;
    if (!listing) return;
    if (this.isOwner) {
      if (!this.canViewReturnRequest) return;
      this.router.navigate(['/listing', listing.id, 'accept-return'], { queryParams: { from: this.router.url } });
      return;
    }
    if (this.borrowerReturnSubmitted) return;
    this.router.navigate(['/listing', listing.id, 'return'], { queryParams: { from: this.router.url } });
  }

  goToOwnerProfile() {
    const listing = this.listing;
    if (!listing) return;
    this.router.navigate(['/listing', listing.id, 'profile'], { queryParams: { from: this.router.url } });
  }

  goToDeleteListing() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.canOwnerDelete) return;
    this.router.navigate(['/listing', listing.id, 'delete'], { queryParams: { from: this.router.url } });
  }

  handleContact() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    const targetId = this.isOwner ? listing.borrowerId : listing.ownerId;
    const targetEmail = this.isOwner ? listing.borrower?.email : listing.owner?.email;
    if (targetEmail) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverEmail: targetEmail } });
      return;
    }
    if (targetId) {
      this.router.navigate(['/mailbox'], { queryParams: { receiverId: targetId } });
      return;
    }
    this.router.navigate(['/mailbox']);
  }

  async share() {
    const url = window.location.href;
    try {
      await navigator.clipboard.writeText(url);
      this.notifyShare(this.i18n.t('listing.share.link_copied'));
    } catch {
      this.notifyError(`${this.i18n.t('listing.share.copy_prompt')} ${url}`);
    }
  }

  private async loadReturnState() {
    const listing = this.listing;
    this.returnSession = null;
    this.returnRequestReady = false;
    this.borrowerReturnSubmitted = false;
    if (!listing) return;
    const active = listing.status === AvailabilityStatus.BORROWED
      || listing.status === AvailabilityStatus.WAITING_FOR_RETURN
      || listing.status === AvailabilityStatus.DISPUTED;
    if (!active) return;

    // Listing status already encodes whether the borrower has submitted the return.
    const submitted = listing.status === AvailabilityStatus.WAITING_FOR_RETURN;
    this.returnRequestReady = this.isOwner && submitted;
    this.borrowerReturnSubmitted = !this.isOwner && submitted;
  }
}
