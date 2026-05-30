import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Flag, Loader2, CheckCircle2, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, User } from '../../core/models/types';

@Component({
  selector: 'app-listing-report',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-report.component.html',
  styleUrl: './listing-report.component.css'
})
export class ListingReportComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly Flag = Flag;
  readonly Loader2 = Loader2;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;

  listing: Listing | null = null;
  currentUser: User | null = null;
  loading = true;
  submitting = false;
  error: string | null = null;
  done = false;

  reason = '';
  details = '';

  private backTo = '';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : `/listing/${encodeURIComponent(id)}`;

    if (!id) {
      this.router.navigateByUrl(from.startsWith('/') ? from : '/');
      return;
    }

    this.loading = true;
    this.render();
    try {
      const [listing, me] = await Promise.all([
        this.api.getListingById(id),
        this.api.getCurrentUser()
      ]);
      this.listing = listing;
      this.currentUser = me;
      if (!me) {
        this.router.navigate(['/connect']);
        return;
      }
    } catch (e: any) {
      this.error = e?.message || 'Failed to load';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  back() {
    this.router.navigateByUrl(this.backTo || '/');
  }

  async submit() {
    const listing = this.listing;
    if (!listing) return;
    if (!this.currentUser) {
      this.router.navigate(['/connect']);
      return;
    }
    if (!this.reason) return;
    if (this.submitting) return;

    this.submitting = true;
    this.error = null;
    this.render();
    try {
      await this.api.reportListing(listing.id, this.reason, this.details);
      this.done = true;
    } catch (e: any) {
      const msg = e?.message || this.i18n.t('listing.error.report_failed');
      this.error = typeof msg === 'string' && msg.includes('already_reported_for_reason')
        ? this.i18n.t('listing.report.already_reported')
        : msg;
    } finally {
      this.submitting = false;
      this.render();
    }
  }
}

