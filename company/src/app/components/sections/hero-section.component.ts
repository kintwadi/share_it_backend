import { Component, inject } from '@angular/core';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { PlatformConfigService } from '../../services/platform-config.service';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [RevealOnScrollDirective],
  template: `
    @if (locale().hero.showSection) {
      <section class="hero" id="top">
        <div class="container hero-grid">
          <div class="hero-copy" appRevealOnScroll>
            <span class="hero-badge">{{ locale().hero.badge }}</span>
            <h1>{{ locale().hero.title }} <span class="accent">{{ locale().hero.accent }}</span></h1>
            <p class="hero-lead">
              {{ locale().hero.description }}
            </p>
          </div>
        </div>
      </section>
    }
  `
})
export class HeroSectionComponent {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly locale = this.platformConfigService.locale;
}
