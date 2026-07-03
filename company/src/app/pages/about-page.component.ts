import { Component, computed, inject } from '@angular/core';
import { SiteFooterComponent } from '../components/sections/site-footer.component';
import { SiteNavComponent } from '../components/sections/site-nav.component';
import { PlatformConfigService } from '../services/platform-config.service';

@Component({
  selector: 'app-about-page',
  standalone: true,
  imports: [SiteNavComponent, SiteFooterComponent],
  template: `
    @if (siteConfig().sectionVisibility.nav) {
      <app-site-nav />
    }

    <main class="about-page">
      <section class="about-section">
        <div class="container">
          <div class="about-copy">
            <span class="hero-badge">{{ locale().hero.badge }}</span>
            <h1>{{ locale().hero.title }} <span class="accent">{{ locale().hero.accent }}</span></h1>
            <p>{{ locale().hero.description }}</p>
          </div>
        </div>
      </section>
    </main>

    @if (siteConfig().sectionVisibility.footer) {
      <app-site-footer />
    }
  `
})
export class AboutPageComponent {
  private readonly platformConfigService = inject(PlatformConfigService);

  readonly siteConfig = this.platformConfigService.siteConfig;
  readonly locale = computed(() => this.platformConfigService.locale());
}
