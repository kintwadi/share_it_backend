import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Star, Loader2, Building2, User as UserIcon } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing } from '../../core/models/types';

@Component({
  selector: 'app-listing-profile',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './listing-profile.component.html',
  styleUrl: './listing-profile.component.css'
})
export class ListingProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly Star = Star;
  readonly Loader2 = Loader2;
  readonly Building2 = Building2;
  readonly UserIcon = UserIcon;

  listing: Listing | null = null;
  loading = true;
  loadingReviews = false;
  reviews: any[] = [];
  backTo = '';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get isPartnerListing(): boolean {
    return !!(this.listing as any)?.partnerId;
  }

  get displayName(): string {
    if (this.isPartnerListing) return String((this.listing as any)?.partnerName || 'Partner');
    return String((this.listing as any)?.owner?.name || '');
  }

  get displayEmail(): string {
    if (this.isPartnerListing) return String((this.listing as any)?.partnerEmail || '');
    return String((this.listing as any)?.owner?.email || '');
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
      this.listing = await this.api.getListingById(id);
      await this.loadReviews();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  back() {
    this.router.navigateByUrl(this.backTo || '/');
  }

  private async loadReviews() {
    const l: any = this.listing as any;
    if (!l) return;
    if (this.isPartnerListing) {
      this.reviews = [];
      this.loadingReviews = false;
      return;
    }
    const ownerId = String(l.ownerId || '').trim();
    if (!ownerId) return;
    this.loadingReviews = true;
    this.render();
    try {
      this.reviews = await this.api.getReviews(ownerId);
    } catch {
      this.reviews = [];
    } finally {
      this.loadingReviews = false;
      this.render();
    }
  }
}

