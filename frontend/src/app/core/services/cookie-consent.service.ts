import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CookieConsentService {
  private readonly storageKey = 'cookie_consent_v1';
  readonly decision = signal<'accepted' | 'denied' | null>(null);

  constructor() {
    let decision: 'accepted' | 'denied' | null = null;
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (raw === 'accepted' || raw === 'true') decision = 'accepted';
      else if (raw === 'denied' || raw === 'false') decision = 'denied';
    } catch { }
    this.decision.set(decision);
  }

  accept() {
    try {
      localStorage.setItem(this.storageKey, 'accepted');
    } catch { }
    this.decision.set('accepted');
  }

  deny() {
    try {
      localStorage.setItem(this.storageKey, 'denied');
    } catch { }
    this.decision.set('denied');
  }
}
