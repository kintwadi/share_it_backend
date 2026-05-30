import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Star, Loader2, CheckCircle2, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, User } from '../../core/models/types';

@Component({
  selector: 'app-listing-review',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './listing-review.component.html',
  styleUrl: './listing-review.component.css'
})
export class ListingReviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly Star = Star;
  readonly Loader2 = Loader2;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;

  listing: Listing | null = null;
  currentUser: User | null = null;
  loading = true;
  submitting = false;
  done = false;
  error: string | null = null;

  rating = 0;
  comment = '';

  private backTo = '/';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get targetUserId(): string | null {
    if (!this.listing || !this.currentUser) return null;
    const ownerId = this.listing.ownerId ?? null;
    const borrowerId = this.listing.borrowerId ?? null;
    if (!ownerId) return null;
    if (this.currentUser.id === ownerId) return borrowerId;
    return ownerId;
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

  setRating(v: number) {
    this.rating = v;
    this.render();
  }

  async submit() {
    const listing = this.listing;
    const targetUserId = this.targetUserId;
    if (!listing || !targetUserId) return;
    if (this.submitting) return;
    if (this.rating < 1 || this.rating > 5) {
      this.error = this.i18n.t('review.rating_error');
      this.render();
      return;
    }

    this.submitting = true;
    this.error = null;
    this.render();
    try {
      await this.api.createReview(targetUserId, listing.id, this.rating, this.comment);
      this.done = true;
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('review.submit_error');
    } finally {
      this.submitting = false;
      this.render();
    }
  }
}

