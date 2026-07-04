import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteFooterComponent } from '../components/sections/site-footer.component';
import { SiteNavComponent } from '../components/sections/site-nav.component';
import { PlatformConfigService } from '../services/platform-config.service';

@Component({
  selector: 'app-about-page',
  standalone: true,
  imports: [RouterLink, SiteNavComponent, SiteFooterComponent],
  template: `
    @if (siteConfig().sectionVisibility.nav) {
      <app-site-nav />
    }

    <main class="about-page">
      <section class="about-hero">
        <div class="container about-hero-grid">
          <div class="about-hero-copy">
            <span class="hero-badge">{{ about().badge }}</span>
            <h1>{{ about().title }} <span class="accent">{{ about().accent }}</span></h1>
            <p class="about-lead">{{ about().lead }}</p>

            <div class="about-intro">
              @for (paragraph of about().intro; track $index) {
                <p>{{ paragraph }}</p>
              }
            </div>

            @if (isInternalHref(about().closingCta.href)) {
              <a class="btn btn-primary" [routerLink]="about().closingCta.href">{{ about().closingCta.label }}</a>
            } @else {
              <a class="btn btn-primary" [href]="about().closingCta.href">{{ about().closingCta.label }}</a>
            }
          </div>

          <aside class="about-summary-card">
            <span class="section-tag">{{ about().projectsEyebrow }}</span>
            <h2>{{ about().projectsTitle }}</h2>
            <p>{{ about().projectsSubtitle }}</p>

            <div class="about-summary-list">
              @for (project of about().projects; track project.id) {
                <article class="about-summary-item">
                  <span class="about-summary-label">{{ project.label }}</span>
                  <h3>{{ project.title }}</h3>
                  <p>{{ project.description }}</p>
                </article>
              }
            </div>
          </aside>
        </div>
      </section>

      <section class="about-projects">
        <div class="container">
          <div class="section-head about-section-head">
            <span class="section-tag">{{ about().projectsEyebrow }}</span>
            <h2>{{ about().projectsTitle }}</h2>
            <p>{{ about().projectsSubtitle }}</p>
          </div>

          <div class="about-project-grid">
            @for (project of about().projects; track project.id) {
              <article class="about-project-card">
                <span class="about-project-label">{{ project.label }}</span>
                <h3>{{ project.title }}</h3>
                <p>{{ project.description }}</p>

                <ul class="about-project-points">
                  @for (point of project.points; track point) {
                    <li>{{ point }}</li>
                  }
                </ul>
              </article>
            }
          </div>
        </div>
      </section>

      <section class="about-vision">
        <div class="container about-vision-grid">
          <div class="about-vision-copy">
            <span class="section-tag">{{ about().visionEyebrow }}</span>
            <h2>{{ about().visionTitle }}</h2>
            <p>{{ about().visionDescription }}</p>
          </div>

          <div class="about-principles-card">
            <ul class="about-principles">
              @for (principle of about().visionPrinciples; track principle) {
                <li>{{ principle }}</li>
              }
            </ul>
          </div>
        </div>
      </section>

      <section class="about-benefits">
        <div class="container">
          <div class="section-head about-section-head">
            <span class="section-tag">{{ about().benefitsEyebrow }}</span>
            <h2>{{ about().benefitsTitle }}</h2>
          </div>

          <div class="about-benefits-grid">
            @for (benefit of about().benefits; track benefit.title) {
              <article class="about-benefit-card">
                <h3>{{ benefit.title }}</h3>
                <p>{{ benefit.description }}</p>
              </article>
            }
          </div>
        </div>
      </section>

      <section class="about-closing">
        <div class="container">
          <div class="about-closing-card">
            <h2>{{ about().closingTitle }}</h2>
            <p>{{ about().closingDescription }}</p>
            @if (isInternalHref(about().closingCta.href)) {
              <a class="btn btn-primary" [routerLink]="about().closingCta.href">{{ about().closingCta.label }}</a>
            } @else {
              <a class="btn btn-primary" [href]="about().closingCta.href">{{ about().closingCta.label }}</a>
            }
          </div>
        </div>
      </section>
    </main>

    @if (siteConfig().sectionVisibility.footer) {
      <app-site-footer />
    }
  `
})
export class AboutPageComponent {
  private readonly platformConfigService = inject(PlatformConfigService);

  readonly siteConfig = this.platformConfigService.siteConfig;
  readonly about = computed(() => this.platformConfigService.locale().about);

  isInternalHref(href: string): boolean {
    return href.startsWith('/');
  }
}
