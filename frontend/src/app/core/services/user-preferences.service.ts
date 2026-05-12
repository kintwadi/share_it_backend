import { Injectable, signal } from '@angular/core';

export type NotificationFrequency = 'immediate' | 'daily' | 'weekly';

export interface UserPreferences {
  emailNotifications: boolean;
  pushNotifications: boolean;
  marketingConsent: boolean;
  locationData: boolean;
  researchOptIn: boolean;
  notificationFrequency: NotificationFrequency;
  dndFrom: string;
  dndTo: string;
}

const PREFS_KEY = 'nearshare_settings_prefs';

@Injectable({
  providedIn: 'root'
})
export class UserPreferencesService {
  prefs = signal<UserPreferences>({
    emailNotifications: true,
    pushNotifications: true,
    marketingConsent: false,
    locationData: true,
    researchOptIn: false,
    notificationFrequency: 'immediate',
    dndFrom: '21:00',
    dndTo: '07:00'
  });

  constructor() {
    this.load();
  }

  load() {
    try {
      const raw = localStorage.getItem(PREFS_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      const next: UserPreferences = {
        ...this.prefs(),
        ...parsed
      };
      if (next.notificationFrequency !== 'daily' && next.notificationFrequency !== 'weekly' && next.notificationFrequency !== 'immediate') {
        next.notificationFrequency = 'immediate';
      }
      if (typeof next.dndFrom !== 'string' || !next.dndFrom) next.dndFrom = '21:00';
      if (typeof next.dndTo !== 'string' || !next.dndTo) next.dndTo = '07:00';
      this.prefs.set(next);
    } catch { }
  }

  update(partial: Partial<UserPreferences>) {
    const next = { ...this.prefs(), ...partial };
    if (next.notificationFrequency !== 'daily' && next.notificationFrequency !== 'weekly' && next.notificationFrequency !== 'immediate') {
      next.notificationFrequency = 'immediate';
    }
    this.prefs.set(next);
    try {
      localStorage.setItem(PREFS_KEY, JSON.stringify(next));
    } catch { }
  }
}

