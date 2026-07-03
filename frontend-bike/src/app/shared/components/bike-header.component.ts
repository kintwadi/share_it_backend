import { CommonModule } from '@angular/common';
import { Component, HostListener, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TPipe } from '../../core/i18n/t.pipe';

@Component({
  selector: 'app-bike-header',
  standalone: true,
  imports: [CommonModule, RouterLink, TPipe],
  templateUrl: './bike-header.component.html',
  styleUrl: './bike-header.component.css'
})
export class BikeHeaderComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  menuOpen = false;

  get user() {
    return this.auth.user();
  }

  get activeLanguage() {
    return this.i18n.language();
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }

  setLanguage(language: 'en' | 'de'): void {
    this.i18n.setLanguage(language);
    this.closeMenu();
  }

  @HostListener('window:resize')
  onResize(): void {
    if (typeof window !== 'undefined' && window.innerWidth > 980) {
      this.menuOpen = false;
    }
  }
}
