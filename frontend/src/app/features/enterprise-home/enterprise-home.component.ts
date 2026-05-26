import { ChangeDetectorRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Search, Building2, ChevronDown, Filter, X, ArrowUpDown } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { EnterpriseCategoriesService, EnterpriseCategorySector } from '../../core/services/enterprise-categories.service';
import { I18nService } from '../../core/services/i18n.service';
import { AvailabilityStatus, Listing, ListingType } from '../../core/models/types';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-enterprise-home',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './enterprise-home.component.html',
  styleUrl: './enterprise-home.component.css'
})
export class EnterpriseHomeComponent implements OnInit {
  api = inject(ApiService);
  categoriesApi = inject(EnterpriseCategoriesService);
  i18n = inject(I18nService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  readonly Search = Search;
  readonly Building2 = Building2;
  readonly ChevronDown = ChevronDown;
  readonly Filter = Filter;
  readonly X = X;
  readonly ArrowUpDown = ArrowUpDown;

  loading = signal(true);
  error = signal('');

  sectors = signal<EnterpriseCategorySector[]>([]);
  selectedSector = signal<string>('');
  selectedGroup = signal<string>('');
  selectedItemType = signal<string>('');

  query = signal('');
  onlyAvailable = signal(true);
  partnerOnly = signal(false);
  listingType = signal<'ALL' | ListingType>('ALL');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);
  sortBy = signal<'RELEVANCE' | 'TITLE' | 'RATE' | 'DISTANCE'>('RELEVANCE');

  filterOpen = signal(false);
  allRows = signal<Listing[]>([]);
  page = signal(1);
  pageSize = signal(10);

  private searchSubject = new Subject<void>();

  sectorGroups = computed(() => {
    const s = this.sectors().find(x => x.label === this.selectedSector());
    if (!s) return [];
    return s.groups.map(g => g.label);
  });

  sectorItems = computed(() => {
    const s = this.sectors().find(x => x.label === this.selectedSector());
    if (!s) return [];
    const g = this.selectedGroup() ? s.groups.find(x => x.label === this.selectedGroup()) : null;
    const all = (g ? g.items : s.groups.flatMap(x => x.items)).map(i => i.label);
    return Array.from(new Set(all));
  });

  selectedKeywords = computed(() => {
    const sector = this.sectors().find(x => x.label === this.selectedSector());
    if (!sector) return [];
    const g = this.selectedGroup() ? sector.groups.find(x => x.label === this.selectedGroup()) : null;
    const items = g ? g.items : sector.groups.flatMap(x => x.items);
    const item = this.selectedItemType() ? items.find(i => i.label === this.selectedItemType()) : null;
    if (!item) return [];
    return Array.from(new Set([item.label, ...(item.keywords || [])])).filter(Boolean);
  });

  filteredRows = computed(() => {
    const base = this.allRows();
    const q = this.query().trim();
    const sector = this.selectedSector().trim();
    const group = this.selectedGroup().trim();
    const item = this.selectedItemType().trim();
    const onlyAvail = this.onlyAvailable();
    const partnerOnly = this.partnerOnly();
    const type = this.listingType();
    const min = this.minRate();
    const max = this.maxRate();
    const sort = this.sortBy();
    const keywords = this.selectedKeywords();

    let filtered = base.filter(l => l.status !== AvailabilityStatus.BLOCKED && l.status !== AvailabilityStatus.HIDDEN);

    if (onlyAvail) {
      filtered = filtered.filter(l => l.status === AvailabilityStatus.AVAILABLE || l.status === AvailabilityStatus.PARTNER_ACTIVE);
    }
    if (partnerOnly) {
      filtered = filtered.filter(l => !!l.partnerId);
    }
    if (type !== 'ALL') {
      filtered = filtered.filter(l => l.type === type);
    }
    if (min != null && !Number.isNaN(min)) {
      filtered = filtered.filter(l => (Number(l.hourlyRate || 0) >= Number(min)));
    }
    if (max != null && !Number.isNaN(max)) {
      filtered = filtered.filter(l => (Number(l.hourlyRate || 0) <= Number(max)));
    }

    const matchAny = (l: Listing, needles: string[]) => {
      if (!needles || needles.length === 0) return true;
      const hay = `${l.title || ''} ${l.category || ''} ${l.description || ''} ${l.partnerName || ''}`.toLowerCase();
      for (const n of needles) {
        const t = String(n || '').trim().toLowerCase();
        if (!t) continue;
        if (hay.includes(t)) return true;
      }
      return false;
    };

    if (item) {
      const needles = keywords.length > 0 ? keywords : [item];
      filtered = filtered.filter(l => matchAny(l, needles));
    } else if (group) {
      const sectorObj = this.sectors().find(x => x.label === sector);
      const groupObj = sectorObj?.groups.find(x => x.label === group);
      const needles = groupObj ? groupObj.items.flatMap(i => [i.label, ...(i.keywords || [])]) : [group];
      filtered = filtered.filter(l => matchAny(l, needles));
    } else if (sector) {
      filtered = filtered.filter(l => matchAny(l, [sector]) || (!!l.partnerId));
    }

    if (sort !== 'RELEVANCE' || !q) {
      const compareTitle = (a: Listing, b: Listing) => String(a.title || '').localeCompare(String(b.title || ''));
      const compareRate = (a: Listing, b: Listing) => Number(a.hourlyRate || 0) - Number(b.hourlyRate || 0);
      const compareDistance = (a: Listing, b: Listing) => Number(a.distanceMiles || 0) - Number(b.distanceMiles || 0);
      const cmp =
        sort === 'TITLE' ? compareTitle :
        sort === 'RATE' ? compareRate :
        sort === 'DISTANCE' ? compareDistance :
        compareTitle;
      filtered = [...filtered].sort(cmp);
    }

    return filtered;
  });

  totalPages = computed(() => {
    const per = Math.max(1, Number(this.pageSize() || 10));
    const total = this.filteredRows().length;
    return Math.max(1, Math.ceil(total / per));
  });

  pagedRows = computed(() => {
    const per = Math.max(1, Number(this.pageSize() || 10));
    const p = Math.min(this.totalPages(), Math.max(1, Number(this.page() || 1)));
    const start = (p - 1) * per;
    return this.filteredRows().slice(start, start + per);
  });

  async ngOnInit() {
    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.reloadFromApi();
    });
    await this.loadCategories();
    await this.reloadFromApi();
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async loadCategories() {
    try {
      const sectors = await this.categoriesApi.load();
      this.sectors.set(sectors);
      if (!this.selectedSector() && sectors.length > 0) {
        this.selectedSector.set(sectors[0].label);
      }
      if (!this.selectedGroup()) {
        this.selectedGroup.set('');
      }
      this.render();
    } catch {
      this.sectors.set([]);
      this.render();
    }
  }

  async reloadFromApi() {
    this.loading.set(true);
    this.error.set('');
    this.render();
    try {
      const data = this.query().trim()
        ? await this.api.searchListings(this.query().trim())
        : await this.api.getListings();
      this.allRows.set(data || []);
      this.page.set(1);
    } catch (e: any) {
      this.allRows.set([]);
      this.error.set(e?.message || 'failed_to_load');
    } finally {
      this.loading.set(false);
      this.render();
    }
  }

  onSectorChange(value: string) {
    this.selectedSector.set(value);
    this.selectedGroup.set('');
    this.selectedItemType.set('');
    this.page.set(1);
  }

  onGroupChange(value: string) {
    this.selectedGroup.set(value);
    this.selectedItemType.set('');
    this.page.set(1);
  }

  onItemTypeChange(value: string) {
    this.selectedItemType.set(value);
    this.page.set(1);
  }

  onQueryChange() {
    this.searchSubject.next();
  }

  openListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  clear() {
    this.query.set('');
    this.selectedGroup.set('');
    this.selectedItemType.set('');
    this.onlyAvailable.set(true);
    this.partnerOnly.set(false);
    this.listingType.set('ALL');
    this.minRate.set(null);
    this.maxRate.set(null);
    this.sortBy.set('RELEVANCE');
    this.pageSize.set(10);
    this.page.set(1);
    this.searchSubject.next();
  }

  toggleFilters() {
    this.filterOpen.update(v => !v);
  }

  setPage(p: number) {
    const next = Math.min(this.totalPages(), Math.max(1, Number(p || 1)));
    this.page.set(next);
  }

  onFilterChanged() {
    this.page.set(1);
  }
}
