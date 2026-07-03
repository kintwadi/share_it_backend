import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export interface MetricStripItem {
  value: string;
  label: string;
}

@Component({
  selector: 'app-metric-strip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="metric-strip" [class.compact]="compact">
      <article *ngFor="let item of items">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </article>
    </div>
  `,
  styles: [`
    .metric-strip {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 0.8rem;
    }

    article {
      padding: 1rem;
      border-radius: 1.1rem;
      background: #fff;
      border: 1px solid var(--border);
      box-shadow: none;
    }

    .compact article {
      padding: 0.95rem;
    }

    strong,
    span {
      display: block;
    }

    strong {
      font-size: 1.15rem;
      color: var(--text);
      font-weight: 700;
    }

    span {
      margin-top: 0.22rem;
      color: var(--muted);
      line-height: 1.5;
    }
  `]
})
export class MetricStripComponent {
  @Input() items: MetricStripItem[] = [];
  @Input() compact = false;
}
