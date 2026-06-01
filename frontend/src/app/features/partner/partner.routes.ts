import { Routes } from '@angular/router';

export const PARTNER_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () => import('./partner-dashboard/partner-dashboard.component').then(m => m.PartnerDashboardComponent) },
  { path: 'fill-request', loadComponent: () => import('./partner-submit-listing/partner-submit-listing.component').then(m => m.PartnerSubmitListingComponent) },
  { path: 'listings/add', loadComponent: () => import('./partner-add-listing/partner-add-listing.component').then(m => m.PartnerAddListingComponent) },
  { path: 'listings/edit/:id', loadComponent: () => import('./partner-edit-listing/partner-edit-listing.component').then(m => m.PartnerEditListingComponent) },
  { path: 'requests', loadComponent: () => import('./partner-requests/partner-requests.component').then(m => m.PartnerRequestsComponent) },
  { path: 'settings', loadComponent: () => import('./partner-settings/partner-settings.component').then(m => m.PartnerSettingsComponent) },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
];
