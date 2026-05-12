import { Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class SettingsConfigService {
  config = signal<any | null>(null);
  loading = signal(false);
  private warnedMissing = new Set<string>();

  constructor(private api: ApiService) {}

  async ensureLoaded() {
    if (this.config()) return;
    await this.reload();
  }

  async reload() {
    this.loading.set(true);
    try {
      const cfg = await this.api.getSettingsConfig();
      this.config.set(cfg);
    } catch {
      this.config.set(null);
    } finally {
      this.loading.set(false);
    }
  }

  isTabEnabled(tab: string): boolean {
    const cfg = this.config();
    if (!cfg) return true;
    if (!cfg.tabs) return true;

    const t = cfg.tabs[tab];
    if (t && typeof t === 'object' && 'enabled' in t) {
      return (t as any).enabled !== false && (t as any).enabled !== 'false';
    }
    const flatKey = `${tab}.enabled`;
    if (cfg.tabs && typeof cfg.tabs === 'object' && flatKey in cfg.tabs) {
      return cfg.tabs[flatKey] !== false && cfg.tabs[flatKey] !== 'false';
    }
    return true;
    if (typeof t === 'boolean') return t !== false;
  }


  isSectionEnabled(tab: string, section: string): boolean {
    const cfg = this.config();
    if (!cfg) return false;
    const tabConfig = cfg?.[tab];
    if (!tabConfig) return false;

    const parts = String(section).split('.').filter(Boolean);
    let current: any = tabConfig;
    let found = true;
    for (const part of parts) {
      if (current && typeof current === 'object' && part in current) {
        current = current[part];
      } else {
        found = false;
        break;
      }
    }
    if (!found) {
      const flatKey = `${section}.enabled`;
      if (tabConfig && typeof tabConfig === 'object' && flatKey in tabConfig) {
        return tabConfig[flatKey] !== false && tabConfig[flatKey] !== 'false';
      }
      const warnKey = `${tab}.${section}`;
      if (!this.warnedMissing.has(warnKey)) {
        this.warnedMissing.add(warnKey);
        console.warn(`Settings config missing for ${warnKey}, defaulting to false`);
      }
      return false;
    }

    if (current && typeof current === 'object' && 'enabled' in current) return current.enabled !== false && current.enabled !== 'false';
    if (typeof current === 'boolean') return current !== false;
    return current !== false && current !== 'false';
  }

  getSectionConfig(tab: string, section: string): any {
    const cfg = this.config();
    if (!cfg) return null;
    const tabConfig = cfg?.[tab];
    if (!tabConfig) return null;
    const parts = String(section).split('.').filter(Boolean);
    let current: any = tabConfig;
    for (const part of parts) {
      if (current && typeof current === 'object' && part in current) {
        current = current[part];
      } else {
        return null;
      }
    }
    return current;
  }
}
