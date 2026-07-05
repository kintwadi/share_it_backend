import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';

function getByPath(obj: any, path: string): any {
  if (!obj || !path) return obj;
  const parts = String(path).split('.').map(p => p.trim()).filter(Boolean);
  let cur = obj;
  for (const p of parts) {
    if (cur == null) return undefined;
    cur = cur[p];
  }
  return cur;
}

function isEnabledValue(v: any): boolean {
  if (v == null) return true;
  if (typeof v === 'boolean') return v;
  if (typeof v === 'number') return v !== 0;
  const s = String(v).trim().toLowerCase();
  if (s === 'false' || s === '0' || s === 'off' || s === 'disabled') return false;
  if (s === 'true' || s === '1' || s === 'on' || s === 'enabled') return true;
  return true;
}

@Injectable({
  providedIn: 'root'
})
export class SettingsConfigService {
  private api = inject(ApiService);

  private loaded = false;
  private loadPromise: Promise<void> | null = null;

  private configSignal = signal<any>({});
  config = this.configSignal.asReadonly();

  async ensureLoaded(): Promise<void> {
    if (this.loaded) return;
    if (!this.loadPromise) {
      this.loadPromise = this.reload().finally(() => {
        this.loadPromise = null;
      });
    }
    return this.loadPromise;
  }

  async reload(): Promise<void> {
    try {
      const cfg = await this.api.getSettingsConfig();
      this.configSignal.set(cfg || {});
    } catch {
      this.configSignal.set({});
    } finally {
      this.loaded = true;
    }
  }

  isTabEnabled(tabId: string): boolean {
    const cfg = this.configSignal();
    const tab = cfg?.tabs?.[tabId];
    if (tab && typeof tab === 'object' && 'enabled' in tab) return isEnabledValue(tab.enabled);
    return isEnabledValue(tab);
  }

  isSectionEnabled(section: string, key?: string): boolean {
    const cfg = this.configSignal();
    const root = cfg?.[section];
    if (key == null || key === '') {
      if (root && typeof root === 'object' && 'enabled' in root) return isEnabledValue((root as any).enabled);
      return isEnabledValue(root);
    }

    const v = getByPath(root, key);
    if (v && typeof v === 'object' && 'enabled' in v) return isEnabledValue((v as any).enabled);
    return isEnabledValue(v);
  }

  getNumber(section: string, key: string, defaultValue: number): number {
    const cfg = this.configSignal();
    const root = cfg?.[section];
    const v = getByPath(root, key);
    const n = Number(v);
    if (!Number.isNaN(n)) return n;
    return defaultValue;
  }

  getBoolean(section: string, key: string, defaultValue: boolean): boolean {
    const cfg = this.configSignal();
    const root = cfg?.[section];
    const v = getByPath(root, key);
    if (v == null) return defaultValue;
    return isEnabledValue(v);
  }
}
