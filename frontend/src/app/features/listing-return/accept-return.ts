import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, AlertTriangle, CheckCircle2, Loader2, Clock, MapPin, FileCheck } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, ReturnMethod, ReturnSessionResponse, User } from '../../core/models/types';

@Component({
  selector: 'app-accept-return',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
  <div class="min-h-screen bg-gray-50 pt-24 pb-10">
    <div class="max-w-2xl mx-auto px-4">
      <button type="button" (click)="back()" class="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-900 mb-4">
        <lucide-icon [img]="ArrowLeft" [size]="16"></lucide-icon>
        {{ i18n.t('common.back') }}
      </button>

      <div class="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div class="px-6 py-5 border-b border-gray-100">
          <div class="text-xl font-bold text-gray-900">{{ i18n.t('return.accept_return_request') }}</div>
          <div *ngIf="item" class="text-sm text-gray-500 mt-1">{{ item.title }}</div>
        </div>

        <div class="p-6 space-y-5">
          <div *ngIf="loading" class="flex items-center gap-3 text-sm text-gray-600">
            <lucide-icon [img]="Loader2" [size]="18" class="animate-spin"></lucide-icon>
            {{ i18n.t('return.initializing') }}
          </div>

          <div *ngIf="!loading && error" class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex gap-3">
            <lucide-icon [img]="AlertTriangle" [size]="18" class="mt-0.5"></lucide-icon>
            <div>{{ error }}</div>
          </div>

          <ng-container *ngIf="!loading && !error">
            <div *ngIf="!session" class="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
              {{ i18n.t('return.no_pending_request') }}
            </div>

            <ng-container *ngIf="session">
              <div class="rounded-2xl border border-gray-200 p-5 space-y-4">
                <div class="grid sm:grid-cols-2 gap-4 text-sm">
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.borrower_name') }}</div>
                    <div class="font-semibold text-gray-900">{{ session.borrowerName || item?.borrower?.name || '-' }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.item_reference') }}</div>
                    <div class="font-semibold text-gray-900 font-mono">{{ session.itemReference || item?.itemReference || '-' }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.return_type') }}</div>
                    <div class="font-semibold text-gray-900">{{ returnMethodLabel(session.returnMethod || null) }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.hour') }}</div>
                    <div class="font-semibold text-gray-900">{{ formatDate(session.submittedAt, 'shortTime') }}</div>
                  </div>
                </div>

                <div *ngIf="session.returnPlace || session.returnAddress" class="grid sm:grid-cols-2 gap-4 text-sm">
                  <div *ngIf="session.returnPlace" class="flex gap-2">
                    <lucide-icon [img]="MapPin" [size]="16" class="text-gray-400 mt-0.5"></lucide-icon>
                    <div>
                      <div class="text-gray-500">{{ i18n.t('return.return_place') }}</div>
                      <div class="font-semibold text-gray-900">{{ session.returnPlace }}</div>
                    </div>
                  </div>
                  <div *ngIf="session.returnAddress" class="flex gap-2">
                    <lucide-icon [img]="FileCheck" [size]="16" class="text-gray-400 mt-0.5"></lucide-icon>
                    <div>
                      <div class="text-gray-500">{{ i18n.t('return.address') }}</div>
                      <div class="font-semibold text-gray-900">{{ session.returnAddress }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="rounded-2xl border border-red-200 bg-red-50 p-5 space-y-3">
                <div class="font-bold text-red-900">{{ i18n.t('return.dispute') }}</div>
                <textarea [(ngModel)]="disputeReason" rows="4" class="w-full rounded-xl border border-red-200 px-4 py-3 outline-none focus:ring-2 focus:ring-red-500" [placeholder]="i18n.t('return.dispute_issue_placeholder')"></textarea>
                <div class="text-xs text-red-700">{{ i18n.t('return.dispute_contact') }}</div>
              </div>

              <div *ngIf="success" class="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 flex gap-3 text-sm text-emerald-800">
                <lucide-icon [img]="CheckCircle2" [size]="18" class="mt-0.5"></lucide-icon>
                <div>{{ success }}</div>
              </div>

              <div class="flex flex-col sm:flex-row gap-3">
                <button type="button" (click)="accept()" [disabled]="busy" class="flex-1 inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-gray-900 text-white font-semibold hover:bg-black transition-colors disabled:opacity-60">
                  <lucide-icon *ngIf="busyAction === 'accept'" [img]="Loader2" [size]="16" class="animate-spin"></lucide-icon>
                  {{ i18n.t('return.accept') }}
                </button>
                <button type="button" (click)="dispute()" [disabled]="busy" class="flex-1 inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-white border border-red-200 text-red-700 font-semibold hover:bg-red-50 transition-colors disabled:opacity-60">
                  <lucide-icon *ngIf="busyAction === 'dispute'" [img]="Loader2" [size]="16" class="animate-spin"></lucide-icon>
                  {{ i18n.t('return.dispute') }}
                </button>
              </div>
            </ng-container>
          </ng-container>
        </div>
      </div>
    </div>
  </div>
  `
})
export class AcceptReturnComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly Loader2 = Loader2;
  readonly Clock = Clock;
  readonly MapPin = MapPin;
  readonly FileCheck = FileCheck;

  item: Listing | null = null;
  currentUser: User | null = null;
  session: ReturnSessionResponse | null = null;
  loading = true;
  busyAction: 'accept' | 'dispute' | null = null;
  error: string | null = null;
  success: string | null = null;
  disputeReason = '';
  private backTo = '/dashboard';
  private redirectTimer: ReturnType<typeof setTimeout> | null = null;

  get busy(): boolean {
    return !!this.busyAction;
  }

  ngOnInit(): void {
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/dashboard';
    void this.load();
  }

  ngOnDestroy(): void {
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async load() {
    const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
    if (!id) {
      this.back();
      return;
    }
    this.loading = true;
    this.error = null;
    this.render();
    try {
      const [item, me] = await Promise.all([
        this.api.getListingById(id),
        this.api.getCurrentUser()
      ]);
      this.item = item;
      this.currentUser = me;
      if (!item || !me || item.ownerId !== me.id) {
        this.error = this.i18n.t('return.lender_only_accept');
        return;
      }
      if (item.status === 'AVAILABLE') {
        this.session = null;
        this.success = this.i18n.t('return.accept_success');
        this.redirectTimer = setTimeout(() => {
          this.router.navigateByUrl(this.backTo);
        }, 900);
        return;
      }
      if (item.status !== 'WAITING_FOR_RETURN' && item.status !== 'DISPUTED') {
        this.session = null;
        return;
      }
      try {
        const session = await this.api.getReturnSession(id);
        this.session = String((session as any)?.status || '').toUpperCase() === 'PENDING' ? session : null;
      } catch {
        this.session = null;
        try {
          const refreshed = await this.api.getListingById(id);
          this.item = refreshed;
          if (refreshed?.status === 'AVAILABLE') {
            this.success = this.i18n.t('return.accept_success');
            this.redirectTimer = setTimeout(() => {
              this.router.navigateByUrl(this.backTo);
            }, 900);
          }
        } catch { }
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('return.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async accept() {
    if (!this.item || !this.session || this.busy) return;
    this.busyAction = 'accept';
    this.error = null;
    this.success = null;
    this.render();
    try {
      await this.api.acceptReturnRequest(this.item.id);
      this.success = this.i18n.t('return.accept_success');
      this.session = null;
      this.redirectTimer = setTimeout(() => {
        this.router.navigateByUrl(this.backTo);
      }, 900);
      this.render();
      return;
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('return.accept_failed');
    } finally {
      this.busyAction = null;
      this.render();
    }
  }

  async dispute() {
    if (!this.item || !this.session || this.busy) return;
    this.busyAction = 'dispute';
    this.error = null;
    this.success = null;
    this.render();
    try {
      await this.api.initiateReturnDispute(this.item.id, this.disputeReason.trim() || 'return_disputed_by_lender');
      this.success = this.i18n.t('return.dispute_started');
      await this.load();
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('return.dispute_failed');
    } finally {
      this.busyAction = null;
      this.render();
    }
  }

  returnMethodLabel(method: ReturnMethod | string | null | undefined): string {
    return String(method || '').toUpperCase() === 'QR_CODE'
      ? this.i18n.t('return.via_qr_code')
      : this.i18n.t('return.via_manual');
  }

  formatDate(value?: string | null, style: 'shortTime' | 'short' = 'short'): string {
    if (!value) return '-';
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return '-';
    return dt.toLocaleString([], style === 'shortTime'
      ? { hour: '2-digit', minute: '2-digit' }
      : { dateStyle: 'medium', timeStyle: 'short' } as any);
  }

  back() {
    this.router.navigateByUrl(this.backTo);
  }
}
