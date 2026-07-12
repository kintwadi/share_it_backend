import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';

export type LayoutMode = 'modern' | 'standard';

@Injectable({
  providedIn: 'root'
})
export class LayoutModeService {
  private static readonly STORAGE_KEY = 'shareit.layout.mode';
  private document = inject(DOCUMENT);
  readonly mode = signal<LayoutMode>(this.readStoredMode());
  readonly isStandard = computed(() => this.mode() === 'standard');
  readonly isModern = computed(() => this.mode() === 'modern');

  constructor() {
    effect(() => {
      const mode = this.mode();
      this.persistMode(mode);

      const body = this.document?.body;
      if (!body) return;
      const standard = mode === 'standard';
      body.classList.toggle('layout-standard', standard);
      body.classList.toggle('layout-modern', !standard);
    });
  }

  setMode(mode: LayoutMode) {
    this.mode.set(mode);
  }

  toggleMode() {
    this.mode.update(mode => (mode === 'standard' ? 'modern' : 'standard'));
  }

  private readStoredMode(): LayoutMode {
    try {
      const raw = window.localStorage.getItem(LayoutModeService.STORAGE_KEY);
      return raw === 'standard' ? 'standard' : 'modern';
    } catch {
      return 'modern';
    }
  }

  private persistMode(mode: LayoutMode) {
    try {
      window.localStorage.setItem(LayoutModeService.STORAGE_KEY, mode);
    } catch {
      // Ignore storage failures and keep the in-memory selection.
    }
  }
}
