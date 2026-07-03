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
            <span class="section-tag">Contact Sales</span>
            <h1>Explore Solutions With Our Team</h1>
            <p>
              Share your goals and we will help you find the right setup for your business.
            </p>
          </div>
        </div>
      </section>

      <section class="contact-section">
        <div class="container contact-layout">
          <div class="contact-panel">
            <h2>Tell us what you need</h2>
            <p>
              Fill out the form and our team will follow up with next steps, pricing guidance, and the
              best product fit for your operation.
            </p>
            <ul class="contact-highlights">
              <li>Fast product guidance</li>
              <li>Implementation recommendations</li>
              <li>Customized solution matching</li>
            </ul>
          </div>

          <div class="contact-form-card">
            <form class="contact-form" (submit)="onSubmit($event)">
              <div class="form-grid">
                <label>
                  <span>Full name</span>
                  <input type="text" name="fullName" placeholder="Jane Doe" required [disabled]="isSubmitting()" />
                </label>
                <label>
                  <span>Work email</span>
                  <input type="email" name="email" placeholder="jane@company.com" required [disabled]="isSubmitting()" />
                </label>
              </div>

              <div class="form-grid">
                <label>
                  <span>Company</span>
                  <input type="text" name="company" placeholder="Your company" required [disabled]="isSubmitting()" />
                </label>
                <label>
                  <span>Solution of interest</span>
                  <input type="text" name="solution" [value]="selectedSolution()" readonly [disabled]="isSubmitting()" />
                </label>
              </div>

              <label>
                <span>Message</span>
                <textarea
                  name="message"
                  rows="6"
                  placeholder="Tell us about your business needs, locations, and goals."
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
                  {{ isSubmitting() ? 'Sending...' : 'Send Request' }}
                </button>
                <a routerLink="/" class="btn btn-outline">Back to Home</a>
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
  readonly solutionParam = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('solution'))),
    { initialValue: null }
  );
  readonly selectedSolution = computed(() => this.formatSolution(this.solutionParam()));
  readonly submitState = signal<'idle' | 'submitting' | 'success' | 'error'>('idle');
  readonly submitMessage = signal('');
  readonly isSubmitting = computed(() => this.submitState() === 'submitting');

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
      this.submitMessage.set('Your request has been sent. Our team will contact you soon.');
    } catch {
      this.submitState.set('error');
      this.submitMessage.set('We could not send your request right now. Please try again in a moment.');
    }
  }

  private formatSolution(value: string | null): string {
    if (!value) {
      return 'General inquiry';
    }

    return value
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/[-_]/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  private resolveContactEndpoint(): string {
    if (typeof window === 'undefined') {
      return 'https://www.vicinity24api.com/api/mail-contact-request';
    }

    const hostname = window.location.hostname.toLowerCase();
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8081/api/mail-contact-request';
    }

    return 'https://www.vicinity24api.com/api/mail-contact-request';
  }
}
