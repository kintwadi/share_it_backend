import { Routes } from '@angular/router';
import { canMatchAdmin, canMatchBorrowerSubscription, canMatchDashboard, canMatchMailbox, canMatchMessages, canMatchNewItem, canMatchPartner, canMatchSettings, canMatchSubscription } from './core/guards/route-guards';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'enterprise', loadComponent: () => import('./features/enterprise-home/enterprise-home.component').then(m => m.EnterpriseHomeComponent) },
  { path: 'connect', loadComponent: () => import('./features/connect/connect.component').then(m => m.ConnectComponent) },
  { path: 'connect/admin', loadComponent: () => import('./features/connect-admin/connectAdmin').then(m => m.ConnectAdminComponent) },
  { path: 'connect/partner', loadComponent: () => import('./features/connect-partner/connectPartner').then(m => m.ConnectPartnerComponent) },
  { path: 'dashboard', canMatch: [canMatchDashboard], loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
  { path: 'admin', canMatch: [canMatchAdmin], loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent) },
  { path: 'partner', canMatch: [canMatchPartner], loadChildren: () => import('./features/partner/partner.routes').then(m => m.PARTNER_ROUTES) },
  { path: 'messages', canMatch: [canMatchMessages], loadComponent: () => import('./features/messages/messages.component').then(m => m.MessagesComponent) },
  { path: 'mailbox', canMatch: [canMatchMailbox], loadComponent: () => import('./features/mailbox/mailbox.component').then(m => m.MailboxComponent) },
  { path: 'listing/:id', loadComponent: () => import('./features/listing-detail/listing-detail.component').then(m => m.ListingDetailComponent) },
  { path: 'new', canMatch: [canMatchNewItem], loadComponent: () => import('./features/new-item/new-item.component').then(m => m.NewItemComponent) },
  { path: 'new-item', canMatch: [canMatchNewItem], loadComponent: () => import('./features/new-item/new-item.component').then(m => m.NewItemComponent) },
  { path: 'edit/:id', canMatch: [canMatchNewItem], loadComponent: () => import('./features/new-item/new-item.component').then(m => m.NewItemComponent) },
  { path: 'settings', canMatch: [canMatchSettings], loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent) },
  { path: 'rate', loadComponent: () => import('./features/rate/rate.component').then(m => m.RateComponent) },
  { path: 'subscription', canMatch: [canMatchSubscription], loadComponent: () => import('./features/subscription/subscription.component').then(m => m.SubscriptionComponent) },
  { path: 'subscription/confirm', canMatch: [canMatchSubscription], loadComponent: () => import('./features/subscription-confirm/subscription-confirm.component').then(m => m.SubscriptionConfirmComponent) },
  { path: 'subscription/checkout', canMatch: [canMatchSubscription], loadComponent: () => import('./features/subscription-checkout/subscription-checkout.component').then(m => m.SubscriptionCheckoutComponent) },
  { path: 'subscription/upgrade', canMatch: [canMatchSubscription], loadComponent: () => import('./features/subscription-upgrade/subscription-upgrade.component').then(m => m.SubscriptionUpgradeComponent) },
  { path: 'borrower-subscription', canMatch: [canMatchBorrowerSubscription], loadComponent: () => import('./features/borrower-subscription/borrower-subscription.component').then(m => m.BorrowerSubscriptionComponent) },
  { path: 'verification/email', loadComponent: () => import('./features/email-verification/email-verification.component').then(m => m.EmailVerificationComponent) },
  { path: '**', redirectTo: '' }
];
