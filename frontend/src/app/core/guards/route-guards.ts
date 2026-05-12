import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthStorageService } from '../services/auth-storage.service';
import { SessionService } from '../services/session.service';
import { SettingsConfigService } from '../services/settings-config.service';

async function ensureAuthenticated(authStorage: AuthStorageService, session: SessionService): Promise<boolean> {
  if (!authStorage.getToken()) return false;
  await session.refresh();
  return !!session.user();
}

function isAdminRole(role: unknown): boolean {
  const r = String(role ?? '').toUpperCase();
  return r === 'ADMIN' || r === 'ROLE_ADMIN';
}

export const canMatchDashboard: CanMatchFn = async () => {
  const router = inject(Router);
  const settingsConfig = inject(SettingsConfigService);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  await settingsConfig.ensureLoaded();
  const ok = await ensureAuthenticated(authStorage, session);
  if (!ok) return router.createUrlTree(['/connect']);
  if (isAdminRole(session.user()?.role)) return true;
  if (!settingsConfig.isSectionEnabled('header', 'dashboard')) {
    return router.createUrlTree(['/']);
  }
  return true;
};

export const canMatchSettings: CanMatchFn = async () => {
  const router = inject(Router);
  const settingsConfig = inject(SettingsConfigService);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  await settingsConfig.ensureLoaded();
  if (!settingsConfig.isSectionEnabled('header', 'settings')) {
    return router.createUrlTree(['/']);
  }
  const ok = await ensureAuthenticated(authStorage, session);
  return ok ? true : router.createUrlTree(['/connect']);
};

export const canMatchMessages: CanMatchFn = async () => {
  const router = inject(Router);
  const settingsConfig = inject(SettingsConfigService);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  await settingsConfig.ensureLoaded();
  if (!settingsConfig.isSectionEnabled('header', 'messages')) {
    return router.createUrlTree(['/']);
  }
  const ok = await ensureAuthenticated(authStorage, session);
  return ok ? true : router.createUrlTree(['/connect']);
};

export const canMatchMailbox: CanMatchFn = async () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  const ok = await ensureAuthenticated(authStorage, session);
  return ok ? true : router.createUrlTree(['/connect']);
};

export const canMatchAdmin: CanMatchFn = async () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  const ok = await ensureAuthenticated(authStorage, session);
  if (!ok) return router.createUrlTree(['/connect']);
  const u = session.user();
  return isAdminRole(u?.role) ? true : router.createUrlTree(['/']);
};

export const canMatchNewItem: CanMatchFn = async () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  const ok = await ensureAuthenticated(authStorage, session);
  return ok ? true : router.createUrlTree(['/connect']);
};

export const canMatchSubscription: CanMatchFn = async () => {
  const router = inject(Router);
  const settingsConfig = inject(SettingsConfigService);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  await settingsConfig.ensureLoaded();
  if (!settingsConfig.isSectionEnabled('header', 'subscribe')) {
    return router.createUrlTree(['/']);
  }
  const ok = await ensureAuthenticated(authStorage, session);
  if (!ok) return router.createUrlTree(['/connect']);
  const u = session.user();
  if (!u || u.role === 'ADMIN') return router.createUrlTree(['/']);
  const sub = session.subscription();
  const status = String(sub?.status || '').toLowerCase();
  const hasSub = status === 'active' || status === 'trialing';
  const nav = router.getCurrentNavigation();
  const state = (nav?.extras?.state || {}) as any;
  const persistedState = (history.state || {}) as any;
  const url = (nav?.extractedUrl?.toString() || nav?.finalUrl?.toString() || '') as string;
  const allowFromUpgrade =
    state?.fromUpgrade === true ||
    persistedState?.fromUpgrade === true ||
    state?.requiredPlan != null ||
    persistedState?.requiredPlan != null ||
    /[?&]fromUpgrade=(1|true)\b/i.test(url);
  return hasSub && !allowFromUpgrade ? router.createUrlTree(['/dashboard']) : true;
};

export const canMatchBorrowerSubscription: CanMatchFn = async () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);
  const session = inject(SessionService);
  const ok = await ensureAuthenticated(authStorage, session);
  return ok ? true : router.createUrlTree(['/connect']);
};
