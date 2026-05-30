import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-cookie-notice',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cookie-notice.component.html'
})
export class CookieNoticeComponent {
  constructor(public i18n: I18nService) {}
}
