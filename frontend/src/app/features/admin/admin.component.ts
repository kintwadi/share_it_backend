import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, AlertTriangle, Ban, Loader2, RefreshCcw, Trash2, Shield } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User } from '../../core/models/types';
import { ConfirmationModalComponent } from '../../shared/components/confirmation-modal/confirmation-modal';

type AdminTab = 'OVERVIEW' | 'USERS' | 'LISTINGS' | 'TRANSACTIONS' | 'SUBSCRIPTIONS' | 'DISPUTES' | 'REPORTS';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ConfirmationModalComponent],
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

  summary: any = null;

  usersQ = '';
  users: any[] = [];
  usersTotal = 0;
  usersPage = 0;

  listings: any[] = [];
  listingsTotal = 0;
  listingsPage = 0;
  listingsStatus = '';

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

  confirmOpen = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmVariant: 'danger' | 'warning' | 'info' = 'info';
  confirmLabel = this.i18n.t('common.confirm');
  confirmLoading = false;
  confirmAction: (() => Promise<void>) | null = null;

  pageSize = 20;

  listingsStatusOptions = ['', 'AVAILABLE', 'BORROWED', 'PENDING', 'APPROVED', 'SCHEDULED', 'BLOCKED', 'HIDDEN', 'DISPUTED', 'SOLD', 'GIFTED'];
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
    await this.refreshActive();
  }

  async setTab(tab: AdminTab) {
    this.activeTab = tab;
    await this.refreshActive();
  }

  async refreshActive() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      await this.loadSummary();
      if (this.activeTab === 'USERS') await this.loadUsers(this.usersPage);
      if (this.activeTab === 'LISTINGS') await this.loadListings(this.listingsPage);
      if (this.activeTab === 'TRANSACTIONS') await this.loadTransactions(this.transactionsPage);
      if (this.activeTab === 'SUBSCRIPTIONS') await this.loadSubscriptions(this.subscriptionsPage);
      if (this.activeTab === 'DISPUTES') await this.loadDisputes(this.disputesPage);
      if (this.activeTab === 'REPORTS') await this.loadReports();
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : this.i18n.t('admin.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
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

  openConfirm(opts: { title: string; message: string; variant?: 'danger' | 'warning' | 'info'; confirmLabel?: string; action: () => Promise<void> }) {
    this.confirmTitle = opts.title;
    this.confirmMessage = opts.message;
    this.confirmVariant = opts.variant ?? 'info';
    this.confirmLabel = opts.confirmLabel ?? this.i18n.t('admin.confirm');
    this.confirmAction = opts.action;
    this.confirmOpen = true;
    this.confirmLoading = false;
    this.render();
  }

  closeConfirm() {
    this.confirmOpen = false;
    this.confirmAction = null;
    this.confirmLoading = false;
    this.render();
  }

  async confirmDo() {
    if (!this.confirmAction) return;
    this.confirmLoading = true;
    this.render();
    try {
      await this.confirmAction();
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : (e?.message || 'Action failed.');
    } finally {
      this.confirmLoading = false;
      this.closeConfirm();
      this.render();
    }
  }

  confirmUserStatus(u: any, status: string) {
    this.openConfirm({
      title: status === 'BLOCKED' ? 'Block user?' : 'Unblock user?',
      message: String(u?.email || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminSetUserStatus(String(u.id), status);
        await this.loadUsers(this.usersPage);
      }
    });
  }

  confirmApproveVerification(u: any) {
    this.openConfirm({
      title: this.i18n.t('admin.confirm.approve_verification_title'),
      message: `${this.i18n.t('admin.confirm.approve_verification_msg')} ${u.email || u.displayName || u.name}?`,
      variant: 'warning',
      confirmLabel: this.i18n.t('admin.action.approve'),
      action: async () => {
        await this.api.approveVerification(String(u.id));
      }
    });
  }

  confirmRevokeVerification(u: any) {
    this.openConfirm({
      title: this.i18n.t('admin.confirm.revoke_verification_title'),
      message: `${this.i18n.t('admin.confirm.revoke_verification_msg')} ${u.email || u.displayName || u.name}?`,
      variant: 'warning',
      confirmLabel: this.i18n.t('admin.action.revoke'),
      action: async () => {
        await this.api.revokeVerification(String(u.id));
      }
    });
  }

  confirmDeleteUser(u: any) {
    this.openConfirm({
      title: 'Delete user?',
      message: `This will attempt to delete ${u?.email || ''}. If they have related data, you must block instead.`,
      variant: 'danger',
      action: async () => {
        await this.api.adminDeleteUser(String(u.id));
        await this.loadUsers(this.usersPage);
      }
    });
  }

  confirmBlockListing(l: any, blocked: boolean) {
    this.openConfirm({
      title: blocked ? 'Block listing?' : 'Unblock listing?',
      message: String(l?.title || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminBlockListing(String(l.id), blocked);
        await this.loadListings(this.listingsPage);
      }
    });
  }

  confirmDeleteListing(l: any) {
    this.openConfirm({
      title: 'Delete listing?',
      message: String(l?.title || ''),
      variant: 'danger',
      action: async () => {
        await this.api.adminDeleteListing(String(l.id));
        await this.loadListings(this.listingsPage);
      }
    });
  }

  confirmDeleteTransaction(t: any) {
    this.openConfirm({
      title: 'Delete transaction?',
      message: String(t?.id || ''),
      variant: 'danger',
      action: async () => {
        await this.api.adminDeleteTransaction(String(t.id));
        await this.loadTransactions(this.transactionsPage);
      }
    });
  }

  confirmRetryRelease(t: any) {
    this.openConfirm({
      title: 'Retry release?',
      message: String(t?.id || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminRetryTransactionRelease(String(t.id));
        await Promise.all([this.loadTransactions(this.transactionsPage), this.loadSummary()]);
      }
    });
  }

  confirmAcceptReturn(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Accept return?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminAcceptReturnDispute(listingId, 'admin_accept_return');
        await Promise.all([this.loadDisputes(this.disputesPage), this.loadTransactions(0), this.loadListings(0), this.loadSummary()]);
      }
    });
  }

  confirmReopenReturn(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Reopen return window?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminReopenReturn(listingId, 10);
        await Promise.all([this.loadDisputes(this.disputesPage), this.loadSummary()]);
      }
    });
  }

  confirmCancelRefund(d: any) {
    const listingId = String(d.listingId || d.id);
    this.openConfirm({
      title: 'Cancel & refund?',
      message: String(d?.listingTitle || d?.listingId || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminCancelAndRefundDispute(listingId, 'admin_cancel_refund');
        await Promise.all([this.loadDisputes(this.disputesPage), this.loadTransactions(0), this.loadListings(0), this.loadSummary()]);
      }
    });
  }

  confirmDeleteReport(r: any) {
    this.openConfirm({
      title: 'Dismiss report?',
      message: String(r?.reason || ''),
      variant: 'warning',
      action: async () => {
        await this.api.adminDeleteReport(String(r.id));
        await Promise.all([this.loadReports(), this.loadSummary()]);
      }
    });
  }
}
