import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, AlertTriangle, Loader2, Shield } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

type AdminConfirmAction =
  | 'user-status'
  | 'approve-verification'
  | 'revoke-verification'
  | 'delete-user'
  | 'block-listing'
  | 'delete-listing'
  | 'approve-partner-listing'
  | 'reject-partner-listing'
  | 'approve-partner-submission'
  | 'reject-partner-submission'
  | 'approve-partner-borrow'
  | 'reject-partner-borrow'
  | 'delete-transaction'
  | 'retry-release'
  | 'accept-return'
  | 'reopen-return'
  | 'cancel-refund'
  | 'dismiss-report';

@Component({
  selector: 'app-admin-action-confirm',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './admin-action-confirm.component.html',
  styleUrl: './admin-action-confirm.component.css'
})
export class AdminActionConfirmComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly AlertTriangle = AlertTriangle;
  readonly Loader2 = Loader2;
  readonly Shield = Shield;

  loading = true;
  confirming = false;
  error: string | null = null;

  backTo = '/admin';
  title = 'Confirm action';
  message = '';
  confirmLabel = 'Confirm';
  variant: 'danger' | 'warning' | 'info' = 'info';

  action: AdminConfirmAction | null = null;
  payload: any = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/admin';

    const st: any = history.state || {};
    this.title = String(st?.title || this.title);
    this.message = String(st?.message || '');
    this.confirmLabel = String(st?.confirmLabel || this.confirmLabel);
    this.variant = (['danger', 'warning', 'info'] as const).includes(st?.variant) ? st.variant : 'info';
    this.action = typeof st?.action === 'string' ? (st.action as AdminConfirmAction) : null;
    this.payload = st?.payload ?? null;

    if (!this.action) {
      this.error = 'Missing action';
    }

    this.loading = false;
    this.render();
  }

  back() {
    this.router.navigateByUrl(this.backTo || '/admin');
  }

  async confirm() {
    if (!this.action) return;
    if (this.confirming) return;
    this.confirming = true;
    this.error = null;
    this.render();

    try {
      const p = this.payload || {};

      switch (this.action) {
        case 'user-status':
          await this.api.adminSetUserStatus(String(p.userId), String(p.status));
          break;
        case 'approve-verification':
          await this.api.approveVerification(String(p.userId));
          break;
        case 'revoke-verification':
          await this.api.revokeVerification(String(p.userId));
          break;
        case 'delete-user':
          await this.api.adminDeleteUser(String(p.userId));
          break;
        case 'block-listing':
          await this.api.adminBlockListing(String(p.listingId), !!p.blocked);
          break;
        case 'delete-listing':
          await this.api.adminDeleteListing(String(p.listingId));
          break;
        case 'approve-partner-listing':
          await this.api.adminApprovePartnerListing(String(p.listingId));
          break;
        case 'reject-partner-listing':
          await this.api.adminRejectPartnerListing(String(p.listingId));
          break;
        case 'approve-partner-submission':
          await this.api.adminApprovePartnerSubmission(String(p.submissionId));
          break;
        case 'reject-partner-submission':
          await this.api.adminRejectPartnerSubmission(String(p.submissionId));
          break;
        case 'approve-partner-borrow':
          await this.api.adminApprovePartnerBorrowRequest(String(p.requestId));
          break;
        case 'reject-partner-borrow':
          await this.api.adminRejectPartnerBorrowRequest(String(p.requestId));
          break;
        case 'delete-transaction':
          await this.api.adminDeleteTransaction(String(p.txId));
          break;
        case 'retry-release':
          await this.api.adminRetryTransactionRelease(String(p.txId));
          break;
        case 'accept-return':
          await this.api.adminAcceptReturnDispute(String(p.listingId), String(p.reason || 'admin_accept_return'));
          break;
        case 'reopen-return':
          await this.api.adminReopenReturn(String(p.listingId), Number(p.days || 10));
          break;
        case 'cancel-refund':
          await this.api.adminCancelAndRefundDispute(String(p.listingId), String(p.reason || 'admin_cancel_refund'));
          break;
        case 'dismiss-report':
          await this.api.adminDeleteReport(String(p.reportId));
          break;
      }

      this.router.navigateByUrl(this.backTo || '/admin', { state: { noticeSuccess: 'done' } as any });
    } catch (e: any) {
      this.error = e?.message || 'Action failed';
      this.confirming = false;
      this.render();
    }
  }
}

