import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Search, Filter, ChevronDown, ChevronLeft, ChevronRight, MapPin, Sparkles, LayoutGrid, List } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { Listing, ListingType, AvailabilityStatus, User } from '../../core/models/types';
import { ResourceCardComponent } from '../../shared/components/resource-card/resource-card';
import { ButtonComponent } from '../../shared/components/button/button';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ResourceCardComponent, ButtonComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  api = inject(ApiService);
  i18n = inject(I18nService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  readonly Search = Search;
  readonly Filter = Filter;
  readonly ChevronDown = ChevronDown;
  readonly ChevronLeft = ChevronLeft;
  readonly ChevronRight = ChevronRight;
  readonly MapPin = MapPin;
  readonly Sparkles = Sparkles;
  readonly LayoutGrid = LayoutGrid;
  readonly ListIcon = List;
  readonly ListingType = ListingType;

  listings: Listing[] = [];
  loading = true;
  filterType: 'ALL' | 'ITEMS' | ListingType = 'ALL';
  searchQuery = '';
  locationQuery = '';
  selectedCategory = '';
  currentPage = 1;
  viewMode: 'modern' | 'list' = 'modern';
  currentUser: User | null = null;
  recommended: Listing[] = [];
  borrowCats = new Set<string>();
  homeConfig = { showHeroBadge: true, showHeroTitle: true, showHeroDesc: true };

  readonly ITEMS_PER_PAGE = 6;
  private searchSubject = new Subject<void>();
  private readonly viewModeStorageKey = 'home_view_mode';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  get CATEGORIES() {
    return [this.i18n.t('home.category_all'), 'Tools', 'Gardening', 'Kitchen', 'Outdoors', 'Music', 'Misc'];
  }

  get totalPages(): number {
    return Math.ceil(this.listings.length / this.ITEMS_PER_PAGE);
  }

  get paginatedListings(): Listing[] {
    const start = (this.currentPage - 1) * this.ITEMS_PER_PAGE;
    return this.listings.slice(start, start + this.ITEMS_PER_PAGE);
  }

  get pagesArray(): number[] {
    const maxVisiblePages = 10;
    const blockStart = Math.floor((this.currentPage - 1) / maxVisiblePages) * maxVisiblePages + 1;
    const blockEnd = Math.min(this.totalPages, blockStart + maxVisiblePages - 1);
    return Array.from({ length: blockEnd - blockStart + 1 }, (_, i) => blockStart + i);
  }

  setViewMode(mode: 'modern' | 'list') {
    this.viewMode = mode;
    try {
      localStorage.setItem(this.viewModeStorageKey, mode);
    } catch { }
    this.render();
  }

  ngOnInit() {
    this.selectedCategory = this.i18n.t('home.category_all');
    try {
      const v = String(localStorage.getItem(this.viewModeStorageKey) || '').toLowerCase();
      if (v === 'modern' || v === 'list') this.viewMode = v as any;
    } catch { }
    this.render();

    this.api.getPublicConfig().then(cfg => {
      if (cfg.home) this.homeConfig = cfg.home;
      this.render();
    });

    this.api.getCurrentUser().then(u => {
      this.currentUser = u;
      this.loadRecommendations();
      this.fetchData();
      this.render();
    }).catch(() => {
      this.currentUser = null;
      this.fetchData();
      this.render();
    });

    this.api.getBorrowingHistory().then(hist => {
      hist.forEach(h => {
        if (h.listing) this.borrowCats.add(h.listing.category);
      });
      if (this.currentUser) this.loadRecommendations();
      this.render();
    });

    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.fetchData();
    });
  }

  onSearchChange() {
    this.searchSubject.next();
  }

  onFilterChange(type: 'ALL' | 'ITEMS' | ListingType) {
    this.filterType = type;
    this.fetchData();
  }

  onCategoryChange(event: any) {
    this.selectedCategory = event.target.value;
    this.fetchData();
  }

  async fetchData() {
    this.loading = true;
    this.render();
    try {
      const data = this.searchQuery
        ? await this.api.searchListings(this.searchQuery)
        : await this.api.getListings();

      let filtered = data.filter(l =>
        l.status !== AvailabilityStatus.BLOCKED &&
        l.status !== AvailabilityStatus.HIDDEN
      );

      if (this.filterType === 'ITEMS') {
        filtered = filtered.filter(l => l.type !== ListingType.SKILL);
      } else if (this.filterType !== 'ALL') {
        filtered = filtered.filter(l => l.type === this.filterType);
      }

      if (this.selectedCategory !== this.i18n.t('home.category_all')) {
        filtered = filtered.filter(l => l.category === this.selectedCategory);
      }

      if (this.locationQuery) {
        filtered = filtered.filter(l =>
          l.owner?.address?.toLowerCase().includes(this.locationQuery.toLowerCase())
        );
      }

      this.listings = filtered;
      this.currentPage = 1;
    } catch (err) {
      console.error(err);
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async loadRecommendations() {
    if (!this.currentUser) {
      this.recommended = [];
      this.render();
      return;
    }
    try {
      const recs = await this.api.getRecommendedListings(6);
      if (recs.length > 0) {
        this.recommended = recs;
      } else {
        this.computeFallbackRecommendations();
      }
    } catch {
      this.computeFallbackRecommendations();
      return;
    }
    this.render();
  }

  computeFallbackRecommendations() {
    if (!this.currentUser || this.listings.length === 0) {
      this.recommended = [];
      this.render();
      return;
    }
    const candidates = this.listings.filter(l => l.ownerId !== this.currentUser?.id);
    const scored = candidates.map(l => {
      let score = 0;
      if (this.borrowCats.has(l.category)) score += 2;
        if (this.filterType === 'ITEMS' && l.type !== ListingType.SKILL) score += 1;
        if (this.filterType !== 'ALL' && this.filterType !== 'ITEMS' && l.type === this.filterType) score += 1;
      if (this.selectedCategory !== this.i18n.t('home.category_all') && l.category === this.selectedCategory) score += 1;
      const distBoost = l.distanceMiles ? Math.max(0, 10 - l.distanceMiles) / 10 : 0;
      score += distBoost;
      return { l, score };
    }).sort((a, b) => b.score - a.score).slice(0, 6).map(s => s.l);
    this.recommended = scored;
    this.render();
  }

  handlePageChange(newPage: number) {
    if (newPage >= 1 && newPage <= this.totalPages) {
      this.currentPage = newPage;
      window.scrollTo({ top: 300, behavior: 'smooth' });
    }
  }

  clearFilters() {
    this.searchQuery = '';
    this.locationQuery = '';
    this.filterType = 'ALL';
    this.selectedCategory = this.i18n.t('home.category_all');
    this.fetchData();
  }

  navigateToListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  async dismissRecommendation(id: string) {
    await this.api.dismissRecommendation(id);
    this.recommended = this.recommended.filter(l => l.id !== id);
  }
}
