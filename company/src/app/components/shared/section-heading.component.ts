import { Component, input } from '@angular/core';

@Component({
  selector: 'app-section-heading',
  standalone: true,
  template: `
    <header class="section-heading">
      @if (eyebrow()) {
        <p class="section-kicker">{{ eyebrow() }}</p>
      }
      <h2>{{ title() }}</h2>
      @if (subtitle()) {
        <p class="section-subtitle">{{ subtitle() }}</p>
      }
    </header>
  `
})
export class SectionHeadingComponent {
  readonly eyebrow = input('');
  readonly title = input.required<string>();
  readonly subtitle = input('');
}
