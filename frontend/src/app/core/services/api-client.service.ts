import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiClientService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getBaseUrl(): string {
    return this.apiUrl;
  }

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.apiUrl}${path}`).pipe(
      catchError(this.handleError)
    );
  }

  post<T>(path: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.apiUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  postFormData<T>(path: string, formData: FormData): Observable<T> {
    return this.http.post<T>(`${this.apiUrl}${path}`, formData).pipe(
      catchError(this.handleError)
    );
  }

  put<T>(path: string, body: any): Observable<T> {
    return this.http.put<T>(`${this.apiUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  patch<T>(path: string, body: any): Observable<T> {
    return this.http.patch<T>(`${this.apiUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.apiUrl}${path}`).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'request_failed';
    if (error.status === 401) {
      errorMessage = 'unauthorized';
    } else if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else if (error.error && error.error.error) {
      errorMessage = error.error.error;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    return throwError(() => new Error(errorMessage));
  }
}
