import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AuthStorageService } from './auth-storage.service';
import { User } from '../models/types';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private api = inject(ApiService);
  private authStorage = inject(AuthStorageService);

  user = signal<User | null>(null);
  subscription = signal<any | null>(null);

  private refreshPromise: Promise<void> | null = null;

  async refresh(): Promise<void> {
    if (this.refreshPromise) return this.refreshPromise;
    this.refreshPromise = this.doRefresh().finally(() => {
      this.refreshPromise = null;
    });
    return this.refreshPromise;
  }

  private async doRefresh(): Promise<void> {
    const token = this.authStorage.getToken();
    if (!token) {
      this.user.set(null);
      this.subscription.set(null);
      return;
    }

    try {
      const [u, sub] = await Promise.all([
        this.api.getCurrentUser().catch(() => null),
        this.api.getCurrentSubscription().catch(() => null)
      ]);
      this.user.set(u);
      this.subscription.set(sub);
    } catch {
      this.user.set(null);
      this.subscription.set(null);
    }
  }

  logout() {
    this.authStorage.clear();
    this.authStorage.setAuthContext('');
    this.user.set(null);
    this.subscription.set(null);
  }
}
