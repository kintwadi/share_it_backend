import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private readonly TOKEN_KEY = 'nearshare_token';
  private readonly USER_ID_KEY = 'nearshare_current_user_id';
  private readonly NOTIFICATIONS_KEY = 'nearshare_notifications';

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
}
