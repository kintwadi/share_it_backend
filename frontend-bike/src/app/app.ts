import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { BikeFooterComponent } from './shared/components/bike-footer.component';
import { BikeHeaderComponent } from './shared/components/bike-header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, BikeHeaderComponent, BikeFooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);

  get isAuthRoute(): boolean {
    const url = this.router.url.split('?')[0];
    return url === '/login' || url === '/signup' || url === '/password-recovery';
  }

  get showSharedHeader(): boolean {
    return !this.isAuthRoute;
  }

  get showSharedFooter(): boolean {
    return this.isAuthRoute;
  }
}
