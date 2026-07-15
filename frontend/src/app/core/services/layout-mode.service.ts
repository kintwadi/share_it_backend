import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { getRuntimeUiLayout } from '../config/runtime-env';

export type LayoutMode = 'modern' | 'standard';

@Injectable({
  providedIn: 'root'
})
export class LayoutModeService {
  private document = inject(DOCUMENT);
  readonly mode = signal<LayoutMode>(this.readConfiguredMode());
  readonly isStandard = computed(() => this.mode() === 'standard');
  readonly isModern = computed(() => this.mode() === 'modern');

  constructor() {
    effect(() => {
      const mode = this.mode();
      const body = this.document?.body;
      if (!body) return;
      const standard = mode === 'standard';
      body.classList.toggle('layout-standard', standard);
      body.classList.toggle('layout-modern', !standard);
    });
  }

  private readConfiguredMode(): LayoutMode {
    return getRuntimeUiLayout() ?? 'modern';
  }
}
