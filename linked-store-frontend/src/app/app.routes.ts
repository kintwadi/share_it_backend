import { Routes } from '@angular/router';
import { CatalogPageComponent } from './features/catalog/catalog-page.component';

export const routes: Routes = [
  { path: '', component: CatalogPageComponent },
  { path: 'products/:id', component: CatalogPageComponent },
  { path: 'stores/:storeSlug', component: CatalogPageComponent },
  { path: 'stores/:storeSlug/products/:id', component: CatalogPageComponent },
  { path: '**', redirectTo: '' }
];
