import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStorageService } from '../services/auth-storage.service';
import { Router } from '@angular/router';
import { getTenantHeaderName, getTenantId } from '../config/runtime-env';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authStorage = inject(AuthStorageService);
  const router = inject(Router);
  const token = authStorage.getToken();
  const tenantId = getTenantId();

  let modifiedReq = req;
  if (tenantId) {
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        [getTenantHeaderName()]: tenantId
      }
    });
  }
  if (token) {
    modifiedReq = modifiedReq.clone({
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
