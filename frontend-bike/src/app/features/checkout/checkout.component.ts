import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SubscriptionService } from '../../core/services/subscription.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly subscriptions = inject(SubscriptionService);

  plan = 'plus';
  loading = false;
  error = '';

  get user() {
    return this.auth.user();
  }

  ngOnInit(): void {
    this.plan = this.route.snapshot.queryParamMap.get('plan') || 'plus';
  }

  async startCheckout(): Promise<void> {
    this.loading = true;
    this.error = '';
    try {
      const response = await this.subscriptions.createCheckoutSession(this.plan, '/bikes');
      if (response.url) {
        window.location.href = response.url;
        return;
      }
      this.error = response.error || 'Checkout session did not return a redirect URL.';
    } catch {
      this.error = 'Unable to create the subscription checkout session.';
    } finally {
      this.loading = false;
    }
  }
}
