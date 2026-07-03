import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  BikeShopFilterOption,
  BikeShopFilterSection,
  BikeShopSearchResponse
} from '../../models/bike';
import {
  BikeCatalogQueryState,
  BikeCatalogStateService,
  BikeCatalogVm
} from '../../services/bike-catalog-state.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { formatBikeEnumLabel } from '../../utils/bike-labels';

@Component({
  selector: 'app-bike-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './bike-search.component.html',
  styleUrl: './bike-search.component.css'
})
export class BikeSearchComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly state = inject(BikeCatalogStateService);
  private cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly vm$ = this.state.vm$;
  readonly searchForm = new FormGroup({
    query: new FormControl('', { nonNullable: true }),
    minPrice: new FormControl<number | null>(null),
    maxPrice: new FormControl<number | null>(null),
    sort: new FormControl('featured', { nonNullable: true })
  });
  readonly sortOptions = [
    { value: 'featured', label: 'Featured' },
    { value: 'newest', label: 'Newest arrivals' },
    { value: 'priceAsc', label: 'Price: Low to high' },
    { value: 'priceDesc', label: 'Price: High to low' },
    { value: 'brandAsc', label: 'Brand: A to Z' },
    { value: 'modelYearDesc', label: 'Model year: Newest' }
  ];

  readonly quickActions = [
    { value: 'COMPLETE_BIKE', label: 'Complete bike' },
    { value: 'FRAMESET', label: 'Frameset' }
  ];

  ngOnInit(): void {
    this.state.initialize(this.route);
    this.bindForm();
  }

  setViewMode(mode: 'modern' | 'traditional'): void {
    this.state.setViewMode(mode);
  }

  toggleSectionOption(section: BikeShopFilterSection, option: BikeShopFilterOption): void {
    if (option.disabled) {
      return;
    }
    if (section.key === 'saleType') {
      this.state.setSaleType(option.selected ? null : option.value);
      return;
    }
    if (section.key === 'category') {
      this.state.toggleCategory(option.value);
      return;
    }
    this.state.toggleFilterOption(section.key, option.value);
  }

  clearFilters(): void {
    this.state.clearAll();
  }

  goToPage(page: number, paginationTotalPages: number): void {
    if (page < 0 || page >= paginationTotalPages) {
      return;
    }
    this.state.setPage(page);
  }

  removeTag(sectionKey: string, value: string): void {
    this.state.removeSelection(sectionKey, value);
  }

  trackSection(_: number, section: BikeShopFilterSection): string {
    return section.key;
  }

  trackOption(_: number, option: BikeShopFilterOption): string {
    return option.value;
  }

  displaySectionLabel(section: BikeShopFilterSection): string {
    return section.displayLabel || section.label;
  }

  displayOptionLabel(section: BikeShopFilterSection, option: BikeShopFilterOption): string {
    return option.displayLabel || option.label;
  }

  activeTags(vm: BikeCatalogVm): Array<{ sectionKey: string; sectionLabel: string; value: string; label: string }> {
    const tags: Array<{ sectionKey: string; sectionLabel: string; value: string; label: string }> = [];
    const sections = vm.response?.sidebar.sections ?? [];

    for (const section of sections) {
      for (const option of section.options) {
        if (option.selected) {
          tags.push({
            sectionKey: section.key,
            sectionLabel: this.displaySectionLabel(section),
            value: option.value,
            label: this.displayOptionLabel(section, option)
          });
        }
      }
    }

    return tags;
  }

  priceHint(response: BikeShopSearchResponse | null): string {
    if (!response?.sidebar) {
      return 'Use the price range to refine premium catalog selections.';
    }

    const min = response.sidebar.minAvailablePrice;
    const max = response.sidebar.maxAvailablePrice;
    if (min == null || max == null) {
      return 'Price range updates with the active tenant and filter context.';
    }

    return `Available range ${this.formatCurrency(min)} - ${this.formatCurrency(max)}`;
  }

  formatEnum(value: string | null | undefined): string {
    return formatBikeEnumLabel(value);
  }

  private bindForm(): void {
    this.state.state$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => {
        this.searchForm.patchValue(
          {
            query: state.query,
            minPrice: state.minPrice,
            maxPrice: state.maxPrice,
            sort: state.sort
          },
          { emitEvent: false }
        );
        this.render();
      });

    this.searchForm.controls.query.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.state.setQuery(value));

    this.searchForm.controls.minPrice.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.state.setPrice('minPrice', this.normalizeNumber(value)));

    this.searchForm.controls.maxPrice.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.state.setPrice('maxPrice', this.normalizeNumber(value)));

    this.searchForm.controls.sort.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.state.setSort(value));
  }

  private normalizeNumber(value: number | null): number | null {
    return value == null || Number.isNaN(value) ? null : value;
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-DE', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(value);
  }

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }
}
