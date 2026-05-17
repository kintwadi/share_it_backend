import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, X, Star, Loader2, CheckCircle2 } from 'lucide-angular';
import { ApiService } from '../../../core/services/api.service';
import { Listing, User } from '../../../core/models/types';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-review-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './review-modal.html',
  styleUrl: './review-modal.css'
})
export class ReviewModalComponent {
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  @Input() item: Listing | null = null;
  @Input() currentUser: User | null = null;
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() submitted = new EventEmitter<void>();

  readonly X = X;
  readonly Star = Star;
  readonly Loader2 = Loader2;
  readonly CheckCircle2 = CheckCircle2;

  rating = 0;
  comment = '';
  loading = false;
  error: string | null = null;
  done = false;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  setRating(v: number) {
    this.rating = v;
    this.render();
  }

  get targetUserId(): string | null {
    if (!this.item || !this.currentUser) return null;
    const ownerId = this.item.ownerId ?? null;
    const borrowerId = this.item.borrowerId ?? null;
    if (!ownerId) return null;
    if (this.currentUser.id === ownerId) return borrowerId;
    return ownerId;
  }

  async submit() {
    const listing = this.item;
    const targetUserId = this.targetUserId;
    if (!listing || !targetUserId) return;
    if (this.rating < 1 || this.rating > 5) {
      this.error = this.i18n.t('review.rating_error');
      this.render();
      return;
    }
    this.loading = true;
    this.error = null;
    this.render();
    try {
      await this.api.createReview(targetUserId, listing.id, this.rating, this.comment);
      this.done = true;
      this.submitted.emit();
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('review.submit_error');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  onClose() {
    this.close.emit();
  }
}
