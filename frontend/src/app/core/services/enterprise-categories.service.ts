import { Injectable, inject } from '@angular/core';
import { ApiClientService } from './api-client.service';
import { firstValueFrom } from 'rxjs';

export type EnterpriseCategoryItem = {
  label: string;
  keywords: string[];
};

export type EnterpriseCategoryGroup = {
  label: string;
  items: EnterpriseCategoryItem[];
};

export type EnterpriseCategorySector = {
  label: string;
  groups: EnterpriseCategoryGroup[];
};

@Injectable({
  providedIn: 'root'
})
export class EnterpriseCategoriesService {
  private api = inject(ApiClientService);
  private cached: EnterpriseCategorySector[] | null = null;
  private loading: Promise<EnterpriseCategorySector[]> | null = null;

  async load(): Promise<EnterpriseCategorySector[]> {
    if (this.cached) return this.cached;
    if (this.loading) return this.loading;
    this.loading = this.loadInternal().finally(() => {
      this.loading = null;
    });
    this.cached = await this.loading;
    return this.cached;
  }

  private async loadInternal(): Promise<EnterpriseCategorySector[]> {
    try {
      return await firstValueFrom(this.api.get<EnterpriseCategorySector[]>('/enterprise/categories'));
    } catch {
      return [];
    }
  }
}

