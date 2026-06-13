import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { getRuntimeEnv } from '../config/runtime-env';

@Injectable({ providedIn: 'root' })
export class ApiClientService {
  private readonly http = inject(HttpClient);

  get<T>(resourcePath: string, options: Record<string, unknown> = {}) {
    return this.http.get<T>(this.buildUrl(resourcePath), this.withTenantHeader(options));
  }

  post<T>(resourcePath: string, body: unknown, options: Record<string, unknown> = {}) {
    return this.http.post<T>(this.buildUrl(resourcePath), body, this.withTenantHeader(options));
  }

  put<T>(resourcePath: string, body: unknown, options: Record<string, unknown> = {}) {
    return this.http.put<T>(this.buildUrl(resourcePath), body, this.withTenantHeader(options));
  }

  delete<T>(resourcePath: string, options: Record<string, unknown> = {}) {
    return this.http.delete<T>(this.buildUrl(resourcePath), this.withTenantHeader(options));
  }

  private buildUrl(resourcePath: string): string {
    const base = getRuntimeEnv().apiUrl;
    const normalized = resourcePath.startsWith('/') ? resourcePath : '/' + resourcePath;
    return base + normalized;
  }

  private withTenantHeader(options: Record<string, unknown>): Record<string, unknown> {
    const env = getRuntimeEnv();
    let headers = options['headers'] instanceof HttpHeaders
      ? options['headers'] as HttpHeaders
      : new HttpHeaders(options['headers'] as Record<string, string> | undefined);

    if (env.tenantId && env.tenantHeaderName && !headers.has(env.tenantHeaderName)) {
      headers = headers.set(env.tenantHeaderName, env.tenantId);
    }

    return { ...options, headers };
  }
}
