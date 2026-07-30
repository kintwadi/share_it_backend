import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Layout } from './core/layout/layout/layout';
import { StandardLayout } from './core/layout/standard-layout/standard-layout';
import { LayoutModeService } from './core/services/layout-mode.service';
import { SettingsConfigService } from './core/services/settings-config.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, Layout, StandardLayout],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private settingsConfig = inject(SettingsConfigService);
  layoutMode = inject(LayoutModeService);
  protected readonly title = signal('share-it-client');

  constructor() {
    this.settingsConfig.ensureLoaded();
  }
}
