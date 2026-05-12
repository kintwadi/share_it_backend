import { Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AuthStorageService } from './auth-storage.service';
import { User } from '../models/types';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  user = signal<User | null>(null);
  subscription = signal<any | null>(null);
  loading = signal(false);

  constructor(
    private api: ApiService,
    private authStorage: AuthStorageService,
  ) {}

  async refresh() {
    const token = this.authStorage.getToken();
    if (!token) {
      this.user.set(null);
      this.subscription.set(null);
      return;
    }
    this.loading.set(true);
    try {
      const u = await this.api.getCurrentUser();
      this.user.set(u);
      if (u) {
        const sub = await this.api.getCurrentSubscription();
        this.subscription.set(sub);
      } else {
        this.subscription.set(null);
      }
    } finally {
      this.loading.set(false);
    }
  }

  logout() {
    this.authStorage.clear();
    this.user.set(null);
    this.subscription.set(null);
  }
}

