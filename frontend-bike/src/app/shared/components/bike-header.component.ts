import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-bike-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './bike-header.component.html',
  styleUrl: './bike-header.component.css'
})
export class BikeHeaderComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  get user() {
    return this.auth.user();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}
