import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { getRuntimeEnv } from '../config/runtime-env';
import { StoreCategory, StoreProduct, StoreProductVariant, StoreSummary } from '../models/catalog.models';

@Injectable({ providedIn: 'root' })
export class LinkedStoreApiService {
  private readonly http = inject(HttpClient);
  private readonly env = getRuntimeEnv();

  getStores(): Observable<StoreSummary[]> {
    return this.http.get<StoreSummary[]>(`${this.env.apiUrl}/stores`);
  }

  getCategories(storeId: number): Observable<StoreCategory[]> {
    return this.http.get<StoreCategory[]>(`${this.env.apiUrl}/categories`, {
      headers: this.storeHeaders(storeId)
    });
  }

  getProducts(storeId: number, categoryId?: number | null): Observable<StoreProduct[]> {
    let params = new HttpParams();
    if (categoryId) {
      params = params.set('categoryId', categoryId);
    }

    return this.http.get<StoreProduct[]>(`${this.env.apiUrl}/products`, {
      headers: this.storeHeaders(storeId),
      params
    });
  }

  getVariants(storeId: number, productId: number): Observable<StoreProductVariant[]> {
    return this.http.get<StoreProductVariant[]>(`${this.env.apiUrl}/products/${productId}/variants`, {
      headers: this.storeHeaders(storeId)
    });
  }

  getDefaultStoreId(): number | null {
    const parsed = Number(this.env.defaultStoreId);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private storeHeaders(storeId: number): HttpHeaders {
    return new HttpHeaders({
      [this.env.storeHeaderName]: String(storeId)
    });
  }
}
