import { Component, computed, inject } from '@angular/core';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { PlatformConfigService } from '../../services/platform-config.service';
import { ProductCardComponent } from '../shared/product-card.component';

@Component({
  selector: 'app-solutions-section',
  standalone: true,
  imports: [ProductCardComponent, RevealOnScrollDirective],
  template: `
    @if (section().showSection) {
      <section class="products" id="solutions">
        <div class="container">
          <header class="section-head" appRevealOnScroll>
            <span class="section-tag">{{ section().eyebrow }}</span>
            <h2>{{ section().title }}</h2>
            <p>{{ section().subtitle }}</p>
          </header>

          <div class="products-grid">
            @for (product of visibleProducts(); track product.id; let index = $index) {
              <app-product-card [product]="product" [delayClass]="index === 1 || index === 3 ? 'delay-1' : ''" />
            }
          </div>
        </div>
      </section>
    }
  `
})
export class SolutionsSectionComponent {
  private readonly platformConfigService = inject(PlatformConfigService);

  readonly locale = this.platformConfigService.locale;
  readonly section = computed(() => this.locale().platformSection);
  readonly visibleProducts = computed(() =>
    this.section().platforms.filter((product) => product.visible !== false)
  );
}
