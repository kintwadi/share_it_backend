import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, AlertTriangle, Ban, Loader2, RefreshCcw, Trash2, Shield } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, User } from '../../core/models/types';

type AdminTab = 'OVERVIEW' | 'USERS' | 'LISTINGS' | 'PARTNER_LISTINGS' | 'TRANSACTIONS' | 'SUBSCRIPTIONS' | 'DISPUTES' | 'REPORTS' | 'SETTINGS';
type PartnerSubTab = 'SUBMISSIONS' | 'BORROW_REQUESTS' | 'ITEMS';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly AlertTriangle = AlertTriangle;
  readonly Ban = Ban;
  readonly Loader2 = Loader2;
  readonly RefreshCcw = RefreshCcw;
  readonly Trash2 = Trash2;
  readonly Shield = Shield;

  activeTab: AdminTab = 'OVERVIEW';
  loading = false;
  error: string | null = null;
  currentUser: User | null = null;
  isPartnerScopedAdmin = false;

  summary: any = null;

  usersQ = '';
  users: any[] = [];
  usersTotal = 0;
  usersPage = 0;

  listings: any[] = [];
  listingsTotal = 0;
  listingsPage = 0;
  listingsStatus = '';

  partnerSubTab: PartnerSubTab = 'SUBMISSIONS';
  partnerSubmissions: any[] = [];
  partnerSubmissionsTotal = 0;
  partnerSubmissionsPage = 0;

  partnerBorrowRequests: any[] = [];
  partnerBorrowRequestsTotal = 0;
  partnerBorrowRequestsPage = 0;

  partnerItems: any[] = [];
  partnerItemsTotal = 0;
  partnerItemsPage = 0;
  partnerItemsStatus = '';
  partnerActivateLoadingId: string | null = null;

  partnerListingSelectedId: string | null = null;
  partnerListingSelected: Listing | null = null;
  partnerListingSelectedLoading = false;

  transactions: any[] = [];
  transactionsTotal = 0;
  transactionsPage = 0;
  transactionsStatus = '';

  subscriptions: any[] = [];
  subscriptionsTotal = 0;
  subscriptionsPage = 0;
  subscriptionsStatus = '';

  disputes: any[] = [];
  disputesTotal = 0;
  disputesPage = 0;

  reports: any[] = [];
  reportsLoading = false;

  settingsSections: any[] = [];
  settingsOriginal: Record<string, any> = {};
  settingsSaving = false;
  settingsSaved = false;

  pageSize = 20;

  listingsStatusOptions = ['', 'AVAILABLE', 'BORROWED', 'PENDING', 'APPROVED', 'SCHEDULED', 'BLOCKED', 'HIDDEN', 'DISPUTED', 'SOLD', 'GIFTED', 'PARTNER_INACTIVE', 'PARTNER_ACTIVE', 'PARTNER_BORROW_REQUESTED'];
  txStatusOptions = ['', 'ESCROWED', 'RELEASED', 'RELEASE_FAILED', 'DISPUTED', 'REFUNDED', 'PENDING', 'FAILED'];
  subStatusOptions = ['', 'active', 'trialing', 'past_due', 'canceled', 'incomplete'];

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    const u = await this.api.getCurrentUser();
    if (!u) {
      this.router.navigate(['/connect']);
      return;
    }
    const role = String((u as any).role ?? '').toUpperCase();
    const isAdmin = role === 'ADMIN' || role === 'ROLE_ADMIN';
    if (!isAdmin) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.currentUser = u;
    this.isPartnerScopedAdmin = String((u as any).adminScope ?? '').toUpperCase() === 'PARTNER';
    if (this.isPartnerScopedAdmin) {
      this.activeTab = 'PARTNER_LISTINGS';
      this.partnerSubTab = 'BORROW_REQUESTS';
    } else {
      this.partnerSubTab = 'ITEMS';
      this.partnerItemsStatus = '';
    }
    await this.refreshActive();
  }

  async setTab(tab: AdminTab) {
    if (this.isPartnerScopedAdmin && tab !== 'PARTNER_LISTINGS') return;
    this.activeTab = tab;
    await this.refreshActive();
  }

  async refreshActive() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      if (!this.isPartnerScopedAdmin) {
        await this.loadSummary();
      }
      if (this.activeTab === 'PARTNER_LISTINGS') {
        await this.loadPartnerSection();
      }
      if (!this.isPartnerScopedAdmin) {
        if (this.activeTab === 'USERS') await this.loadUsers(this.usersPage);
        if (this.activeTab === 'LISTINGS') await this.loadListings(this.listingsPage);
        if (this.activeTab === 'TRANSACTIONS') await this.loadTransactions(this.transactionsPage);
        if (this.activeTab === 'SUBSCRIPTIONS') await this.loadSubscriptions(this.subscriptionsPage);
        if (this.activeTab === 'DISPUTES') await this.loadDisputes(this.disputesPage);
        if (this.activeTab === 'REPORTS') await this.loadReports();
        if (this.activeTab === 'SETTINGS') await this.loadAppSettings();
      }
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : this.i18n.t('admin.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async loadAppSettings() {
    const res = await this.api.adminGetAppSettings();
    const sections = Array.isArray(res?.sections) ? res.sections : [];
    this.settingsSections = sections;
    const original: Record<string, any> = {};
    for (const s of sections) {
      const items = Array.isArray(s?.items) ? s.items : [];
      for (const it of items) {
        if (it?.key) {
          original[String(it.key)] = it.value;
        }
      }
    }
    this.settingsOriginal = original;
    this.settingsSaved = false;
    this.settingsSaving = false;
    this.render();
  }

  private isDirtySetting(item: any): boolean {
    const key = String(item?.key || '');
    if (!key) return false;
    return JSON.stringify(this.settingsOriginal[key]) !== JSON.stringify(item?.value);
  }

  resetSetting(item: any) {
    if (!item) return;
    item.value = item.defaultValue;
    this.settingsSaved = false;
    this.render();
  }

  async saveSettings() {
    if (this.settingsSaving) return;
    this.settingsSaving = true;
    this.settingsSaved = false;
    this.error = null;
    this.render();
    try {
      const updates: { key: string; value: any }[] = [];
      for (const section of this.settingsSections) {
        const items = Array.isArray(section?.items) ? section.items : [];
        for (const it of items) {
          if (!it?.key) continue;
          if (!this.isDirtySetting(it)) continue;
          const isOverridden = !!it.overridden;
          const nowEqualsDefault = JSON.stringify(it.value) === JSON.stringify(it.defaultValue);
          updates.push({ key: String(it.key), value: isOverridden && nowEqualsDefault ? null : it.value });
        }
      }
      await this.api.adminUpdateAppSettings(updates);
      await this.loadAppSettings();
      this.settingsSaved = true;
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('admin.error.load_failed');
    } finally {
      this.settingsSaving = false;
      this.render();
      if (this.settingsSaved) {
        setTimeout(() => {
          this.settingsSaved = false;
          this.render();
        }, 2000);
      }
    }
  }

  async loadSummary() {
    this.summary = await this.api.getAdminSummary();
  }

  async loadUsers(page: number) {
    const res = await this.api.adminListUsers({ q: this.usersQ || undefined, page, size: this.pageSize });
    this.users = Array.isArray(res?.items) ? res.items : [];
    this.usersTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.usersPage = page;
  }

  async loadListings(page: number) {
    const res = await this.api.adminListListings({ status: this.listingsStatus || undefined, page, size: this.pageSize });
    this.listings = Array.isArray(res?.items) ? res.items : [];
    this.listingsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.listingsPage = page;
  }

  get partnerRows(): any[] {
    if (this.partnerSubTab === 'SUBMISSIONS') return this.partnerSubmissions;
    if (this.partnerSubTab === 'BORROW_REQUESTS') return this.partnerBorrowRequests;
    return this.partnerItems;
  }

  get partnerTotal(): number {
    if (this.partnerSubTab === 'SUBMISSIONS') return this.partnerSubmissionsTotal;
    if (this.partnerSubTab === 'BORROW_REQUESTS') return this.partnerBorrowRequestsTotal;
    return this.partnerItemsTotal;
  }

  get partnerPage(): number {
    if (this.partnerSubTab === 'SUBMISSIONS') return this.partnerSubmissionsPage;
    if (this.partnerSubTab === 'BORROW_REQUESTS') return this.partnerBorrowRequestsPage;
    return this.partnerItemsPage;
  }

  async setPartnerSubTab(tab: PartnerSubTab) {
    this.partnerSubTab = tab;
    this.partnerListingSelectedId = null;
    this.partnerListingSelected = null;
    await this.loadPartnerSection(0);
  }

  async loadPartnerSection(page?: number) {
    if (this.partnerSubTab === 'SUBMISSIONS') {
      await this.loadPartnerSubmissions(typeof page === 'number' ? page : this.partnerSubmissionsPage);
      return;
    }
    if (this.partnerSubTab === 'BORROW_REQUESTS') {
      await this.loadPartnerBorrowRequests(typeof page === 'number' ? page : this.partnerBorrowRequestsPage);
      return;
    }
    await this.loadPartnerItems(typeof page === 'number' ? page : this.partnerItemsPage);
  }

  async loadPartnerSubmissions(page: number) {
    const res = await this.api.adminListPartnerSubmissions({ page, size: this.pageSize });
    this.partnerSubmissions = Array.isArray(res?.items) ? res.items : [];
    this.partnerSubmissionsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.partnerSubmissionsPage = page;
    await this.ensurePartnerSelection();
  }

  async loadPartnerBorrowRequests(page: number) {
    const res = await this.api.adminListPartnerBorrowRequests({ page, size: this.pageSize });
    this.partnerBorrowRequests = Array.isArray(res?.items) ? res.items : [];
    this.partnerBorrowRequestsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.partnerBorrowRequestsPage = page;
    await this.ensurePartnerSelection();
  }

  async loadPartnerItems(page: number) {
    const status = undefined;
    const res = await this.api.adminListPartnerItems({ status, page, size: this.pageSize });
    this.partnerItems = Array.isArray(res?.items) ? res.items : [];
    this.partnerItemsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.partnerItemsPage = page;
    await this.ensurePartnerSelection();
  }

  canTogglePartnerActive(row: any): boolean {
    if (this.isPartnerScopedAdmin) return false;
    const st = String(row?.status || '');
    if (st === 'PARTNER_INACTIVE') return true;
    if (st === 'PARTNER_ACTIVE') return true;
    return false;
  }

  async togglePartnerActive(row: any, checked: boolean) {
    if (!this.canTogglePartnerActive(row)) return;
    const id = String(row?.id || '');
    if (!id) return;

    this.partnerActivateLoadingId = id;
    this.error = null;
    this.render();
    try {
      if (checked) {
        await this.api.adminActivatePartnerItem(id);
      } else {
        await this.api.adminDeactivatePartnerItem(id);
      }
      const nextStatus = checked ? 'PARTNER_ACTIVE' : 'PARTNER_INACTIVE';
      this.partnerItems = this.partnerItems.map(x => String(x?.id) === id ? { ...x, status: nextStatus } : x);
      this.partnerSubmissions = this.partnerSubmissions.map(x => String(x?.id) === id ? { ...x, status: nextStatus } : x);
      if (this.partnerListingSelectedId === id && this.partnerListingSelected) {
        this.partnerListingSelected = { ...(this.partnerListingSelected as any), status: nextStatus } as any;
      }
      if (!this.isPartnerScopedAdmin) {
        await this.loadSummary();
      }
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : (e?.message || 'Action failed.');
    } finally {
      this.partnerActivateLoadingId = null;
      this.render();
    }
  }

  private async ensurePartnerSelection() {
    const rows = this.partnerRows;
    if (this.partnerListingSelectedId && !rows.some(r => String(r?.id) === String(this.partnerListingSelectedId))) {
      this.partnerListingSelectedId = null;
      this.partnerListingSelected = null;
    }
    if (!this.partnerListingSelectedId && rows.length > 0) {
      await this.selectPartnerListing(rows[0]);
    }
  }

  async selectPartnerListing(row: any) {
    const id = String(row?.id || '');
    if (!id) return;
    this.partnerListingSelectedId = id;
    this.partnerListingSelected = null;
    this.partnerListingSelectedLoading = true;
    this.render();
    try {
      const listing = await this.api.getListingById(id);
      this.partnerListingSelected = listing;
    } finally {
      this.partnerListingSelectedLoading = false;
      this.render();
    }
  }

  async loadTransactions(page: number) {
    const res = await this.api.adminListTransactions({ status: this.transactionsStatus || undefined, page, size: this.pageSize });
    this.transactions = Array.isArray(res?.items) ? res.items : [];
    this.transactionsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.transactionsPage = page;
  }

  async loadSubscriptions(page: number) {
    const res = await this.api.adminListSubscriptions({ status: this.subscriptionsStatus || undefined, page, size: this.pageSize });
    this.subscriptions = Array.isArray(res?.items) ? res.items : [];
    this.subscriptionsTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.subscriptionsPage = page;
  }

  async loadDisputes(page: number) {
    const res = await this.api.adminListDisputes({ page, size: this.pageSize });
    this.disputes = Array.isArray(res?.items) ? res.items : [];
    this.disputesTotal = typeof res?.total === 'number' ? res.total : Number(res?.total || 0);
    this.disputesPage = page;
  }

  async loadReports() {
    this.reportsLoading = true;
    this.render();
    try {
      const res = await this.api.adminListReports({ page: 0, size: 50 });
      this.reports = Array.isArray(res?.items) ? res.items : [];
    } catch {
      this.reports = [];
    } finally {
      this.reportsLoading = false;
      this.render();
    }
  }

  openConfirm(opts: { title: string; message: string; variant?: 'danger' | 'warning' | 'info'; confirmLabel?: string; action: string; payload: any }) {
    this.router.navigate(['/admin/confirm'], {
      queryParams: { from: this.router.url },
      state: {
        title: opts.title,
        message: opts.message,
        variant: opts.variant ?? 'info',
        confirmLabel: opts.confirmLabel ?? this.i18n.t('admin.confirm'),
        action: opts.action,
        payload: opts.payload ?? null,
      } as any
    });
  }

  confirmUserStatus(u: any, status: string) {
    this.openConfirm({
      title: status === 'BLOCKED' ? 'Block user?' : 'Unblock user?',
      message: String(u?.email || ''),
      variant: 'warning',
      action: 'user-status',
      payload: { userId: String(u?.id || ''), status }
    });
  }

  confirmApproveVerification(u: any) {
    this.openConfirm({
      title: this.i18n.t('admin.confirm.approve_verification_title'),
      message: `${this.i18n.t('admin.confirm.approve_verification_msg')} ${u.email || u.displayName || u.name}?`,
      variant: 'warning',
      confirmLabel: this.i18n.t('admin.action.approve'),
      action: 'approve-verification',
      payload: { userId: String(u?.id || '') }
    });
  }

  confirmRevokeVerification(u: any) {
    this.openConfirm({
      title: this.i18n.t('admin.confirm.revoke_verification_title'),
      message: `${this.i18n.t('admin.confirm.revoke_verification_msg')} ${u.email || u.displayName || u.name}?`,
      variant: 'warning',
      confirmLabel: this.i18n.t('admin.action.revoke'),
      action: 'revoke-verification',
      payload: { userId: String(u?.id || '') }
    });
  }

  confirmDeleteUser(u: any) {
    this.openConfirm({
      title: 'Delete user?',
      message: `This will attempt to delete ${u?.email || ''}. If they have related data, you must block instead.`,
      variant: 'danger',
      action: 'delete-user',
      payload: { userId: String(u?.id || '') }
    });
  }

  confirmBlockListing(l: any, blocked: boolean) {
    this.openConfirm({
      title: blocked ? 'Block listing?' : 'Unblock listing?',
      message: String(l?.title || ''),
      variant: 'warning',
      action: 'block-listing',
      payload: { listingId: String(l?.id || ''), blocked: !!blocked }
    });
  }

  confirmDeleteListing(l: any) {
    this.openConfirm({
      title: 'Delete listing?',
      message: String(l?.title || ''),
      variant: 'danger',
      action: 'delete-listing',
      payload: { listingId: String(l?.id || '') }
    });
  }

  confirmApprovePartnerListing(l: any) {
    this.openConfirm({
      title: 'Approve partner listing?',
      message: String(l?.title || ''),
      variant: 'warning',
      confirmLabel: 'Approve',
      action: 'approve-partner-listing',
      payload: { listingId: String(l?.id || '') }
    });
  }

  confirmRejectPartnerListing(l: any) {
    this.openConfirm({
      title: 'Reject partner listing?',
      message: String(l?.title || ''),
      variant: 'danger',
      confirmLabel: 'Reject',
      action: 'reject-partner-listing',
      payload: { listingId: String(l?.id || '') }
    });
  }

  confirmApprovePartnerListingRequest(l: any) {
    this.openConfirm({
      title: 'Approve partner listing?',
      message: String(l?.title || ''),
      variant: 'warning',
      confirmLabel: 'Approve',
      action: 'approve-partner-submission',
      payload: { submissionId: String(l?.id || '') }
    });
  }

  confirmRejectPartnerListingRequest(l: any) {
    this.openConfirm({
      title: 'Reject partner listing?',
      message: String(l?.title || ''),
      variant: 'danger',
      confirmLabel: 'Reject',
      action: 'reject-partner-submission',
      payload: { submissionId: String(l?.id || '') }
    });
  }

  confirmApprovePartnerBorrowRequest(l: any) {
    this.openConfirm({
      title: 'Approve borrow request?',
      message: String(l?.title || ''),
      variant: 'warning',
      confirmLabel: 'Approve',
      action: 'approve-partner-borrow',
      payload: { requestId: String(l?.id || '') }
    });
  }

  confirmRejectPartnerBorrowRequest(l: any) {
    this.openConfirm({
      title: 'Reject borrow request?',
      message: String(l?.title || ''),
      variant: 'danger',
      confirmLabel: 'Reject',
      action: 'reject-partner-borrow',
      payload: { requestId: String(l?.id || '') }
    });
  }

  confirmDeleteTransaction(t: any) {
    this.openConfirm({
      title: 'Delete transaction?',
      message: String(t?.id || ''),
      variant: 'danger',
      action: 'delete-transaction',
      payload: { txId: String(t?.id || '') }
    });
  }

  confirmRetryRelease(t: any) {
    this.openConfirm({
      title: 'Retry release?',
      message: String(t?.id || ''),
      variant: 'warning',
      action: 'retry-release',
      payload: { txId: String(t?.id || '') }
    });
  }

  confirmAcceptReturn(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Accept return?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: 'accept-return',
      payload: { listingId, reason: 'admin_accept_return' }
    });
  }

  confirmReopenReturn(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Reopen return window?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: 'reopen-return',
      payload: { listingId, days: 10 }
    });
  }

  confirmCancelRefund(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Cancel & refund?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: 'cancel-refund',
      payload: { listingId, reason: 'admin_cancel_refund' }
    });
  }

  confirmDeleteReport(r: any) {
    this.openConfirm({
      title: 'Dismiss report?',
      message: String(r?.reason || ''),
      variant: 'warning',
      action: 'dismiss-report',
      payload: { reportId: String(r?.id || '') }
    });
  }
}
