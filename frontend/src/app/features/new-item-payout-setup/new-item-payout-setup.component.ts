import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, CreditCard, ArrowLeft } from 'lucide-angular';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-new-item-payout-setup',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './new-item-payout-setup.component.html',
  styleUrl: './new-item-payout-setup.component.css'
})
export class NewItemPayoutSetupComponent {
  private router = inject(Router);
  i18n = inject(I18nService);

  readonly CreditCard = CreditCard;
  readonly ArrowLeft = ArrowLeft;

  back() {
    this.router.navigate(['/new-item']);
  }

  managePayouts() {
    this.router.navigate(['/settings'], { queryParams: { tab: 'payments' } });
  }
}

