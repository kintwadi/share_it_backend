import { Routes } from '@angular/router';

export const BIKE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/bike-search/bike-search.component').then(m => m.BikeSearchComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/bike-detail/bike-detail.component').then(m => m.BikeDetailComponent)
  }
];
