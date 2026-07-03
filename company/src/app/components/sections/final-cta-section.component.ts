import { Component, computed, inject } from '@angular/core';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { PlatformConfigService } from '../../services/platform-config.service';

@Component({
  selector: 'app-final-cta-section',
  standalone: true,
  imports: [RevealOnScrollDirective],
  template: `
    @if (section().showSection) {
      <section class="final-cta" id="demo">
        <div class="container">
          <div class="final-cta-inner" appRevealOnScroll>
            <h2>{{ section().title }}</h2>
            <p>
              {{ section().description }}
            </p>
            <div class="btn-row">
              <a class="btn btn-white" [href]="section().primaryCta.href">{{ section().primaryCta.label }}</a>
              <a class="btn btn-ghost" [href]="section().secondaryCta.href">{{ section().secondaryCta.label }}</a>
            </div>
          </div>
        </div>
      </section>
    }
  `
})
export class FinalCtaSectionComponent {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly section = computed(() => this.platformConfigService.locale().finalCta);
}
