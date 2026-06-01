import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

function shouldSuppressGlobalOverlay(err: any): boolean {
  const msg = String(err?.message || err?.toString?.() || err || '');
  return msg.includes('NG0100') || msg.includes('ExpressionChangedAfterItHasBeenCheckedError');
}

try {
  globalThis.addEventListener(
    'error',
    (event: any) => {
      if (shouldSuppressGlobalOverlay(event?.error || event?.message)) {
        event.preventDefault?.();
        event.stopImmediatePropagation?.();
      }
    },
    true
  );
  globalThis.addEventListener(
    'unhandledrejection',
    (event: any) => {
      if (shouldSuppressGlobalOverlay(event?.reason)) {
        event.preventDefault?.();
        event.stopImmediatePropagation?.();
      }
    },
    true
  );
} catch { }

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
