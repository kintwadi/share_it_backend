import { Injectable, inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, from, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';
import {
  BikeShopFilterSection,
  BikeShopSearchRequest,
  BikeShopSearchResponse,
  BikeShopSidebar
} from '../models/bike';
import { BikeApiService } from './bike-api.service';

type ViewMode = 'modern' | 'traditional';

export interface BikeCatalogQueryState {
  query: string;
  saleType: string | null;
  categories: string[];
  minPrice: number | null;
  maxPrice: number | null;
  sort: string;
  page: number;
  size: number;
  filters: Record<string, string[]>;
}

export interface BikeCatalogVm {
  state: BikeCatalogQueryState;
  response: BikeShopSearchResponse | null;
  loading: boolean;
  error: string;
  viewMode: ViewMode;
  hasActiveFilters: boolean;
}

const DEFAULT_STATE: BikeCatalogQueryState = {
  query: '',
  saleType: null,
  categories: [],
  minPrice: null,
  maxPrice: null,
  sort: 'featured',
  page: 0,
  size: 12,
  filters: {}
};

@Injectable({ providedIn: 'root' })
export class BikeCatalogStateService {
  private readonly api = inject(BikeApiService);
  private readonly router = inject(Router);

  private readonly initializedSubject = new BehaviorSubject(false);
  private readonly stateSubject = new BehaviorSubject<BikeCatalogQueryState>(DEFAULT_STATE);
  private readonly responseSubject = new BehaviorSubject<BikeShopSearchResponse | null>(null);
  private readonly loadingSubject = new BehaviorSubject(false);
  private readonly errorSubject = new BehaviorSubject('');
  private readonly viewModeSubject = new BehaviorSubject<ViewMode>('traditional');

  private route: ActivatedRoute | null = null;

  readonly state$ = this.stateSubject.asObservable();
  readonly viewMode$ = this.viewModeSubject.asObservable();
  readonly vm$ = combineLatest([
    this.stateSubject,
    this.responseSubject,
    this.loadingSubject,
    this.errorSubject,
    this.viewModeSubject
  ]).pipe(
    map(([state, response, loading, error, viewMode]) => ({
      state,
      response,
      loading,
      error,
      viewMode,
      hasActiveFilters: this.hasActiveFilters(state)
    }))
  );

  constructor() {
    combineLatest([
      this.initializedSubject.pipe(filter(Boolean)),
      this.stateSubject.pipe(
        map((state) => ({
          query: state.query,
          saleType: state.saleType,
          categories: state.categories,
          sort: state.sort,
          page: state.page,
          size: state.size,
          filters: state.filters
        })),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
      ),
      this.stateSubject.pipe(
        map((state) => ({ minPrice: state.minPrice, maxPrice: state.maxPrice })),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        debounceTime(300)
      )
    ]).pipe(
      map(([, nonPrice, price]) => ({ ...nonPrice, ...price }) as BikeCatalogQueryState),
      tap((state) => this.syncUrl(state)),
      tap(() => {
        this.loadingSubject.next(true);
        this.errorSubject.next('');
      }),
      switchMap((state) =>
        from(this.api.searchShopCatalog(this.toRequest(state))).pipe(
          map((response) => ({ state, response, error: '' })),
          catchError(() =>
            of({
              state,
              response: null,
              error: 'Unable to load the bicycle catalog right now.'
            })
          )
        )
      )
    ).subscribe(({ state, response, error }) => {
      if (response) {
        const sanitizedState = this.sanitizeStateAgainstSidebar(state, response.sidebar);
        this.responseSubject.next(response);
        if (!this.isSameState(sanitizedState, state)) {
          this.stateSubject.next(sanitizedState);
        }
      } else {
        this.responseSubject.next(null);
      }
      this.errorSubject.next(error);
      this.loadingSubject.next(false);
    });
  }

  initialize(route: ActivatedRoute): void {
    this.route = route;
    this.stateSubject.next(this.parseQueryParams(route.snapshot.queryParams));
    this.initializedSubject.next(true);
  }

  setViewMode(mode: ViewMode): void {
    this.viewModeSubject.next(mode);
  }

  setQuery(query: string): void {
    this.patchState({ query: query.trim(), page: 0 });
  }

  setSaleType(saleType: string | null): void {
    this.patchState({ saleType: saleType || null, page: 0 });
  }

  setSort(sort: string): void {
    this.patchState({ sort, page: 0 }, false);
  }

  setPrice(field: 'minPrice' | 'maxPrice', value: number | null): void {
    this.patchState({ [field]: value, page: 0 } as Partial<BikeCatalogQueryState>);
  }

  setPage(page: number): void {
    this.patchState({ page }, false);
  }

  toggleCategory(category: string): void {
    const categories = this.toggleArrayValue(this.stateSubject.value.categories, category);
    this.patchState({ categories, page: 0 });
  }

  toggleFilterOption(sectionKey: string, optionValue: string): void {
    const current = { ...this.stateSubject.value.filters };
    const nextValues = this.toggleArrayValue(current[sectionKey] ?? [], optionValue);
    if (nextValues.length) {
      current[sectionKey] = nextValues;
    } else {
      delete current[sectionKey];
    }
    this.patchState({ filters: current, page: 0 });
  }

  clearAll(): void {
    this.stateSubject.next({ ...DEFAULT_STATE });
  }

  removeSelection(sectionKey: string, optionValue: string): void {
    if (sectionKey === 'category') {
      this.toggleCategory(optionValue);
      return;
    }
    if (sectionKey === 'saleType') {
      this.setSaleType(null);
      return;
    }
    this.toggleFilterOption(sectionKey, optionValue);
  }

  private patchState(partial: Partial<BikeCatalogQueryState>, resetPage = true): void {
    const current = this.stateSubject.value;
    const next: BikeCatalogQueryState = {
      ...current,
      ...partial,
      page: resetPage ? (partial.page ?? 0) : (partial.page ?? current.page)
    };
    this.stateSubject.next(next);
  }

  private toRequest(state: BikeCatalogQueryState): BikeShopSearchRequest {
    return {
      query: state.query || null,
      saleType: state.saleType,
      categories: state.categories,
      minPrice: state.minPrice,
      maxPrice: state.maxPrice,
      sort: state.sort,
      page: state.page,
      size: state.size,
      filters: Object.entries(state.filters).map(([key, values]) => ({ key, values }))
    };
  }

  private parseQueryParams(queryParams: Params): BikeCatalogQueryState {
    const filters: Record<string, string[]> = {};
    Object.entries(queryParams).forEach(([key, value]) => {
      if (!key.startsWith('f_')) {
        return;
      }
      const parsed = String(value)
        .split(',')
        .map((entry) => entry.trim())
        .filter(Boolean);
      if (parsed.length) {
        filters[key.slice(2)] = parsed;
      }
    });

    return {
      query: String(queryParams['q'] ?? '').trim(),
      saleType: queryParams['saleType'] ? String(queryParams['saleType']) : null,
      categories: queryParams['category'] ? String(queryParams['category']).split(',').filter(Boolean) : [],
      minPrice: queryParams['minPrice'] ? Number(queryParams['minPrice']) : null,
      maxPrice: queryParams['maxPrice'] ? Number(queryParams['maxPrice']) : null,
      sort: String(queryParams['sort'] ?? 'featured'),
      page: queryParams['page'] ? Math.max(Number(queryParams['page']) || 0, 0) : 0,
      size: 12,
      filters
    };
  }

  private syncUrl(state: BikeCatalogQueryState): void {
    if (!this.route) {
      return;
    }

    const queryParams: Params = {
      q: state.query || null,
      saleType: state.saleType,
      category: state.categories.length ? state.categories.join(',') : null,
      minPrice: state.minPrice,
      maxPrice: state.maxPrice,
      sort: state.sort !== 'featured' ? state.sort : null,
      page: state.page > 0 ? state.page : null
    };

    Object.entries(state.filters).forEach(([key, values]) => {
      queryParams[`f_${key}`] = values.length ? values.join(',') : null;
    });

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: '',
      replaceUrl: true
    });
  }

  private sanitizeStateAgainstSidebar(state: BikeCatalogQueryState, sidebar: BikeShopSidebar): BikeCatalogQueryState {
    const allowedSections = new Map(sidebar.sections.map((section) => [section.key, section]));
    const nextFilters: Record<string, string[]> = {};

    Object.entries(state.filters).forEach(([key, values]) => {
      const section = allowedSections.get(key);
      if (!section) {
        return;
      }
      const allowedOptions = new Set(section.options.map((option) => option.value));
      const filteredValues = values.filter((value) => allowedOptions.has(value));
      if (filteredValues.length) {
        nextFilters[key] = filteredValues;
      }
    });

    return {
      ...state,
      filters: nextFilters
    };
  }

  private toggleArrayValue(values: string[], value: string): string[] {
    return values.includes(value)
      ? values.filter((item) => item !== value)
      : [...values, value];
  }

  private hasActiveFilters(state: BikeCatalogQueryState): boolean {
    return Boolean(
      state.query ||
      state.saleType ||
      state.categories.length ||
      state.minPrice !== null ||
      state.maxPrice !== null ||
      Object.keys(state.filters).length
    );
  }

  private isSameState(a: BikeCatalogQueryState, b: BikeCatalogQueryState): boolean {
    return JSON.stringify(a) === JSON.stringify(b);
  }
}
