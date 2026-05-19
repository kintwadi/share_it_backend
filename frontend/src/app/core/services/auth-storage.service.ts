import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private readonly TOKEN_KEY = 'nearshare_token';
  private readonly USER_ID_KEY = 'nearshare_current_user_id';
  private readonly NOTIFICATIONS_KEY = 'nearshare_notifications';
  private readonly AUTH_CONTEXT_KEY = 'nearshare_auth_context';
  authContext = signal<'user' | 'admin' | 'partner' | null>(this.readAuthContext());

  private readAuthContext(): 'user' | 'admin' | 'partner' | null {
    const raw = sessionStorage.getItem(this.AUTH_CONTEXT_KEY) || localStorage.getItem(this.AUTH_CONTEXT_KEY);
    const v = String(raw || '').toLowerCase();
    if (v === 'user' || v === 'admin' || v === 'partner') return v;
    return null;
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY) || localStorage.getItem(this.TOKEN_KEY);
  }

  setToken(token: string, rememberMe: boolean = false): void {
    if (rememberMe) {
      localStorage.setItem(this.TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  clear(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_ID_KEY);
    localStorage.removeItem(this.USER_ID_KEY);
    sessionStorage.removeItem(this.AUTH_CONTEXT_KEY);
    localStorage.removeItem(this.AUTH_CONTEXT_KEY);
    this.authContext.set(null);
  }

  getUserId(): string | null {
    return sessionStorage.getItem(this.USER_ID_KEY) || localStorage.getItem(this.USER_ID_KEY);
  }

  setUserId(id: string, rememberMe: boolean = false): void {
    if (rememberMe) {
      localStorage.setItem(this.USER_ID_KEY, id);
    } else {
      sessionStorage.setItem(this.USER_ID_KEY, id);
    }
  }

  getAuthContext(): 'user' | 'admin' | 'partner' | null {
    return this.authContext();
  }

  setAuthContext(ctx: 'user' | 'admin' | 'partner', rememberMe: boolean = false): void {
    if (rememberMe) {
      localStorage.setItem(this.AUTH_CONTEXT_KEY, ctx);
    } else {
      sessionStorage.setItem(this.AUTH_CONTEXT_KEY, ctx);
    }
    this.authContext.set(ctx);
  }
}
