import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductSolution } from '../../models/landing.models';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [RevealOnScrollDirective, RouterLink],
  template: `
    <article class="product-card" appRevealOnScroll [class.delay-1]="delayClass() === 'delay-1'">
      <div class="product-image">
        <img [src]="product().image" [alt]="product().alt" loading="lazy" />
      </div>
      <div class="product-body">
        <span class="product-tag">{{ product().label }}</span>
        <h3>{{ displayTitle() }}</h3>
        <p class="product-tagline">{{ product().tagline }}</p>
        <p class="product-description">{{ displayDescription() }}</p>
        <div class="product-features" aria-label="Product features">
          @for (feature of displayFeatures(); track feature) {
            <span class="feature-chip">{{ feature }}</span>
          }
        </div>
        @if (isInternalRoute()) {
          <a class="product-link" routerLink="/contact" [queryParams]="ctaQueryParams()">
            {{ product().ctaLabel || 'Explore' }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </a>
        } @else {
          <a class="product-link" [href]="ctaHref()">
            {{ product().ctaLabel || 'Explore' }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </a>
        }
      </div>
    </article>
  `
})
export class ProductCardComponent {
  readonly product = input.required<ProductSolution>();
  readonly delayClass = input('');
  readonly ctaHref = computed(() => this.product().link || `/contact?solution=${this.product().id}`);
  readonly ctaQueryParams = computed(() => ({ solution: this.product().id }));
  readonly isInternalRoute = computed(() => !this.product().link);
  readonly displayTitle = computed(() => {
    const product = this.product();
    const prefixedLabel = `${product.label}: `;

    if (product.title.startsWith(prefixedLabel)) {
      return product.title.slice(prefixedLabel.length);
    }

    return product.title === product.label && product.tagline ? product.tagline : product.title;
  });
  readonly displayDescription = computed(() => {
    const product = this.product();

    switch (product.id) {
      case 'sas':
        return 'A peer-to-peer platform where businesses and individuals can lend and borrow items, equipment, and resources within their area.';
      case 'linkedStore':
        return 'A unified inventory network for multi-location retailers and partner businesses. Pool inventory across stores and partners.';
      default:
        return product.description;
    }
  });
  readonly displayFeatures = computed(() =>
    this.product().features.map((feature) => {
      switch (feature) {
        case 'Verified user profiles':
          return 'Verified profiles';
        case 'Secure item tracking':
          return 'Secure tracking';
        case 'Rating & review system':
          return 'Rating system';
        case 'Shared fulfillment routing':
          return 'Shared fulfillment';
        default:
          return feature;
      }
    })
  );
}
