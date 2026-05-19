import { ErrorHandler, Injectable } from '@angular/core';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: any): void {
    const msg = String(error?.message || error?.toString?.() || '');
    if (msg.includes('NG0100') || msg.includes('ExpressionChangedAfterItHasBeenCheckedError')) {
      return;
    }
    console.error(error);
  }
}

