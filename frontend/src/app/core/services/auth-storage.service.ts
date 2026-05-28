import { Injectable, signal } from '@angular/core';

type AuthContext = 'user' | 'admin' | 'partner' | '';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private tokenKey = 'auth_token';
  private userIdKey = 'auth_user_id';
  private contextKey = 'auth_context';

  private authContextSignal = signal<AuthContext>(this.readContext());

  authContext = this.authContextSignal.asReadonly();

  getToken(): string | null {
    try {
      const t = localStorage.getItem(this.tokenKey);
      return t && t.trim() ? t : null;
    } catch {
      return null;
    }
  }

  setToken(token: string | null) {
    try {
      if (!token || !String(token).trim()) {
        localStorage.removeItem(this.tokenKey);
        return;
      }
      localStorage.setItem(this.tokenKey, String(token));
    } catch { }
  }

  getUserId(): string | null {
    try {
      const id = localStorage.getItem(this.userIdKey);
      return id && id.trim() ? id : null;
    } catch {
      return null;
    }
  }

  setUserId(userId: string | null) {
    try {
      if (!userId || !String(userId).trim()) {
        localStorage.removeItem(this.userIdKey);
        return;
      }
      localStorage.setItem(this.userIdKey, String(userId));
    } catch { }
  }

  clear() {
    try {
      localStorage.removeItem(this.tokenKey);
      localStorage.removeItem(this.userIdKey);
    } catch { }
  }

  getAuthContext(): AuthContext {
    return this.authContextSignal();
  }

  setAuthContext(ctx: AuthContext) {
    const next = (ctx || '') as AuthContext;
    this.authContextSignal.set(next);
    try {
      if (!next) {
        localStorage.removeItem(this.contextKey);
      } else {
        localStorage.setItem(this.contextKey, next);
      }
    } catch { }
  }

  private readContext(): AuthContext {
    try {
      const raw = String(localStorage.getItem(this.contextKey) || '').trim();
      if (raw === 'user' || raw === 'admin' || raw === 'partner') return raw;
      return '';
    } catch {
      return '';
    }
  }
}
