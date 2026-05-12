import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Star, Loader2, CheckCircle2 } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { ButtonComponent } from '../../shared/components/button/button';

@Component({
  selector: 'app-rate',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ButtonComponent],
  templateUrl: './rate.component.html',
  styleUrl: './rate.component.css'
})
export class RateComponent implements OnInit {
  route = inject(ActivatedRoute);
  router = inject(Router);
  api = inject(ApiService);
  i18n = inject(I18nService);

  readonly Star = Star;
  readonly Loader2 = Loader2;
  readonly CheckCircle2 = CheckCircle2;

  token = '';
  loading = true;
  invite: any = null;
  rating = 0;
  comment = '';
  error: string | null = null;
  submitted = false;
  submitting = false;

  stars = [1, 2, 3, 4, 5];

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      this.loadInvite();
    });
  }

  async loadInvite() {
    if (!this.token) {
      this.error = this.i18n.t('rate.error.missing_token');
      this.loading = false;
      return;
    }
    try {
      this.loading = true;
      const res = await this.api.getReviewInvite(this.token);
      this.invite = res;
      if (res?.used) {
        this.submitted = true;
      }
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : this.i18n.t('rate.error.load_failed');
    } finally {
      this.loading = false;
    }
  }

  setRating(val: number) {
    this.rating = val;
  }

  async handleSubmit() {
    if (!this.token) return;
    if (this.rating < 1 || this.rating > 5) {
      this.error = this.i18n.t('rate.error.rating_required');
      return;
    }
    this.error = null;
    this.submitting = true;
    try {
      await this.api.submitReviewInvite(this.token, this.rating, this.comment);
      this.submitted = true;
    } catch (e: any) {
      this.error = e instanceof Error ? e.message : this.i18n.t('rate.error.submit_failed');
    } finally {
      this.submitting = false;
    }
  }

  navToDashboard() {
    this.router.navigate(['/dashboard']);
  }

  navToHome() {
    this.router.navigate(['/']);
  }

  navToListing() {
    if (this.invite?.listingId) {
      this.router.navigate(['/listing', this.invite.listingId]);
    }
  }
}
