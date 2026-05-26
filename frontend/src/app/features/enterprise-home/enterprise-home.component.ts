import { ChangeDetectorRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Search, Building2, ChevronDown } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { EnterpriseCategoriesService, EnterpriseCategorySector } from '../../core/services/enterprise-categories.service';
import { I18nService } from '../../core/services/i18n.service';
import { AvailabilityStatus, Listing, ListingType } from '../../core/models/types';

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

  loading = signal(true);
  error = signal('');

  sectors = signal<EnterpriseCategorySector[]>([]);
  selectedSector = signal<string>('');
  selectedItemType = signal<string>('');

  query = signal('');
  onlyAvailable = signal(true);
  listingType = signal<'ALL' | ListingType>('ALL');

  rows = signal<Listing[]>([]);

  sectorItems = computed(() => {
    const s = this.sectors().find(x => x.label === this.selectedSector());
    if (!s) return [];
    const all = s.groups.flatMap(g => g.items.map(i => i.label));
    return Array.from(new Set(all));
  });

  async ngOnInit() {
    await this.loadCategories();
    await this.loadListings();
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
      this.render();
    } catch {
      this.sectors.set([]);
      this.render();
    }
  }

  async loadListings() {
    this.loading.set(true);
    this.error.set('');
    this.render();
    try {
      const data = this.query().trim()
        ? await this.api.searchListings(this.query().trim())
        : await this.api.getListings();

      let filtered = data.filter(l =>
        l.status !== AvailabilityStatus.BLOCKED &&
        l.status !== AvailabilityStatus.HIDDEN
      );

      if (this.onlyAvailable()) {
        filtered = filtered.filter(l => l.status === AvailabilityStatus.AVAILABLE);
      }

      if (this.listingType() !== 'ALL') {
        filtered = filtered.filter(l => l.type === this.listingType());
      }

      const itemType = this.selectedItemType().trim();
      if (itemType) {
        const q = itemType.toLowerCase();
        filtered = filtered.filter(l => {
          const hay = `${l.title || ''} ${l.category || ''} ${l.description || ''}`.toLowerCase();
          return hay.includes(q);
        });
      }

      const sector = this.selectedSector().trim();
      if (sector) {
        const q = sector.toLowerCase();
        filtered = filtered.filter(l => {
          const hay = `${l.title || ''} ${l.category || ''} ${l.description || ''}`.toLowerCase();
          return hay.includes(q) || !!l.partnerId;
        });
      }

      this.rows.set(filtered);
    } catch (e: any) {
      this.rows.set([]);
      this.error.set(e?.message || 'failed_to_load');
    } finally {
      this.loading.set(false);
      this.render();
    }
  }

  onSectorChange(value: string) {
    this.selectedSector.set(value);
    this.selectedItemType.set('');
    this.loadListings();
  }

  onItemTypeChange(value: string) {
    this.selectedItemType.set(value);
    this.loadListings();
  }

  onQueryChange() {
    this.loadListings();
  }

  openListing(id: string) {
    this.router.navigate(['/listing', id]);
  }

  clear() {
    this.query.set('');
    this.selectedItemType.set('');
    this.onlyAvailable.set(true);
    this.listingType.set('ALL');
    this.loadListings();
  }
}

