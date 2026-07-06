import { Component, input } from '@angular/core';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { ValueProp } from '../../models/landing.models';

@Component({
  selector: 'app-value-card',
  standalone: true,
  template: `
    <article class="value-card" appRevealOnScroll [class.delay-1]="delayClass() === 'delay-1'" [class.delay-2]="delayClass() === 'delay-2'">
      <div class="value-icon" aria-hidden="true">
        @switch (value().icon) {
          @case ('analytics') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 3v18h18" />
              <path d="M18 17V9" />
              <path d="M13 17V5" />
              <path d="M8 17v-3" />
            </svg>
          }
          @case ('roi') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v20" />
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
            </svg>
          }
          @default {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
            </svg>
          }
        }
      </div>
      <h3>{{ value().title }}</h3>
      <p>{{ value().description }}</p>
    </article>
  `,
  imports: [RevealOnScrollDirective]
})
export class ValueCardComponent {
  readonly value = input.required<ValueProp>();
  readonly delayClass = input('');
}
