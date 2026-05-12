import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, Shield, ArrowLeft } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { User, UserRole } from '../../core/models/types';

@Component({
  selector: 'app-borrower-subscription',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './borrower-subscription.component.html',
  styleUrl: './borrower-subscription.component.css'
})
export class BorrowerSubscriptionComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly Shield = Shield;
  readonly ArrowLeft = ArrowLeft;

  user: User | null = null;
  loading = true;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    try {
      const u = await this.api.getCurrentUser();
      this.user = u;
      if (!u || u.role !== UserRole.BORROWER) {
        this.router.navigate(['/dashboard']);
        return;
      }
    } finally {
      this.loading = false;
      this.render();
    }
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
