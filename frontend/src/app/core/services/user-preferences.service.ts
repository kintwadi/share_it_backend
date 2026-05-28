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

const DEFAULT_PREFS: UserPreferences = {
  emailNotifications: true,
  pushNotifications: true,
  marketingConsent: false,
  locationData: true,
  researchOptIn: false,
  notificationFrequency: 'immediate',
  dndFrom: '21:00',
  dndTo: '07:00'
};

@Injectable({
  providedIn: 'root'
})
export class UserPreferencesService {
  private key = 'user_prefs';

  private prefsSignal = signal<UserPreferences>(this.read());
  prefs = this.prefsSignal.asReadonly();

  update(patch: Partial<UserPreferences>) {
    const next: UserPreferences = { ...this.prefsSignal(), ...patch };
    this.prefsSignal.set(next);
    try {
      localStorage.setItem(this.key, JSON.stringify(next));
    } catch { }
  }

  private read(): UserPreferences {
    try {
      const raw = localStorage.getItem(this.key);
      if (!raw) return { ...DEFAULT_PREFS };
      const parsed = JSON.parse(raw);
      return { ...DEFAULT_PREFS, ...(parsed || {}) };
    } catch {
      return { ...DEFAULT_PREFS };
    }
  }
}
