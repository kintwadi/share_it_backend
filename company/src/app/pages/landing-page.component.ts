import { Component, computed, inject } from '@angular/core';
import { FinalCtaSectionComponent } from '../components/sections/final-cta-section.component';
import { SiteFooterComponent } from '../components/sections/site-footer.component';
import { SiteNavComponent } from '../components/sections/site-nav.component';
import { SolutionsSectionComponent } from '../components/sections/solutions-section.component';
import { TestimonialSectionComponent } from '../components/sections/testimonial-section.component';
import { PlatformConfigService } from '../services/platform-config.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    SiteNavComponent,
    SolutionsSectionComponent,
    TestimonialSectionComponent,
    FinalCtaSectionComponent,
    SiteFooterComponent
  ],
  template: `
    @if (siteConfig().sectionVisibility.nav) {
      <app-site-nav />
    }

    <main class="landing-shell">
      <app-solutions-section />
      <app-testimonial-section />
      <app-final-cta-section />
    </main>

    @if (siteConfig().sectionVisibility.footer) {
      <app-site-footer />
    }
  `
})
export class LandingPageComponent {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly siteConfig = this.platformConfigService.siteConfig;
}
