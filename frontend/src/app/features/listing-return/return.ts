import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, CheckCircle2, Loader2, MapPin, Clock, QrCode, ClipboardCheck, AlertTriangle, Star } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { AvailabilityStatus, Listing, ReturnMethod, ReturnSessionResponse, User } from '../../core/models/types';

type ReturnTab = 'qr' | 'manual';

@Component({
  selector: 'app-return',
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
          <div class="text-xl font-bold text-gray-900">{{ i18n.t('return.title') }}</div>
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

          <ng-container *ngIf="!loading && !error && item">
            <div *ngIf="submittedSession; else returnForm" class="space-y-5">
              <div class="rounded-2xl border border-emerald-200 bg-emerald-50 p-5">
                <div class="flex items-start gap-3">
                  <div class="rounded-full bg-emerald-100 p-2 text-emerald-700">
                    <lucide-icon [img]="CheckCircle2" [size]="20"></lucide-icon>
                  </div>
                  <div class="flex-1">
                    <div class="font-bold text-emerald-900">{{ i18n.t('return.request_sent_title') }}</div>
                    <div class="text-sm text-emerald-700 mt-1">{{ i18n.t('return.request_sent_body') }}</div>
                  </div>
                </div>
              </div>

              <div class="rounded-2xl border border-gray-200 p-5 space-y-4">
                <div class="grid sm:grid-cols-2 gap-4 text-sm">
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.borrower_name') }}</div>
                    <div class="font-semibold text-gray-900">{{ submittedSession.borrowerName || currentUser?.name || '-' }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.item_reference') }}</div>
                    <div class="font-semibold text-gray-900 font-mono">{{ submittedSession.itemReference || item.itemReference || '-' }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.return_type') }}</div>
                    <div class="font-semibold text-gray-900">{{ returnMethodLabel(submittedSession.returnMethod || null) }}</div>
                  </div>
                  <div>
                    <div class="text-gray-500">{{ i18n.t('return.hour') }}</div>
                    <div class="font-semibold text-gray-900">{{ formatDate(submittedSession.submittedAt, 'shortTime') }}</div>
                  </div>
                </div>

                <div *ngIf="submittedSession.returnPlace || submittedSession.returnAddress" class="grid sm:grid-cols-2 gap-4 text-sm">
                  <div *ngIf="submittedSession.returnPlace">
                    <div class="text-gray-500">{{ i18n.t('return.return_place') }}</div>
                    <div class="font-semibold text-gray-900">{{ submittedSession.returnPlace }}</div>
                  </div>
                  <div *ngIf="submittedSession.returnAddress">
                    <div class="text-gray-500">{{ i18n.t('return.address') }}</div>
                    <div class="font-semibold text-gray-900">{{ submittedSession.returnAddress }}</div>
                  </div>
                </div>
              </div>

              <div class="rounded-2xl border border-amber-200 bg-amber-50 p-5">
                <div class="font-bold text-amber-900">{{ i18n.t('return.optional_rating_title') }}</div>
                <div class="text-sm text-amber-800 mt-1">{{ i18n.t('return.optional_rating_body') }}</div>
                <div class="mt-4 flex flex-col sm:flex-row gap-3">
                  <button type="button" (click)="goToReview()" class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gray-900 text-white font-semibold hover:bg-black transition-colors">
                    <lucide-icon [img]="Star" [size]="16"></lucide-icon>
                    {{ i18n.t('return.leave_rating') }}
                  </button>
                  <button type="button" (click)="back()" class="px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 font-semibold hover:bg-gray-50 transition-colors">
                    {{ i18n.t('return.skip_for_now') }}
                  </button>
                </div>
              </div>
            </div>

            <ng-template #returnForm>
              <div *ngIf="!isBorrower" class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                {{ i18n.t('return.borrower_only_submit') }}
              </div>

              <ng-container *ngIf="isBorrower">
                <div class="inline-flex rounded-xl bg-gray-100 p-1">
                  <button type="button" (click)="activeTab = 'qr'; render()" class="px-4 py-2 text-sm font-semibold rounded-lg transition-colors" [ngClass]="activeTab === 'qr' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600'">
                    {{ i18n.t('return.via_qr_code') }}
                  </button>
                  <button type="button" (click)="activeTab = 'manual'; render()" class="px-4 py-2 text-sm font-semibold rounded-lg transition-colors" [ngClass]="activeTab === 'manual' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600'">
                    {{ i18n.t('return.via_manual') }}
                  </button>
                </div>

                <div class="rounded-2xl border border-gray-200 p-5 space-y-4">
                  <div class="flex items-center gap-2 text-sm font-semibold text-gray-900">
                    <lucide-icon [img]="activeTab === 'qr' ? QrCode : ClipboardCheck" [size]="18"></lucide-icon>
                    {{ activeTab === 'qr' ? i18n.t('return.via_qr_code') : i18n.t('return.via_manual') }}
                  </div>

                  <div *ngIf="activeTab === 'qr'" class="space-y-2">
                    <label class="block text-sm font-medium text-gray-700">{{ i18n.t('return.six_digit_code') }}</label>
                    <input [(ngModel)]="qrCode" maxlength="6" class="w-full rounded-xl border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-gray-900" placeholder="000000" />
                  </div>

                  <div *ngIf="activeTab === 'manual'" class="space-y-2">
                    <label class="block text-sm font-medium text-gray-700">{{ i18n.t('return.item_reference') }}</label>
                    <input [(ngModel)]="itemNumber" class="w-full rounded-xl border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-gray-900" [placeholder]="i18n.t('return.item_number_placeholder')" />
                  </div>

                  <div class="grid sm:grid-cols-2 gap-4">
                    <div class="space-y-2">
                      <label class="block text-sm font-medium text-gray-700">{{ i18n.t('return.return_place_optional') }}</label>
                      <input [(ngModel)]="returnPlace" class="w-full rounded-xl border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-gray-900" [placeholder]="i18n.t('return.return_place_placeholder')" />
                    </div>
                    <div class="space-y-2">
                      <label class="block text-sm font-medium text-gray-700">{{ i18n.t('return.address_optional') }}</label>
                      <input [(ngModel)]="returnAddress" class="w-full rounded-xl border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-gray-900" [placeholder]="i18n.t('return.address_placeholder')" />
                    </div>
                  </div>

                  <div class="rounded-xl bg-gray-50 px-4 py-3 text-sm text-gray-600 flex items-start gap-2">
                    <lucide-icon [img]="Clock" [size]="16" class="mt-0.5"></lucide-icon>
                    {{ i18n.t('return.hour_auto_note') }}
                  </div>

                  <button type="button" (click)="submit()" [disabled]="submitting || !canSubmit" class="w-full inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-gray-900 text-white font-semibold hover:bg-black transition-colors disabled:opacity-60 disabled:cursor-not-allowed">
                    <lucide-icon *ngIf="submitting" [img]="Loader2" [size]="16" class="animate-spin"></lucide-icon>
                    {{ i18n.t('return.submit_return_request') }}
                  </button>
                </div>
              </ng-container>
            </ng-template>
          </ng-container>
        </div>
      </div>
    </div>
  </div>
  `
})
export class ReturnComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly CheckCircle2 = CheckCircle2;
  readonly Loader2 = Loader2;
  readonly MapPin = MapPin;
  readonly Clock = Clock;
  readonly QrCode = QrCode;
  readonly ClipboardCheck = ClipboardCheck;
  readonly AlertTriangle = AlertTriangle;
  readonly Star = Star;

  item: Listing | null = null;
  currentUser: User | null = null;
  session: ReturnSessionResponse | null = null;
  loading = true;
  submitting = false;
  error: string | null = null;
  activeTab: ReturnTab = 'qr';
  qrCode = '';
  itemNumber = '';
  returnPlace = '';
  returnAddress = '';
  private backTo = '/dashboard';

  get isBorrower(): boolean {
    return !!this.item && !!this.currentUser && this.item.borrowerId === this.currentUser.id;
  }

  get submittedSession(): ReturnSessionResponse | null {
    if (!this.session) return null;
    return this.session.status === 'PENDING' || this.session.status === 'COMPLETED' ? this.session : null;
  }

  get canSubmit(): boolean {
    if (!this.item || !this.isBorrower) return false;
    if (this.item.status !== AvailabilityStatus.BORROWED && this.item.status !== AvailabilityStatus.WAITING_FOR_RETURN) return false;
    return this.activeTab === 'qr' ? /^\d{6}$/.test(this.qrCode.trim()) : !!this.itemNumber.trim();
  }

  ngOnInit(): void {
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/dashboard';
    void this.load();
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
      this.itemNumber = String(item?.itemReference || item?.id || '').trim();
      try {
        this.session = await this.api.getReturnSession(id);
      } catch {
        this.session = null;
      }
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('return.error.load_failed');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async submit() {
    if (!this.item || !this.canSubmit || this.submitting) return;
    this.submitting = true;
    this.error = null;
    this.render();
    try {
      this.session = await this.api.submitReturnRequest(this.item.id, {
        returnMethod: this.activeTab === 'qr' ? 'QR_CODE' : 'MANUAL',
        qrCode: this.activeTab === 'qr' ? this.qrCode.trim() : null,
        itemNumber: this.activeTab === 'manual' ? this.itemNumber.trim() : null,
        returnPlace: this.returnPlace.trim() || null,
        returnAddress: this.returnAddress.trim() || null
      });
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('return.error.manual_failed');
    } finally {
      this.submitting = false;
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

  goToReview() {
    if (!this.item) return;
    this.router.navigate(['/listing', this.item.id, 'review'], { queryParams: { from: this.backTo } });
  }

  back() {
    this.router.navigateByUrl(this.backTo);
  }
}
