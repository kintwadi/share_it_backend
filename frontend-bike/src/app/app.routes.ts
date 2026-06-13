import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent)
  },
  {
    path: 'discover',
    redirectTo: 'bikes',
    pathMatch: 'full'
  },
  {
    path: 'bikes',
    loadChildren: () => import('./bike/bike.routes').then((m) => m.BIKE_ROUTES)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/signup.component').then((m) => m.SignupComponent)
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
    redirectTo: ''
  }
];
