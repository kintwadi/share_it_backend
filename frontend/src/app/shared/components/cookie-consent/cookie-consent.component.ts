import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Cookie } from 'lucide-angular';
import { CookieConsentService } from '../../../core/services/cookie-consent.service';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-cookie-consent',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './cookie-consent.component.html'
})
export class CookieConsentComponent {
  private consent = inject(CookieConsentService);
  i18n = inject(I18nService);

  readonly Cookie = Cookie;
  decision = this.consent.decision;

  accept() {
    this.consent.accept();
  }

  deny() {
    this.consent.deny();
  }
}
