import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ShieldAlert, ArrowLeft } from 'lucide-angular';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-subscription-required',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './subscription-required.component.html',
  styleUrl: './subscription-required.component.css'
})
export class SubscriptionRequiredComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  i18n = inject(I18nService);

  readonly ShieldAlert = ShieldAlert;
  readonly ArrowLeft = ArrowLeft;

  get returnTo(): string {
    const qp = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    return qp.startsWith('/') ? qp : '/dashboard';
  }

  back() {
    this.router.navigateByUrl(this.returnTo);
  }

  upgrade() {
    this.router.navigate(['/subscription'], { queryParams: { fromUpgrade: true, from: this.returnTo }, state: { fromUpgrade: true, from: this.returnTo } as any });
  }
}

