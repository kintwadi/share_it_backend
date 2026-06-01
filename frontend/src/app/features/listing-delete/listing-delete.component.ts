import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Trash2, AlertTriangle, Loader2 } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing } from '../../core/models/types';

@Component({
  selector: 'app-listing-delete',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './listing-delete.component.html',
  styleUrl: './listing-delete.component.css'
})
export class ListingDeleteComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly Trash2 = Trash2;
  readonly AlertTriangle = AlertTriangle;
  readonly Loader2 = Loader2;

  listing: Listing | null = null;
  loading = true;
  deleting = false;
  error: string | null = null;

  private backTo = '/';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/';

    if (!id) {
      this.router.navigateByUrl(this.backTo);
      return;
    }

    this.loading = true;
    this.render();
    try {
      this.listing = await this.api.getListingById(id);
    } catch (e: any) {
      this.error = e?.message || 'Failed to load listing';
      this.listing = null;
    } finally {
      this.loading = false;
      this.render();
    }
  }

  back() {
    this.router.navigateByUrl(this.backTo);
  }

  async confirmDelete() {
    const listing = this.listing;
    if (!listing) return;
    if (this.deleting) return;
    this.deleting = true;
    this.error = null;
    this.render();
    try {
      await this.api.deleteListing(listing.id);
      this.router.navigateByUrl(this.backTo);
    } catch (e: any) {
      this.error = e?.message || 'Failed to delete listing';
    } finally {
      this.deleting = false;
      this.render();
    }
  }
}

