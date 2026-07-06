import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { SiteFooterComponent } from '../components/sections/site-footer.component';
import { SiteNavComponent } from '../components/sections/site-nav.component';
import { PlatformConfigService } from '../services/platform-config.service';

@Component({
  selector: 'app-contact-page',
  standalone: true,
  imports: [RouterLink, SiteNavComponent, SiteFooterComponent],
  template: `
    @if (siteConfig().sectionVisibility.nav) {
      <app-site-nav />
    }

    <main class="contact-page">
      <section class="contact-hero">
        <div class="container">
          <div class="contact-hero-copy">
            <span class="section-tag">{{ contact().badge }}</span>
            <h1>{{ contact().title }}</h1>
            <p>
              {{ contact().description }}
            </p>
          </div>
        </div>
      </section>

      <section class="contact-section">
        <div class="container contact-layout">
          <div class="contact-panel">
            <h2>{{ contact().panelTitle }}</h2>
            <p>
              {{ contact().panelDescription }}
            </p>
            <ul class="contact-highlights">
              @for (highlight of contact().highlights; track highlight) {
                <li>{{ highlight }}</li>
              }
            </ul>

            <div class="contact-info-cards">
              <div class="contact-info-card">
                <span class="contact-info-label">{{ contact().phoneLabel }}</span>
                <a [href]="contactPhoneHref()">{{ locale().footer.phone }}</a>
              </div>
              <div class="contact-info-card">
                <span class="contact-info-label">{{ contact().addressLabel }}</span>
                <p>{{ locale().footer.address }}</p>
              </div>
            </div>
          </div>

          <div class="contact-form-card">
            <form class="contact-form" (submit)="onSubmit($event)">
              <div class="form-grid">
                <label>
                  <span>{{ contact().fullName.label }}</span>
                  <input type="text" name="fullName" [placeholder]="contact().fullName.placeholder" required [disabled]="isSubmitting()" />
                </label>
                <label>
                  <span>{{ contact().email.label }}</span>
                  <input type="email" name="email" [placeholder]="contact().email.placeholder" required [disabled]="isSubmitting()" />
                </label>
              </div>

              <div class="form-grid">
                <label>
                  <span>{{ contact().company.label }}</span>
                  <input type="text" name="company" [placeholder]="contact().company.placeholder" required [disabled]="isSubmitting()" />
                </label>
                <label>
                  <span>{{ contact().solution.label }}</span>
                  <input type="text" name="solution" [value]="selectedSolution()" readonly [disabled]="isSubmitting()" />
                </label>
              </div>

              <label>
                <span>{{ contact().message.label }}</span>
                <textarea
                  name="message"
                  rows="6"
                  [placeholder]="contact().message.placeholder"
                  required
                  [disabled]="isSubmitting()"></textarea>
              </label>

              @if (submitMessage()) {
                <p class="contact-status" [class.success]="submitState() === 'success'" [class.error]="submitState() === 'error'">
                  {{ submitMessage() }}
                </p>
              }

              <div class="contact-actions">
                <button type="submit" class="btn btn-primary" [disabled]="isSubmitting()">
                  {{ isSubmitting() ? contact().sendingLabel : contact().submitLabel }}
                </button>
                <a routerLink="/" class="btn btn-outline">{{ contact().backToHomeLabel }}</a>
              </div>
            </form>
          </div>
        </div>
      </section>
    </main>

    @if (siteConfig().sectionVisibility.footer) {
      <app-site-footer />
    }
  `
})
export class ContactPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly platformConfigService = inject(PlatformConfigService);
  private readonly contactEndpoint = this.resolveContactEndpoint();
  private readonly publicHeaderName = 'X-Public-Origin';
  private readonly publicHeaderValue = 'vicinity24.com';

  readonly siteConfig = this.platformConfigService.siteConfig;
  readonly locale = this.platformConfigService.locale;
  readonly contact = computed(() => this.locale().contact);
  readonly solutionParam = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('solution'))),
    { initialValue: null }
  );
  readonly selectedSolution = computed(() => this.formatSolution(this.solutionParam()));
  readonly submitState = signal<'idle' | 'submitting' | 'success' | 'error'>('idle');
  readonly submitMessage = signal('');
  readonly isSubmitting = computed(() => this.submitState() === 'submitting');
  readonly contactPhoneHref = computed(() => this.toTelHref(this.locale().footer.phone));

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();

    const form = event.target as HTMLFormElement | null;
    if (!form || this.isSubmitting()) {
      return;
    }

    if (!form.reportValidity()) {
      return;
    }

    const formData = new FormData(form);
    const payload = {
      fullName: String(formData.get('fullName') ?? '').trim(),
      email: String(formData.get('email') ?? '').trim(),
      company: String(formData.get('company') ?? '').trim(),
      solution: String(formData.get('solution') ?? '').trim() || this.selectedSolution(),
      message: String(formData.get('message') ?? '').trim()
    };

    this.submitState.set('submitting');
    this.submitMessage.set('');

    try {
      const response = await fetch(this.contactEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [this.publicHeaderName]: this.publicHeaderValue
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error('request_failed');
      }

      form.reset();
      this.submitState.set('success');
      this.submitMessage.set(this.contact().successMessage);
    } catch {
      this.submitState.set('error');
      this.submitMessage.set(this.contact().errorMessage);
    }
  }

  private formatSolution(value: string | null): string {
    if (!value) {
      return this.contact().generalInquiryLabel;
    }

    return value
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/[-_]/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  private resolveContactEndpoint(): string {
    if (typeof window === 'undefined') {
      return 'https://vicinity24api.com/api/mail-contact-request';
    }

    const hostname = window.location.hostname.toLowerCase();
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8081/api/mail-contact-request';
    }

    const runtimeApiUrl = this.normalizeBaseApiUrl((window as any).__env?.BASE_API_URL);
    if (runtimeApiUrl) {
      return `${runtimeApiUrl}/api/mail-contact-request`;
    }

    return 'https://vicinity24api.com/api/mail-contact-request';
  }

  private normalizeBaseApiUrl(value: unknown): string | null {
    const trimmed = String(value ?? '').trim();
    if (!trimmed) {
      return null;
    }

    const withProtocol = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
    return withProtocol.replace(/\/+$/, '');
  }

  private toTelHref(phone: string): string {
    const normalized = String(phone ?? '').replace(/[^\d+]/g, '');
    return normalized ? `tel:${normalized}` : 'tel:';
  }
}
