import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStorageService } from '../services/auth-storage.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(AuthStorageService);
  const session = storage.read();
  if (!session?.token) {
    return next(req);
  }
  return next(req.clone({
    setHeaders: {
      Authorization: 'Bearer ' + session.token
    }
  }));
};
