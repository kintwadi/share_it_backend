import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStorageService } from '../services/auth-storage.service';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authStorage = inject(AuthStorageService);
  const router = inject(Router);
  const token = authStorage.getToken();

  let modifiedReq = req;
  if (token) {
    modifiedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        authStorage.clear();
        const currentUrl = String(router.url || '');
        if (!currentUrl.startsWith('/connect')) {
          router.navigate(['/connect']);
        }
      }
      return throwError(() => error);
    })
  );
};
