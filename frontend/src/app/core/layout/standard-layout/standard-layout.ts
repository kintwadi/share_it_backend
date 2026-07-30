import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { CookieConsentComponent } from '../../../shared/components/cookie-consent/cookie-consent.component';
import { Layout } from '../layout/layout';

@Component({
  selector: 'app-standard-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, LucideAngularModule, CookieConsentComponent],
  templateUrl: './standard-layout.html',
  styleUrl: './standard-layout.css'
})
export class StandardLayout extends Layout {}
