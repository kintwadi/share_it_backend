import { Component, computed, inject } from '@angular/core';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { PlatformConfigService } from '../../services/platform-config.service';

@Component({
  selector: 'app-testimonial-section',
  standalone: true,
  imports: [RevealOnScrollDirective],
  template: `
    @if (section().showSection) {
      <section class="testimonial" id="platform">
        <div class="container">
          <div class="testimonial-inner" appRevealOnScroll>
            <div class="section-tag testimonial-tag">{{ section().eyebrow }}</div>
            <h2>{{ section().title }}</h2>
            <div class="quote-mark">"</div>
            <blockquote>
              {{ section().quote }}
            </blockquote>
            <cite>- {{ section().author }}, <span>{{ section().authorRole }}</span></cite>
          </div>
        </div>
      </section>
    }
  `
})
export class TestimonialSectionComponent {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly section = computed(() => this.platformConfigService.locale().testimonial);
}
