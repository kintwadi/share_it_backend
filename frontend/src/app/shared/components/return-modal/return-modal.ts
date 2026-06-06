import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, X, QrCode, AlertTriangle, CheckCircle2, RefreshCw } from 'lucide-angular';
import * as QRCodeGen from 'qrcode';
import { ApiService } from '../../../core/services/api.service';
import { SettingsConfigService } from '../../../core/services/settings-config.service';
import { AvailabilityStatus, Listing, ReturnSessionResponse, ReturnStatus, User } from '../../../core/models/types';
import { I18nService } from '../../../core/services/i18n.service';

type ReturnTab = 'qr' | 'scan' | 'manual' | 'dispute';

@Component({
  selector: 'app-return-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './return-modal.html',
  styleUrl: './return-modal.css'
})
export class ReturnModalComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private settings = inject(SettingsConfigService);
  private cdr = inject(ChangeDetectorRef);

  @Input() item: Listing | null = null;
  @Input() currentUser: User | null = null;
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() complete = new EventEmitter<void>();

  readonly X = X;
  readonly QrCode = QrCode;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly RefreshCw = RefreshCw;
  i18n = inject(I18nService);

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

  ngOnInit() {
    this.settings.ensureLoaded();
    this.ensureTabAllowed();
    if (this.isOpen) {
      this.fetchSession();
      this.startPolling();
    }
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  ngOnChanges() {
    if (!this.isOpen) {
      this.stopPolling();
      return;
    }
    this.ensureTabAllowed();
    this.fetchSession();
    this.startPolling();
  }

  setTab(tab: ReturnTab) {
    this.activeTab = tab;
    if (tab === 'manual' && !this.itemNumber && (this.item?.itemReference || this.item?.id)) {
      this.itemNumber = String(this.item.itemReference || this.item.id);
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
      if (item.status === AvailabilityStatus.AVAILABLE) {
        await this.setSession(null);
        this.error = this.i18n.t('return.error.already_available');
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
          this.complete.emit();
        }
        return;
      } catch { }

      const created = await this.api.initiateReturnSession(item.id);
      await this.setSession(created);
      this.error = null;
      this.notice = null;
      if (created?.status === ReturnStatus.COMPLETED) {
        this.complete.emit();
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
        this.complete.emit();
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
    if (!this.itemNumber) return;
    try {
      this.loading = true;
      this.render();
      const updated = await this.api.manualReturnFallback(item.id, this.itemNumber, this.conciergeId || undefined);
      await this.setSession(updated);
      this.error = null;
      if (updated?.status === ReturnStatus.COMPLETED) {
        this.complete.emit();
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

  async handleDispute() {
    const item = this.item;
    if (!item) return;
    if (!this.disputeReason) return;
    try {
      this.loading = true;
      this.render();
      const updated = await this.api.initiateReturnDispute(item.id, this.disputeReason, undefined, this.conciergeId || undefined);
      await this.setSession(updated);
      this.error = null;
      this.notice = this.i18n.t('return.notice.dispute_started');
      this.close.emit();
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('return.error.dispute_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  onClose() {
    this.close.emit();
  }

  onScannedCodeChange(val: string) {
    this.scannedCode = (val || '').replace(/\D/g, '').slice(0, 6);
  }

  useListingIdAsItemNumber() {
    if (!this.item?.id) return;
    this.itemNumber = String(this.item.itemReference || this.item.id);
    this.render();
  }

  async copyListingId() {
    const ref = this.item?.id ? String(this.item.itemReference || this.item.id) : '';
    if (!ref) return;
    try {
      await navigator.clipboard.writeText(ref);
      this.notice = this.i18n.t('common.copied');
    } catch {
      this.notice = ref;
    } finally {
      this.render();
    }
  }
}
