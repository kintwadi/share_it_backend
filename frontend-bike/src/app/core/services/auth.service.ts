import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthSession, LoginPayload, RegisterPayload, TokenResponse } from '../models/auth.models';
import { ApiClientService } from './api-client.service';
import { AuthStorageService } from './auth-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiClientService);
  private readonly storage = inject(AuthStorageService);
  private readonly sessionState = signal<AuthSession | null>(this.storage.read());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed(() => this.sessionState()?.user ?? null);
  readonly isAuthenticated = computed(() => Boolean(this.sessionState()?.token));

  async login(payload: LoginPayload): Promise<TokenResponse> {
    const response = await firstValueFrom(this.api.post<TokenResponse>('/auth/login', payload));
    if (!response.mfaRequired && response.token && response.user) {
      this.persist({ token: response.token, user: response.user });
    }
    return response;
  }

  async register(payload: RegisterPayload): Promise<unknown> {
    return firstValueFrom(this.api.post('/auth/register', payload));
  }

  async forgotPassword(email: string): Promise<void> {
    await firstValueFrom(this.api.post('/auth/forgot-password', { email }));
  }

  async verifyResetCode(email: string, code: string): Promise<string> {
    const response = await firstValueFrom(this.api.post<{ valid: boolean; token: string }>('/auth/verify-reset-code', { email, code }));
    return response.token;
  }

  async resetPassword(token: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.api.post('/auth/reset-password', { token, newPassword }));
  }

  logout(): void {
    this.sessionState.set(null);
    this.storage.clear();
  }

  private persist(session: AuthSession): void {
    this.sessionState.set(session);
    this.storage.write(session);
  }
}
