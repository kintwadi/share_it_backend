import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-section-intro',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="section-intro">
      <div>
        <p class="eyebrow" *ngIf="eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p *ngIf="description">{{ description }}</p>
      </div>
      <a *ngIf="linkLabel && linkHref" [routerLink]="linkHref">{{ linkLabel }}</a>
    </div>
  `,
  styles: [`
    .section-intro {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: end;
      margin-bottom: 1.1rem;
    }

    .eyebrow {
      margin: 0 0 0.45rem;
      color: var(--primary);
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-size: 0.76rem;
    }

    h2,
    p {
      margin: 0;
    }

    h2 {
      letter-spacing: -0.02em;
    }

    p:last-child {
      margin-top: 0.4rem;
      color: var(--muted);
      line-height: 1.6;
    }

    a {
      text-decoration: none;
      color: var(--text);
      font-weight: 600;
      white-space: nowrap;
    }

    @media (max-width: 700px) {
      .section-intro {
        flex-direction: column;
        align-items: start;
      }
    }
  `]
})
export class SectionIntroComponent {
  @Input() eyebrow = '';
  @Input({ required: true }) title = '';
  @Input() description = '';
  @Input() linkLabel = '';
  @Input() linkHref = '';
}
