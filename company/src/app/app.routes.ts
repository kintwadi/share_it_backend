import { Routes } from '@angular/router';
import { AboutPageComponent } from './pages/about-page.component';
import { ContactPageComponent } from './pages/contact-page.component';
import { LandingPageComponent } from './pages/landing-page.component';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'about', component: AboutPageComponent },
  { path: 'contact', component: ContactPageComponent },
  { path: '**', redirectTo: '' }
];
