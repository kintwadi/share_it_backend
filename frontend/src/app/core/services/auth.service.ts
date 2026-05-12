import { Injectable, inject } from '@angular/core';
import { ApiClientService } from './api-client.service';
import { Observable, tap } from 'rxjs';
import { User } from '../models/types';
import { AuthStorageService } from './auth-storage.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private api = inject(ApiClientService);
  private authStorage = inject(AuthStorageService);

  login(email: string, password: string): Observable<{ token: string; userId: string }> {
    return this.api.post<{ token: string; userId: string }>('/auth/login', { email, password }).pipe(
      tap(res => {
        this.authStorage.setToken(res.token);
        this.authStorage.setUserId(res.userId);
      })
    );
  }

  getCurrentUser(): Observable<User> {
    return this.api.get<User>('/users/me');
  }

  logout(): void {
    this.authStorage.clear();
  }
}
