import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, QrCode, AlertTriangle, CheckCircle2, RefreshCw } from 'lucide-angular';
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
    return this.isOwner ? this.session.lenderCode : this.session.borrowerCode;
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
        this.session = null;
        this.error = this.i18n.t('return.error.all_disabled');
        return;
      }
      if (item.status === AvailabilityStatus.AVAILABLE) {
        this.session = null;
        this.error = this.i18n.t('return.error.already_available');
        return;
      }
      if (item.status === AvailabilityStatus.DISPUTED) {
        this.session = null;
        this.error = this.i18n.t('return.error.disputed');
        return;
      }

      try {
        const data = await this.api.getReturnSession(item.id);
        this.session = data;
        this.error = null;
        this.notice = null;
        if (data?.status === ReturnStatus.COMPLETED) {
          this.router.navigate(['/listing', item.id, 'review'], { queryParams: { from: this.backTo } });
        }
        return;
      } catch { }

      const created = await this.api.initiateReturnSession(item.id);
      this.session = created;
      this.error = null;
      this.notice = null;
      if (created?.status === ReturnStatus.COMPLETED) {
        this.router.navigate(['/listing', item.id, 'review'], { queryParams: { from: this.backTo } });
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
      this.session = updated;
      this.error = null;
      this.notice = null;
      if (updated?.status === ReturnStatus.COMPLETED) {
        this.router.navigate(['/listing', item.id, 'review'], { queryParams: { from: this.backTo } });
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
      this.session = updated;
      this.error = null;
      if (updated?.status === ReturnStatus.COMPLETED) {
        this.router.navigate(['/listing', item.id, 'review'], { queryParams: { from: this.backTo } });
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

