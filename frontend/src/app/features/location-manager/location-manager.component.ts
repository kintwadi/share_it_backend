import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, CheckCircle2, Loader2, MapPin, Pencil, Plus, Search, Trash2, X } from 'lucide-angular';
import { Subject, debounceTime } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { LocationApiService, LocationResponse } from '../../core/services/location-api.service';
import { ExchangeLocation } from '../../core/models/types';
import { ButtonComponent } from '../../shared/components/button/button';

type Tab = 'REGISTER' | 'LIST';

@Component({
  selector: 'app-location-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ButtonComponent],
  templateUrl: './location-manager.component.html',
  styleUrl: './location-manager.component.css'
})
export class LocationManagerComponent implements OnInit {
  private api = inject(ApiService);
  private locationApi = inject(LocationApiService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly CheckCircle2 = CheckCircle2;
  readonly Loader2 = Loader2;
  readonly MapPin = MapPin;
  readonly Pencil = Pencil;
  readonly Plus = Plus;
  readonly Search = Search;
  readonly Trash2 = Trash2;
  readonly X = X;

  activeTab: Tab = 'REGISTER';

  loading = false;
  error: string | null = null;
  notice: { type: 'success' | 'error'; message: string } | null = null;

  name = '';
  query = '';
  suggestions: LocationResponse[] = [];
  selected: LocationResponse | null = null;

  streetAddress = '';
  city = '';
  postalCode = '';
  country = '';
  latitude: number | null = null;
  longitude: number | null = null;
  active = true;

  listLoading = false;
  locations: ExchangeLocation[] = [];

  editId: string | null = null;
  editModel: any = null;
  editSuggestions: LocationResponse[] = [];

  private query$ = new Subject<string>();
  private editQuery$ = new Subject<string>();

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    this.query$.pipe(debounceTime(250)).subscribe(q => {
      this.loadSuggestions(q);
    });
    this.editQuery$.pipe(debounceTime(250)).subscribe(q => {
      this.loadEditSuggestions(q);
    });
  }

  setTab(tab: Tab) {
    this.activeTab = tab;
    this.error = null;
    this.notice = null;
    this.render();
    if (tab === 'LIST') {
      this.loadList();
    }
  }

  goBack() {
    this.router.navigate(['/admin']);
  }

  onQueryChange(v: string) {
    this.query = v;
    this.query$.next(v);
  }

  private async loadSuggestions(q: string) {
    const query = String(q || '').trim();
    if (query.length < 2) {
      this.suggestions = [];
      this.render();
      return;
    }
    try {
      this.suggestions = await this.locationApi.autocomplete(query, undefined, 6);
      this.render();
    } catch {
      this.suggestions = [];
      this.render();
    }
  }

  selectSuggestion(s: LocationResponse) {
    this.selected = s;
    this.query = String(s.displayName || '');
    this.suggestions = [];
    this.streetAddress = String(s.streetAddress || '');
    this.city = String(s.city || '');
    this.postalCode = String(s.postalCode || '');
    this.country = String(s.country || '');
    this.latitude = typeof s.latitude === 'number' ? s.latitude : null;
    this.longitude = typeof s.longitude === 'number' ? s.longitude : null;
    this.render();
  }

  clearSelected() {
    this.selected = null;
    this.query = '';
    this.suggestions = [];
    this.latitude = null;
    this.longitude = null;
    this.render();
  }

  private buildAddress(): string {
    const s = String(this.streetAddress || '').trim();
    const c = String(this.city || '').trim();
    const p = String(this.postalCode || '').trim();
    const co = String(this.country || '').trim();
    const parts = [s, c].filter(Boolean);
    const tail = [p, co].filter(Boolean).join(' ');
    const head = parts.join(', ');
    if (head && tail) return `${head}, ${tail}`.trim();
    return (head || tail || '').trim();
  }

  async createLocation() {
    if (this.loading) return;
    this.loading = true;
    this.error = null;
    this.notice = null;
    this.render();
    try {
      const name = String(this.name || '').trim();
      const address = this.buildAddress();
      const payload = {
        name,
        address: address || (this.selected?.displayName ?? null),
        streetAddress: this.streetAddress || null,
        city: this.city || null,
        postalCode: this.postalCode || null,
        country: this.country || null,
        latitude: this.latitude,
        longitude: this.longitude,
        active: this.active
      };
      const created = await this.api.adminCreateExchangeLocation(payload);
      this.notice = { type: 'success', message: `${this.i18n.t('location_manager.created')}: ${created.referenceId}` };
      this.name = '';
      this.clearSelected();
      this.streetAddress = '';
      this.city = '';
      this.postalCode = '';
      this.country = '';
      this.active = true;
      this.render();
      this.setTab('LIST');
    } catch (e: any) {
      this.error = e?.error?.error || e?.message || this.i18n.t('location_manager.error_save');
      this.render();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async loadList() {
    if (this.listLoading) return;
    this.listLoading = true;
    this.error = null;
    this.render();
    try {
      const rows = await this.api.adminListExchangeLocations();
      this.locations = Array.isArray(rows) ? rows : [];
      this.render();
    } catch (e: any) {
      this.error = e?.error?.error || e?.message || this.i18n.t('location_manager.error_load');
      this.render();
    } finally {
      this.listLoading = false;
      this.render();
    }
  }

  startEdit(row: ExchangeLocation) {
    this.editId = String(row.id);
    this.editModel = {
      id: row.id,
      referenceId: row.referenceId,
      name: row.name,
      streetAddress: row.streetAddress || '',
      city: row.city || '',
      postalCode: row.postalCode || '',
      country: row.country || '',
      address: row.address,
      latitude: row.location?.x ?? null,
      longitude: row.location?.y ?? null,
      active: !!row.active,
      query: row.address
    };
    this.editSuggestions = [];
    this.notice = null;
    this.error = null;
    this.render();
  }

  cancelEdit() {
    this.editId = null;
    this.editModel = null;
    this.editSuggestions = [];
    this.render();
  }

  onEditQueryChange(v: string) {
    if (!this.editModel) return;
    this.editModel.query = v;
    this.editQuery$.next(v);
  }

  private async loadEditSuggestions(q: string) {
    const query = String(q || '').trim();
    if (query.length < 2) {
      this.editSuggestions = [];
      this.render();
      return;
    }
    try {
      this.editSuggestions = await this.locationApi.autocomplete(query, undefined, 6);
      this.render();
    } catch {
      this.editSuggestions = [];
      this.render();
    }
  }

  selectEditSuggestion(s: LocationResponse) {
    if (!this.editModel) return;
    this.editModel.query = String(s.displayName || '');
    this.editSuggestions = [];
    this.editModel.streetAddress = String(s.streetAddress || '');
    this.editModel.city = String(s.city || '');
    this.editModel.postalCode = String(s.postalCode || '');
    this.editModel.country = String(s.country || '');
    this.editModel.latitude = typeof s.latitude === 'number' ? s.latitude : null;
    this.editModel.longitude = typeof s.longitude === 'number' ? s.longitude : null;
    this.render();
  }

  private buildEditAddress(): string {
    if (!this.editModel) return '';
    const s = String(this.editModel.streetAddress || '').trim();
    const c = String(this.editModel.city || '').trim();
    const p = String(this.editModel.postalCode || '').trim();
    const co = String(this.editModel.country || '').trim();
    const parts = [s, c].filter(Boolean);
    const tail = [p, co].filter(Boolean).join(' ');
    const head = parts.join(', ');
    if (head && tail) return `${head}, ${tail}`.trim();
    return (head || tail || '').trim();
  }

  async saveEdit() {
    if (!this.editModel || !this.editId) return;
    this.loading = true;
    this.error = null;
    this.notice = null;
    this.render();
    try {
      const address = this.buildEditAddress();
      const payload = {
        name: String(this.editModel.name || '').trim(),
        address: address || String(this.editModel.address || '').trim() || String(this.editModel.query || '').trim(),
        streetAddress: this.editModel.streetAddress || null,
        city: this.editModel.city || null,
        postalCode: this.editModel.postalCode || null,
        country: this.editModel.country || null,
        latitude: this.editModel.latitude,
        longitude: this.editModel.longitude,
        active: !!this.editModel.active
      };
      await this.api.adminUpdateExchangeLocation(this.editId, payload);
      this.notice = { type: 'success', message: this.i18n.t('location_manager.updated') };
      await this.loadList();
      this.cancelEdit();
    } catch (e: any) {
      this.error = e?.error?.error || e?.message || this.i18n.t('location_manager.error_save');
      this.render();
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async delete(row: ExchangeLocation) {
    const id = String(row?.id || '');
    if (!id) return;
    this.loading = true;
    this.error = null;
    this.notice = null;
    this.render();
    try {
      await this.api.adminDeleteExchangeLocation(id);
      this.notice = { type: 'success', message: this.i18n.t('location_manager.deleted') };
      await this.loadList();
      if (this.editId === id) this.cancelEdit();
    } catch (e: any) {
      this.error = e?.error?.error || e?.message || this.i18n.t('location_manager.error_delete');
      this.render();
    } finally {
      this.loading = false;
      this.render();
    }
  }
}
