import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

function normalizeBaseUrl(baseUrl: string): string {
  const trimmed = String(baseUrl || '').trim();
  if (!trimmed) return '';
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
}

function joinUrl(baseUrl: string, path: string): string {
  const base = normalizeBaseUrl(baseUrl);
  const p = String(path || '').trim();
  if (!p) return base;
  if (p.startsWith('http://') || p.startsWith('https://')) return p;
  if (p.startsWith('/')) return `${base}${p}`;
  return `${base}/${p}`;
}

@Injectable({
  providedIn: 'root'
})
export class ApiClientService {
  private http = inject(HttpClient);

  private baseUrl = (() => {
    const runtime = (globalThis as any)?.__env?.API_URL;
    const url = typeof runtime === 'string' && runtime.trim() ? runtime.trim() : environment.apiUrl;
    return normalizeBaseUrl(url);
  })();

  getBaseUrl(): string {
    return this.baseUrl;
  }

  get<T>(path: string, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.get<T>(url, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  getText(path: string, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<string> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.get(url, {
      responseType: 'text',
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  post<T>(path: string, body: any, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.post<T>(url, body, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  put<T>(path: string, body: any, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.put<T>(url, body, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  patch<T>(path: string, body: any, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.patch<T>(url, body, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  delete<T>(path: string, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.delete<T>(url, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }

  postFormData<T>(path: string, form: FormData, options?: { params?: Record<string, any>; headers?: Record<string, string> }): Observable<T> {
    const url = joinUrl(this.baseUrl, path);
    return this.http.post<T>(url, form, {
      params: options?.params ? new HttpParams({ fromObject: options.params as any }) : undefined,
      headers: options?.headers ? new HttpHeaders(options.headers) : undefined
    });
  }
}
