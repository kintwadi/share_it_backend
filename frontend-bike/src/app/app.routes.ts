import { Routes, UrlSegment } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

const fahrradFuchsListingAlias = (url: UrlSegment[]) =>
  url.length === 2 && url[0].path === 'bikes' && url[1].path === 'fahrrad-fuchs'
    ? { consumed: url }
    : null;

const fahrradFuchsDetailAlias = (url: UrlSegment[]) =>
  url.length === 3 && url[0].path === 'bikes' && url[1].path === 'fahrrad-fuchs'
    ? {
        consumed: url,
        posParams: {
          slug: url[2]
        }
      }
    : null;

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'bikes',
    pathMatch: 'full'
  },
  {
    path: 'discover',
    redirectTo: 'bikes',
    pathMatch: 'full'
  },
  {
    matcher: fahrradFuchsListingAlias,
    loadComponent: () => import('./fahrad-fuchs/pages/fahrrad-fuchs-to-go-home.component').then((m) => m.FahrradFuchsToGoHomeComponent)
  },
  {
    matcher: fahrradFuchsDetailAlias,
    loadComponent: () => import('./fahrad-fuchs/pages/fahrrad-fuchs-to-go-bike-detail.component').then((m) => m.FahrradFuchsToGoBikeDetailComponent)
  },
  {
    path: 'bikes',
    loadChildren: () => import('./bike/bike.routes').then((m) => m.BIKE_ROUTES)
  },
  {
    path: 'fahrad-fuchs',
    loadChildren: () => import('./fahrad-fuchs/fahrrad-fuchs-to-go.routes').then((m) => m.FAHRRAD_FUCHS_TO_GO_ROUTES)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'password-recovery',
    loadComponent: () => import('./features/auth/password-recovery.component').then((m) => m.PasswordRecoveryComponent)
  },
  {
    path: 'subscription',
    loadComponent: () => import('./features/subscription/subscription.component').then((m) => m.SubscriptionComponent)
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent)
  },
  {
    path: 'listings/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/list-bike/list-bike.component').then((m) => m.ListBikeComponent)
  },
  {
    path: '**',
    redirectTo: 'bikes'
  }
];
