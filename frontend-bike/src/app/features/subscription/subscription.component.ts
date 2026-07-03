import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SubscriptionConfig } from '../../core/models/commerce.models';
import { SubscriptionService } from '../../core/services/subscription.service';
import { MetricStripComponent } from '../../shared/components/metric-strip.component';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, RouterLink, MetricStripComponent],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.css'
})
export class SubscriptionComponent implements OnInit {
  private readonly subscriptions = inject(SubscriptionService);

  config: SubscriptionConfig = {
    enabled: true,
    starter: true,
    plus: true,
    pro: true
  };
  loading = true;
  readonly planMetrics = [
    { value: 'Starter', label: 'Explore the storefront' },
    { value: 'Plus', label: 'Run day-to-day bike operations' },
    { value: 'Pro', label: 'Scale partner and commerce workflows' }
  ];

  async ngOnInit(): Promise<void> {
    try {
      this.config = await this.subscriptions.getConfig();
    } catch {
      this.config = {
        enabled: true,
        starter: true,
        plus: true,
        pro: true
      };
    } finally {
      this.loading = false;
    }
  }
}