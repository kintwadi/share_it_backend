import { Component, computed, inject } from '@angular/core';
import { PlatformConfigService } from '../../services/platform-config.service';

@Component({
  selector: 'app-site-footer',
  standalone: true,
  template: `
    @if (footer().showSection) {
      <footer id="contact">
        <div class="footer-inner">
          <div class="footer-grid">
            <section class="footer-brand">
              <a class="logo" href="#top" [attr.aria-label]="footer().brand.ariaLabel">
                <span class="logo-mark">{{ footer().brand.mark }}</span>
                <strong>{{ footer().brand.name }}</strong>
              </a>
              <p>{{ footer().description }}</p>
            </section>

            @for (group of footer().groups; track group.id) {
              <section class="footer-col">
                <h4>{{ group.title }}</h4>
                @for (link of group.links; track link.label) {
                  <a [href]="link.href">{{ link.label }}</a>
                }
              </section>
            }

            <section class="footer-col footer-contact">
              <h4>{{ footer().contactTitle }}</h4>
              <a [href]="'mailto:' + footer().email">{{ footer().email }}</a>
              <a [href]="'tel:' + footer().phone.replaceAll(' ', '').replaceAll('(', '').replaceAll(')', '').replaceAll('-', '')">{{ footer().phone }}</a>
            </section>
          </div>

          <div class="footer-bottom">
            <span>{{ footer().bottomLeft }}</span>
            @if (footer().bottomRight) {
              <span>{{ footer().bottomRight }}</span>
            }
          </div>
        </div>
      </footer>
    }
  `
})
export class SiteFooterComponent {
  private readonly platformConfigService = inject(PlatformConfigService);
  readonly footer = computed(() => this.platformConfigService.locale().footer);
}
