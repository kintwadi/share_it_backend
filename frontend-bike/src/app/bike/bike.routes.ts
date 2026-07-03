import { Routes } from '@angular/router';
import { authGuard } from '../core/guards/auth.guard';

export const BIKE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/bike-search/bike-search.component').then(m => m.BikeSearchComponent)
  },
  {
    path: 'admin/catalog',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/bike-catalog-admin/bike-catalog-admin.component').then(m => m.BikeCatalogAdminComponent)
  },
  {
    path: 'admin/catalog/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/bike-catalog-admin/bike-catalog-admin.component').then(m => m.BikeCatalogAdminComponent)
  },
  {
    path: 'catalog/:id',
    loadComponent: () => import('./pages/bike-catalog-detail/bike-catalog-detail.component').then(m => m.BikeCatalogDetailComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/bike-detail/bike-detail.component').then(m => m.BikeDetailComponent)
  }
];
