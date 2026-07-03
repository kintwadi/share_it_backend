import { Injectable } from '@angular/core';
import { AuthSession } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthStorageService {
  private readonly storageKey = 'frontend-bike.session';

  read(): AuthSession | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  write(session: AuthSession): void {
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }

  clear(): void {
    localStorage.removeItem(this.storageKey);
  }
}
