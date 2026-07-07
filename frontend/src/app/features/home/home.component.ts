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
import { getListingPrimaryRate, getListingPricingUnit, getPricingUnitLong, isListingFree } from '../../core/utils/listing-pricing';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { LocationApiService, LocationResponse } from '../../core/services/location-api.service';
import { PlatformGeolocationService } from '../../core/services/platform-geolocation.service';

type HomeSortMode = 'best_match' | 'nearest' | 'newest';

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
  locationApi = inject(LocationApiService);
  platformGeolocation = inject(PlatformGeolocationService);

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
  locationDenied = false;
  borrowerLat: number | null = null;
  borrowerLng: number | null = null;
  locationSelectedLabel: string | null = null;
  locationResults: LocationResponse[] = [];
  locationLoading = false;
  searchRadiusKm = 25;
  sortMode: HomeSortMode = 'best_match';
  selectedCategory = '';
  currentPage = 1;
  viewMode: 'modern' | 'list' = 'modern';
  currentUser: User | null = null;
  recommended: Listing[] = [];
  borrowCats = new Set<string>();
  homeConfig = { showHeroBadge: true, showHeroTitle: true, showHeroDesc: true };

  readonly ITEMS_PER_PAGE = 6;
  readonly radiusOptions = [5, 10, 25, 50, 100];
  readonly sortOptions: HomeSortMode[] = ['best_match', 'nearest', 'newest'];
  private searchSubject = new Subject<void>();
  private locationSubject = new Subject<void>();
  private fetchRequestId = 0;
  private readonly viewModeStorageKey = 'home_view_mode';
  private readonly sortModeStorageKey = 'home_sort_mode';
  private readonly borrowerLatKey = 'borrower_lat';
  private readonly borrowerLngKey = 'borrower_lng';
  private readonly borrowerLabelKey = 'borrower_location_label';

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

  get isLocationEnabled(): boolean {
    return typeof this.borrowerLat === 'number' && typeof this.borrowerLng === 'number';
  }

  get showNearbyFirstBadge(): boolean {
    return this.isLocationEnabled && this.sortMode !== 'newest';
  }

  listingPricePeriodLabel(listing: Listing): string {
    if (isListingFree(listing)) return '';
    if (listing.type === ListingType.SELL) return '';
    const unit = getPricingUnitLong(getListingPricingUnit(listing));
    return `per ${unit}`;
  }

  listingPriceValue(listing: Listing): string {
    return this.i18n.formatPrice(getListingPrimaryRate(listing));
  }

  listingHasVisiblePrice(listing: Listing): boolean {
    return !isListingFree(listing) && getListingPrimaryRate(listing) > 0;
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
      const storedSort = String(localStorage.getItem(this.sortModeStorageKey) || '').toLowerCase();
      if (storedSort === 'best_match' || storedSort === 'nearest' || storedSort === 'newest') {
        this.sortMode = storedSort as HomeSortMode;
      }
    } catch { }
    this.render();

    this.api.getPublicConfig().then(cfg => {
      if (cfg.home) this.homeConfig = cfg.home;
      this.render();
    });

    this.api.getCurrentUser().then(u => {
      this.currentUser = u;
      this.loadRecommendations();
      this.initBorrowerLocation().then(() => this.fetchData());
      this.render();
    }).catch(() => {
      this.currentUser = null;
      this.initBorrowerLocation().then(() => this.fetchData());
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
    this.locationSubject.pipe(debounceTime(250)).subscribe(() => {
      this.fetchLocationAutocomplete();
    });
  }

  onSearchChange() {
    this.searchSubject.next();
  }

  onLocationInputChange(value: string) {
    this.locationQuery = value;
    const normalizedQuery = String(value || '').trim();
    const normalizedSelected = String(this.locationSelectedLabel || '').trim();
    const hadLocation = this.isLocationEnabled;

    if (!normalizedQuery) {
      this.clearBorrowerLocation();
      this.locationResults = [];
      this.locationDenied = false;
      if (hadLocation) {
        this.fetchData();
      } else {
        this.render();
      }
      return;
    }

    if (!normalizedSelected || normalizedQuery !== normalizedSelected) {
      this.clearBorrowerCoordinates();
      if (hadLocation) {
        this.fetchData();
      }
    }

    this.locationSubject.next();
  }

  onLocationSelect(loc: LocationResponse) {
    const lat = typeof loc.latitude === 'number' ? loc.latitude : null;
    const lng = typeof loc.longitude === 'number' ? loc.longitude : null;
    if (lat == null || lng == null) return;
    this.setBorrowerLocation(lat, lng, loc.displayName || this.locationQuery || null);
    this.locationResults = [];
    this.fetchData();
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
    const requestId = ++this.fetchRequestId;
    this.loading = true;
    this.render();
    try {
      const hadLocation = this.isLocationEnabled;
      const data = await this.loadListings(hadLocation);
      if (requestId !== this.fetchRequestId) return;

      let filtered = this.applyClientFilters(data);

      if (hadLocation && filtered.length === 0) {
        const fallbackData = await this.loadListings(false);
        if (requestId !== this.fetchRequestId) return;

        const fallbackFiltered = this.applyClientFilters(fallbackData);
        if (fallbackFiltered.length > 0) {
          this.clearBorrowerLocation();
          this.locationResults = [];
          filtered = fallbackFiltered;
        }
      }

      if (requestId !== this.fetchRequestId) return;
      this.listings = filtered;
      this.currentPage = 1;
    } catch (err) {
      if (requestId === this.fetchRequestId) {
        console.error(err);
      }
    } finally {
      if (requestId === this.fetchRequestId) {
        this.loading = false;
        this.render();
      }
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
    this.locationDenied = false;
    this.filterType = 'ALL';
    this.searchRadiusKm = 25;
    this.selectedCategory = this.i18n.t('home.category_all');
    this.locationResults = [];
    this.clearBorrowerCoordinates();
    this.fetchData();
  }

  navigateToListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  async dismissRecommendation(id: string) {
    await this.api.dismissRecommendation(id);
    this.recommended = this.recommended.filter(l => l.id !== id);
  }

  async requestCurrentLocation() {
    const hadLocation = this.isLocationEnabled;
    const hadLocationText = !!String(this.locationQuery || this.locationSelectedLabel || '').trim();
    this.locationLoading = true;
    this.locationResults = [];
    this.render();

    try {
      const pos = await this.platformGeolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 60000
      });
      this.setBorrowerLocation(
        pos.coords.latitude,
        pos.coords.longitude,
        this.i18n.t('home.location.current_location')
      );
      this.locationDenied = false;
      await this.fetchData();
    } catch {
      this.locationDenied = true;
      this.clearBorrowerLocation();
      this.locationResults = [];
      if (hadLocation || hadLocationText) {
        await this.fetchData();
      }
    } finally {
      this.locationLoading = false;
      this.render();
    }
  }

  onRadiusChange(value: string | number) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) return;
    this.searchRadiusKm = parsed;
    if (this.isLocationEnabled) {
      this.fetchData();
    }
  }

  onSortChange(value: string) {
    if (value !== 'best_match' && value !== 'nearest' && value !== 'newest') {
      return;
    }
    this.sortMode = value;
    try {
      localStorage.setItem(this.sortModeStorageKey, value);
    } catch { }
    this.fetchData();
  }

  clearLocationFilter() {
    if (!this.locationQuery && !this.isLocationEnabled) return;
    const hadLocation = this.isLocationEnabled;
    this.locationQuery = '';
    this.locationResults = [];
    this.locationDenied = false;
    this.clearBorrowerCoordinates();
    if (hadLocation) {
      this.fetchData();
    } else {
      this.render();
    }
  }

  private async initBorrowerLocation() {
    if (this.borrowerLat != null && this.borrowerLng != null) return;

    try {
      const storedLat = parseFloat(String(localStorage.getItem(this.borrowerLatKey) || ''));
      const storedLng = parseFloat(String(localStorage.getItem(this.borrowerLngKey) || ''));
      const label = String(localStorage.getItem(this.borrowerLabelKey) || '').trim();
      if (!Number.isNaN(storedLat) && !Number.isNaN(storedLng)) {
        this.borrowerLat = storedLat;
        this.borrowerLng = storedLng;
        this.locationSelectedLabel = label || null;
        this.locationQuery = label || '';
      }
    } catch {
    }
  }

  private setBorrowerLocation(lat: number, lng: number, label: string | null) {
    this.borrowerLat = lat;
    this.borrowerLng = lng;
    this.locationSelectedLabel = label;
    this.locationQuery = label || '';
    try {
      localStorage.setItem(this.borrowerLatKey, String(lat));
      localStorage.setItem(this.borrowerLngKey, String(lng));
      if (label) localStorage.setItem(this.borrowerLabelKey, label);
    } catch { }
  }

  private clearBorrowerCoordinates() {
    this.borrowerLat = null;
    this.borrowerLng = null;
    this.locationSelectedLabel = null;
    try {
      localStorage.removeItem(this.borrowerLatKey);
      localStorage.removeItem(this.borrowerLngKey);
      localStorage.removeItem(this.borrowerLabelKey);
    } catch { }
  }

  private clearBorrowerLocation() {
    this.clearBorrowerCoordinates();
    this.locationQuery = '';
  }

  private loadListings(useLocation: boolean) {
    return this.api.getListings({
      search: this.searchQuery || null,
      category: this.selectedCategory !== this.i18n.t('home.category_all') ? this.selectedCategory : null,
      type: this.filterType !== 'ALL' && this.filterType !== 'ITEMS' ? this.filterType : null,
      viewerLat: useLocation ? this.borrowerLat : null,
      viewerLng: useLocation ? this.borrowerLng : null,
      nearbyRadiusKm: useLocation ? this.searchRadiusKm : null,
      sortBy: this.sortMode,
      size: 200
    });
  }

  private applyClientFilters(data: Listing[]) {
    let filtered = data.filter(l =>
      l.status !== AvailabilityStatus.BLOCKED &&
      l.status !== AvailabilityStatus.HIDDEN
    );

    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      filtered = filtered.filter(l => String(l.title || '').toLowerCase().includes(q));
    }

    if (this.filterType === 'ITEMS') {
      filtered = filtered.filter(l => l.type !== ListingType.SKILL);
    } else if (this.filterType !== 'ALL') {
      filtered = filtered.filter(l => l.type === this.filterType);
    }

    if (this.selectedCategory !== this.i18n.t('home.category_all')) {
      filtered = filtered.filter(l => l.category === this.selectedCategory);
    }

    return filtered;
  }

  private async fetchLocationAutocomplete() {
    const q = String(this.locationQuery || '').trim();
    if (this.isLocationEnabled && q === String(this.locationSelectedLabel || '').trim()) {
      this.locationResults = [];
      this.render();
      return;
    }
    if (q.length < 2) {
      this.locationResults = [];
      this.render();
      return;
    }
    this.locationLoading = true;
    this.render();
    try {
      this.locationResults = await this.locationApi.autocomplete(q, 'pt,de,fr,be', 6);
    } catch {
      this.locationResults = [];
    } finally {
      this.locationLoading = false;
      this.render();
    }
  }
}
