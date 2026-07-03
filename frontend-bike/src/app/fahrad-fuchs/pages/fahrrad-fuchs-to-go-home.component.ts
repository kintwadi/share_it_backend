import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { AuthService } from '../../core/services/auth.service';
import { FahrradFuchsToGoBikeCardComponent } from '../components/fahrrad-fuchs-to-go-bike-card.component';
import { FahrradFuchsToGoApiService } from '../fahrrad-fuchs-to-go-api.service';
import { FahrradFuchsToGoCatalogItem, FahrradFuchsToGoStorefrontResponse } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-home',
  standalone: true,
  imports: [CommonModule, RouterLink, FahrradFuchsToGoBikeCardComponent, TPipe],
  templateUrl: './fahrrad-fuchs-to-go-home.component.html',
  styleUrl: './fahrrad-fuchs-to-go-home.component.css'
})
export class FahrradFuchsToGoHomeComponent implements OnInit {
  readonly trialDepositAmount = 100;
  readonly trialWindowDays = 2;

  private readonly api = inject(FahrradFuchsToGoApiService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  storefront: FahrradFuchsToGoStorefrontResponse | null = null;
  loading = true;
  errorKey = '';
  activeHeroBikeIndex = 0;

  get heroBikes(): FahrradFuchsToGoCatalogItem[] {
    return this.storefront?.bikes || [];
  }

  get activeHeroBike(): FahrradFuchsToGoCatalogItem | null {
    if (!this.heroBikes.length) {
      return null;
    }
    return this.heroBikes[this.activeHeroBikeIndex] || this.heroBikes[0];
  }

  get requiresLoginForReservation(): boolean {
    return !this.auth.isAuthenticated();
  }

  async ngOnInit(): Promise<void> {
    try {
      this.storefront = await this.api.getStorefront();
      this.activeHeroBikeIndex = 0;
    } catch {
      this.errorKey = 'errors.storefrontLoad';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  previousHeroBike(): void {
    if (!this.heroBikes.length) {
      return;
    }
    this.activeHeroBikeIndex = (this.activeHeroBikeIndex - 1 + this.heroBikes.length) % this.heroBikes.length;
  }

  nextHeroBike(): void {
    if (!this.heroBikes.length) {
      return;
    }
    this.activeHeroBikeIndex = (this.activeHeroBikeIndex + 1) % this.heroBikes.length;
  }

  heroReservationLink(slug: string): string[] {
    return this.auth.isAuthenticated() ? ['/fahrad-fuchs/checkout', slug] : ['/login'];
  }

  heroReservationQueryParams(slug: string): Record<string, string> | undefined {
    if (this.auth.isAuthenticated()) {
      return undefined;
    }
    return { redirect: `/fahrad-fuchs/checkout/${slug}` };
  }

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }
}
