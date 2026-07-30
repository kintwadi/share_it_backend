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
  const requestUrl = String(req.url || '');
  const isPublicReviewInviteRequest = requestUrl.includes('/reviews/invite/');

  let modifiedReq = req;
  if (tenantId) {
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        [getTenantHeaderName()]: tenantId
      }
    });
  }
  if (token && !isPublicReviewInviteRequest) {
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        const currentUrl = String(router.url || '');
        const isBorrowerFlowUrl =
          currentUrl.startsWith('/verification/email') ||
          currentUrl.startsWith('/listing/') ||
          currentUrl.startsWith('/rate');
        const isNonCriticalProbe =
          isPublicReviewInviteRequest ||
          requestUrl.includes('/borrower-subscription/') ||
          requestUrl.includes('/subscriptions/') ||
          requestUrl.includes('/payments/methods') ||
          requestUrl.includes('/notifications') ||
          requestUrl.includes('/users/me');
        const shouldSuppressForcedLogout = isBorrowerFlowUrl || isNonCriticalProbe;
        console.warn('[auth-interceptor] auth error', {
          status: error.status,
          requestUrl,
          currentUrl,
          isBorrowerFlowUrl,
          isNonCriticalProbe,
          shouldSuppressForcedLogout
        });
        if (shouldSuppressForcedLogout) {
          return throwError(() => error);
        }
        authStorage.clear();
        if (!currentUrl.startsWith('/connect')) {
          router.navigate(['/connect']);
        }
      }
      return throwError(() => error);
    })
  );
};
