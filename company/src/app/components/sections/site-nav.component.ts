import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { LanguageCode } from '../../models/landing.models';
import { PlatformConfigService } from '../../services/platform-config.service';

@Component({
  selector: 'app-site-nav',
  standalone: true,
  template: `
    <nav class="nav" [class.scrolled]="scrolled()" aria-label="Main navigation">
      <div class="container nav-inner">
        <a class="logo" [href]="homeHref()" [attr.aria-label]="locale().nav.brand.ariaLabel" (click)="closeMenu()">
          <span class="logo-mark">{{ locale().nav.brand.mark }}</span>
          <strong>{{ locale().nav.brand.name }}</strong>
        </a>

        <button
          type="button"
          class="hamburger"
          [class.active]="menuOpen()"
          [attr.aria-expanded]="menuOpen()"
          aria-controls="primary-nav"
          aria-label="Toggle menu"
          (click)="toggleMenu()">
          <span></span>
          <span></span>
          <span></span>
        </button>

        <div class="nav-links" id="primary-nav" [class.open]="menuOpen()">
          <div class="nav-primary">
            @for (item of navLinks(); track item.id) {
              <a
                [href]="resolveHref(item.href, item.id)"
                [class.active]="activePrimaryId() === item.id"
                (click)="closeMenu()">
                {{ item.label }}
              </a>
            }
          </div>

          <div class="nav-actions">
            <label class="lang-select-wrap" aria-label="Language selector">
              <select class="lang-select" [value]="currentLanguage()" (change)="setLanguage($any($event.target).value)">
                @for (language of supportedLanguages(); track language.code) {
                  <option [value]="language.code">{{ language.label }}</option>
                }
              </select>
            </label>
          </div>
        </div>
      </div>
    </nav>
  `
})
export class SiteNavComponent implements OnInit {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly menuOpen = signal(false);
  readonly scrolled = signal(false);
  readonly currentPath = signal('/');
  readonly locale = this.platformConfigService.locale;
  readonly supportedLanguages = this.platformConfigService.supportedLanguages;
  readonly currentLanguage = this.platformConfigService.currentLanguage;
  readonly navLinks = computed(() => this.locale().nav.links);
  readonly isContactPage = computed(() => this.currentPath() === '/contact');
  readonly isAboutPage = computed(() => this.currentPath() === '/about');
  readonly isHomePage = computed(() => this.currentPath() === '/');
  readonly activePrimaryId = computed(() => {
    if (this.isContactPage()) {
      return 'contact';
    }

    if (this.isAboutPage()) {
      return 'about';
    }

    if (this.isHomePage()) {
      return 'solutions';
    }

    return null;
  });
  readonly homeHref = computed(() => (this.isHomePage() ? '#top' : '/'));

  ngOnInit(): void {
    this.updateHeaderState();
  }

  toggleMenu(): void {
    this.menuOpen.update((value) => !value);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  setLanguage(language: LanguageCode): void {
    this.platformConfigService.setLanguage(language);
    this.closeMenu();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.updateHeaderState();
    if (typeof window !== 'undefined' && window.innerWidth > 768) {
      this.closeMenu();
    }
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.updateHeaderState();
  }

  resolveHref(href: string, id: string): string {
    if (id === 'contact') {
      return '/contact';
    }

    if (!href.startsWith('#')) {
      return href;
    }

    return this.isHomePage() ? href : `/${href}`;
  }

  private updateHeaderState(): void {
    if (typeof window === 'undefined') {
      return;
    }

    this.currentPath.set(window.location.pathname || '/');
    this.scrolled.set(window.scrollY > 20);
  }
}
