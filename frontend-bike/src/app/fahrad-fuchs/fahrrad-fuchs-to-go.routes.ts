import { Routes } from '@angular/router';
import { authGuard } from '../core/guards/auth.guard';

export const FAHRRAD_FUCHS_TO_GO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/fahrrad-fuchs-to-go-home.component').then((m) => m.FahrradFuchsToGoHomeComponent)
  },
  {
    path: 'bikes/:slug',
    loadComponent: () => import('./pages/fahrrad-fuchs-to-go-bike-detail.component').then((m) => m.FahrradFuchsToGoBikeDetailComponent)
  },
  {
    path: 'checkout/:slug',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/fahrrad-fuchs-to-go-checkout.component').then((m) => m.FahrradFuchsToGoCheckoutComponent)
  },
  {
    path: 'bookings',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/fahrrad-fuchs-to-go-bookings.component').then((m) => m.FahrradFuchsToGoBookingsComponent)
  }
];
