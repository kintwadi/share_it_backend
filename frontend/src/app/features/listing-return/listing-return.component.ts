import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, QrCode, AlertTriangle, CheckCircle2, RefreshCw } from 'lucide-angular';
import * as QRCodeGen from 'qrcode';
import { ApiService } from '../../core/services/api.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { AvailabilityStatus, Listing, ReturnSessionResponse, ReturnStatus, User } from '../../core/models/types';
import { I18nService } from '../../core/services/i18n.service';

type ReturnTab = 'qr' | 'scan' | 'manual' | 'dispute';

@Component({
  selector: 'app-listing-return',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-return.component.html',
  styleUrl: './listing-return.component.css'
})
export class ListingReturnComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private settings = inject(SettingsConfigService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly QrCode = QrCode;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly RefreshCw = RefreshCw;

  item: Listing | null = null;
  currentUser: User | null = null;

  session: ReturnSessionResponse | null = null;
  qrCodeImageUrl: string | null = null;
  loading = true;
  error: string | null = null;
  notice: string | null = null;

  activeTab: ReturnTab = 'qr';
  scannedCode = '';
  itemNumber = '';
  conciergeId = '';
  disputeReason = '';

  private pollId: any = null;
  private backTo = '/dashboard';
  private renderedQrValue = '';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get isOwner(): boolean {
    if (!this.item || !this.currentUser) return false;
    return this.item.ownerId === this.currentUser.id;
  }

  get qrEnabled(): boolean {
    return this.settings.isSectionEnabled('returns', 'qr');
  }

  get manualEnabled(): boolean {
    return this.settings.isSectionEnabled('returns', 'manual');
  }

  get disputeEnabled(): boolean {
    return this.settings.isSectionEnabled('returns', 'dispute');
  }

  get anyEnabled(): boolean {
    return this.qrEnabled || this.manualEnabled || this.disputeEnabled;
  }

  get allowedTabs(): ReturnTab[] {
    const tabs: ReturnTab[] = [];
    if (this.qrEnabled) tabs.push('qr', 'scan');
    if (this.manualEnabled) tabs.push('manual');
    if (this.disputeEnabled) tabs.push('dispute');
    return tabs;
  }

  get myQrCode(): string | undefined {
    if (!this.session) return undefined;
    return (this.isOwner ? this.session.lenderCode : this.session.borrowerCode) ?? undefined;
  }

  private async setSession(session: ReturnSessionResponse | null) {
    this.session = session;
    await this.refreshQrCodeImage();
  }

  private async refreshQrCodeImage() {
    const qrValue = String(this.myQrCode || '').trim();
    if (!qrValue) {
      this.qrCodeImageUrl = null;
      this.renderedQrValue = '';
      return;
    }
    if (this.renderedQrValue === qrValue && this.qrCodeImageUrl) return;
    try {
      this.qrCodeImageUrl = await QRCodeGen.toDataURL(qrValue, {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 320,
        color: {
          dark: '#111827',
          light: '#FFFFFFFF'
        }
      });
      this.renderedQrValue = qrValue;
    } catch {
      this.qrCodeImageUrl = null;
      this.renderedQrValue = '';
    }
  }

  get myScanStatus(): boolean {
    if (!this.session) return false;
    return this.isOwner ? this.session.lenderScanned : this.session.borrowerScanned;
  }

  get theirScanStatus(): boolean {
    if (!this.session) return false;
    return this.isOwner ? this.session.borrowerScanned : this.session.lenderScanned;
  }

  get myStatusBadgeClass(): string {
    return this.myScanStatus ? 'badge--on' : 'badge--waiting-self';
  }

  get theirStatusBadgeClass(): string {
    return this.theirScanStatus ? 'badge--on' : 'badge--waiting-other';
  }

  get canInitiateReturn(): boolean {
    return !this.isOwner;
  }

  get canSubmitManual(): boolean {
    if (this.loading || !this.itemNumber) return false;
    if (!this.session) return false;
    if (!this.isOwner) return !this.session.manualBorrowerConfirmed;
    return this.session.manualBorrowerConfirmed && !this.session.manualLenderConfirmed;
  }

  get manualActionLabel(): string {
    return this.i18n.t(this.isOwner ? 'return.manual_action_accept' : 'return.manual_action_start');
  }

  get lenderDisplayName(): string {
    const item = this.item as any;
    return String(item?.owner?.name || item?.partnerName || this.i18n.t('return.fallback_lender')).trim();
  }

  get borrowerDisplayName(): string {
    const item = this.item as any;
    return String(item?.borrower?.name || this.i18n.t('return.fallback_borrower')).trim();
  }

  get borrowerWaitingForLenderMessage(): string | null {
    if (this.isOwner || !this.session) return null;
    if (!this.session.manualBorrowerConfirmed || this.session.manualLenderConfirmed) return null;
    return this.i18n.t('return.waiting_for_lender_accept').replace('{name}', this.lenderDisplayName);
  }

  get lenderWaitingForBorrowerMessage(): string | null {
    if (!this.isOwner) return null;
    if (this.session) return null;
    if (this.error) return null;
    if (!this.item) return null;
    return this.i18n.t('return.waiting_for_borrower_start').replace('{name}', this.borrowerDisplayName);
  }

  ngOnInit() {
    this.settings.ensureLoaded();
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/dashboard';
    this.ensureTabAllowed();
    this.init();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  back() {
    this.router.navigateByUrl(this.backTo);
  }

  private goToReview(listingId: string) {
    this.stopPolling();
    this.router.navigate(['/listing', listingId, 'review'], { queryParams: { from: this.backTo } });
  }

  private async init() {
    const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
    if (!id) {
      this.back();
      return;
    }
    try {
      this.loading = true;
      this.render();
      const [item, me] = await Promise.all([
        this.api.getListingById(id),
        this.api.getCurrentUser()
      ]);
      this.item = item;
      this.currentUser = me;
      if (!me) {
        this.router.navigate(['/connect']);
        return;
      }
      await this.fetchSession();
      this.startPolling();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  setTab(tab: ReturnTab) {
    this.activeTab = tab;
    if (tab === 'manual' && !this.itemNumber && (this.item?.itemReference || this.item?.id)) {
      this.itemNumber = String((this.item as any).itemReference || this.item.id);
    }
    this.ensureTabAllowed();
    this.render();
  }

  private ensureTabAllowed() {
    const allowed = this.allowedTabs;
    if (allowed.length === 0) {
      this.activeTab = 'qr';
      return;
    }
    if (!allowed.includes(this.activeTab)) {
      this.activeTab = allowed[0];
    }
  }

  private startPolling() {
    if (this.pollId) return;
    if (!this.item) return;
    if (!this.anyEnabled) return;
    if (this.item.status === AvailabilityStatus.DISPUTED) return;
    this.pollId = setInterval(() => this.fetchSession(), 5000);
  }

  private stopPolling() {
    if (this.pollId) {
      clearInterval(this.pollId);
      this.pollId = null;
    }
  }

  async fetchSession() {
    const item = this.item;
    const currentUser = this.currentUser;
    if (!item || !currentUser) return;

    try {
      this.loading = true;
      this.render();

      if (!this.anyEnabled) {
        await this.setSession(null);
        this.error = this.i18n.t('return.error.all_disabled');
        return;
      }
      if (item.status === AvailabilityStatus.DISPUTED) {
        await this.setSession(null);
        this.error = this.i18n.t('return.error.disputed');
        return;
      }

      try {
        const data = await this.api.getReturnSession(item.id);
        await this.setSession(data);
        this.error = null;
        this.notice = null;
        if (data?.status === ReturnStatus.COMPLETED) {
          this.goToReview(item.id);
        }
        return;
      } catch {
        try {
          const refreshed = await this.api.getListingById(item.id);
          this.item = refreshed;
          if (this.session && refreshed?.status === AvailabilityStatus.AVAILABLE) {
            this.error = null;
            this.notice = null;
            this.goToReview(item.id);
            return;
          }
          if (refreshed?.status === AvailabilityStatus.AVAILABLE) {
            await this.setSession(null);
            this.error = this.i18n.t('return.error.already_available');
            return;
          }
          if (refreshed?.status === AvailabilityStatus.DISPUTED) {
            await this.setSession(null);
            this.error = this.i18n.t('return.error.disputed');
            return;
          }
        } catch { }
      }

      if (!this.canInitiateReturn) {
        await this.setSession(null);
        this.notice = null;
        this.error = null;
        return;
      }

      const created = await this.api.initiateReturnSession(item.id);
      await this.setSession(created);
      this.error = null;
      this.notice = null;
      if (created?.status === ReturnStatus.COMPLETED) {
        this.goToReview(item.id);
      }
    } catch (err: any) {
      const msg = err?.message || this.i18n.t('return.error.load_failed');
      if (typeof msg === 'string' && msg.includes('Listing is not currently borrowed')) {
        this.error = this.i18n.t('return.error.not_borrowed');
      } else if (typeof msg === 'string' && msg.includes('Listing already returned')) {
        this.error = this.i18n.t('return.error.already_returned');
      } else {
        this.error = msg;
      }
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async handleScan() {
    const item = this.item;
    if (!item) return;
    if (!this.scannedCode || this.scannedCode.length !== 6) return;
    try {
      this.loading = true;
      this.render();
      const updated = await this.api.scanReturnQrCode(item.id, this.scannedCode);
      await this.setSession(updated);
      this.error = null;
      this.notice = null;
      if (updated?.status === ReturnStatus.COMPLETED) {
        this.goToReview(item.id);
      }
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('return.error.verify_failed');
    } finally {
      this.loading = false;
      this.scannedCode = '';
      this.render();
    }
  }

  async handleManualFallback() {
    const item = this.item;
    if (!item) return;
    if (!this.canSubmitManual) return;
    try {
      this.loading = true;
      this.render();
      const updated = await this.api.manualReturnFallback(item.id, this.itemNumber, this.conciergeId || undefined);
      await this.setSession(updated);
      this.error = null;
      if (updated?.status === ReturnStatus.COMPLETED) {
        this.goToReview(item.id);
      } else {
        this.notice = this.i18n.t('return.notice.manual_submitted');
      }
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('return.error.manual_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }
}

