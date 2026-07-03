import { Component, input } from '@angular/core';
import { VALUE_PROPS } from '../../data/landing-content';
import { ValueProp } from '../../models/landing.models';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { ValueCardComponent } from '../shared/value-card.component';

@Component({
  selector: 'app-value-props-section',
  standalone: true,
  imports: [ValueCardComponent, RevealOnScrollDirective],
  template: `
    <section class="values" id="about">
      <div class="container">
        <header class="section-head" appRevealOnScroll>
          <span class="section-tag">Why Vicinity24</span>
          <h2>Built for businesses that need to move fast</h2>
          <p>We combine enterprise-grade capability with the simplicity and pricing SMBs actually need.</p>
        </header>

        <div class="values-grid">
          @for (value of values(); track value.title; let index = $index) {
            <app-value-card [value]="value" [delayClass]="index === 1 ? 'delay-1' : index === 2 ? 'delay-2' : ''" />
          }
        </div>
      </div>
    </section>
  `
})
export class ValuePropsSectionComponent {
  readonly values = input<ValueProp[]>(VALUE_PROPS);
}
