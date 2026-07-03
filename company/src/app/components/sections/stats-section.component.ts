import { Component, input } from '@angular/core';
import { STATS } from '../../data/landing-content';
import { RevealOnScrollDirective } from '../../directives/reveal-on-scroll.directive';
import { StatItem } from '../../models/landing.models';

@Component({
  selector: 'app-stats-section',
  standalone: true,
  imports: [RevealOnScrollDirective],
  template: `
    <section class="stats">
      <div class="container">
        <div class="stats-grid">
          @for (stat of stats(); track stat.label; let index = $index) {
            <div appRevealOnScroll [class.delay-1]="index === 1" [class.delay-2]="index === 2" [class.delay-3]="index === 3">
              <div class="stat-num">
                {{ stat.value }}
                @if (stat.accent) {
                  <span class="accent">{{ stat.accent }}</span>
                }
              </div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          }
        </div>
      </div>
    </section>
  `
})
export class StatsSectionComponent {
  readonly stats = input<StatItem[]>(STATS);
}
