import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { 
  LucideAngularModule, 
  Home, 
  LayoutDashboard, 
  Shield, 
  MessageSquare, 
  Settings, 
  LogOut, 
  Zap, 
  LogIn, 
  Menu, 
  X, 
  UserCircle, 
  ChevronDown, 
  Bell,
  Globe,
  Building2
} from 'lucide-angular';
import { SessionService } from '../../services/session.service';
import { Notification } from '../../models/types';
import { NotificationService } from '../../services/notification.service';
import { NotificationType } from '../../models/types';
import { SettingsConfigService } from '../../services/settings-config.service';
import { I18nService } from '../../services/i18n.service';
import { UserPreferencesService } from '../../services/user-preferences.service';
import { AuthStorageService } from '../../services/auth-storage.service';
import { CookieConsentComponent } from '../../../shared/components/cookie-consent/cookie-consent.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, LucideAngularModule, CookieConsentComponent],
  templateUrl: './layout.html',
  styleUrl: './layout.css'
})
export class Layout {
  readonly Home = Home;
  readonly LayoutDashboard = LayoutDashboard;
  readonly Shield = Shield;
  readonly MessageSquare = MessageSquare;
  readonly Settings = Settings;
  readonly LogOut = LogOut;
  readonly Zap = Zap;
  readonly LogIn = LogIn;
  readonly Menu = Menu;
  readonly X = X;
  readonly UserCircle = UserCircle;
  readonly ChevronDown = ChevronDown;
  readonly Bell = Bell;
  readonly Globe = Globe;
  readonly Building2 = Building2;

  private router = inject(Router);
  private session = inject(SessionService);
  private notificationsApi = inject(NotificationService);
  private settingsConfig = inject(SettingsConfigService);
  private prefs = inject(UserPreferencesService);
  private authStorage = inject(AuthStorageService);
  i18n = inject(I18nService);

  isMenuOpen = signal(false);
  isProfileDropdownOpen = signal(false);
  isLangDropdownOpen = signal(false);
  isCurrDropdownOpen = signal(false);
  isNotifDropdownOpen = signal(false);

  currentUser = this.session.user;
  subscription = this.session.subscription;
  isAdmin = computed(() => {
    const role = String(this.currentUser()?.role ?? '').toUpperCase();
    return role === 'ADMIN' || role === 'ROLE_ADMIN';
  });
  isPartnerScopedAdmin = computed(() => {
    if (!this.isAdmin()) return false;
    return String((this.currentUser() as any)?.adminScope ?? '').toUpperCase() === 'PARTNER';
  });
  hasSubscription = computed(() => {
    if (!this.settingsConfig.isSectionEnabled('enable', 'subscription')) return !!this.currentUser();
    const sub = this.subscription();
    if (!sub) return false;
    const status = String(sub.status || '').toLowerCase();
    return status === 'active' || status === 'trialing';
  });
  showNotificationsBell = computed(() => {
    if (!this.currentUser()) return false;
    if (!this.hasSubscription()) return false;
    if (!this.prefs.prefs().pushNotifications) return false;
    return this.isHeaderSectionEnabled('notifications');
  });
  notifications = signal<Notification[]>([]);
  unreadCount = computed(() => this.notifications().filter(n => !n.isRead).length);
  groupedNotifications = computed(() => {
    const list = this.notifications();
    const pending = list.filter(n => n.type === NotificationType.PENDING_REQUEST);
    const price = list.filter(n => n.type === NotificationType.PRICE_SUGGESTION);
    const updates = list.filter(n => n.type !== NotificationType.PENDING_REQUEST && n.type !== NotificationType.PRICE_SUGGESTION);
    return [
      { title: this.i18n.t('notif.section.pending_requests'), items: pending },
      { title: this.i18n.t('notif.section.price_suggestions'), items: price },
      { title: this.i18n.t('notif.section.updates'), items: updates }
    ].filter(s => s.items.length > 0);
  });
  
  currency = this.i18n.currency;
  language = this.i18n.language;
  
  isHomePage = signal(true);
  currentPath = signal('');
  isPartnerContext = computed(() => this.authStorage.authContext() === 'partner');
  isPartnerArea = computed(() => String(this.currentPath() || '').startsWith('/partner'));
  enterpriseEnabled = computed(() => {
    const cfg = this.settingsConfig.config();
    const raw = cfg?.enable?.enterprise;
    return raw === true || raw === 'true';
  });

  constructor() {
    this.session.refresh();
    this.settingsConfig.ensureLoaded();
    effect(() => {
      if (!this.showNotificationsBell()) {
        this.isNotifDropdownOpen.set(false);
      }
    });
    effect(() => {
      const u = this.currentUser();
      const showNotifications = this.showNotificationsBell();
      if (!u || !showNotifications) {
        this.notifications.set([]);
        return;
      }
      this.loadNotifications();
    });
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const url = String(event.urlAfterRedirects || '');
      setTimeout(() => {
        this.isHomePage.set(url === '/' || url === '/home');
        this.currentPath.set(url);
        this.isMenuOpen.set(false);
        this.isProfileDropdownOpen.set(false);
        this.isNotifDropdownOpen.set(false);
        if (!this.currentUser() && !!this.session.user()) return;
        this.session.refresh();
      }, 0);
    });
  }

  toggleMenu() {
    this.isMenuOpen.update(v => !v);
  }

  toggleProfileDropdown() {
    this.isProfileDropdownOpen.update(v => !v);
    if (this.isProfileDropdownOpen()) {
      this.isLangDropdownOpen.set(false);
      this.isCurrDropdownOpen.set(false);
    }
  }

  toggleCurrDropdown() {
    this.isCurrDropdownOpen.update(v => !v);
  }

  toggleLangDropdown() {
    this.isLangDropdownOpen.update(v => !v);
  }

  toggleNotifDropdown() {
    this.isNotifDropdownOpen.update(v => !v);
    if (this.isNotifDropdownOpen()) {
      this.loadNotifications();
    }
  }

  async loadNotifications() {
    try {
      const list = await this.notificationsApi.getNotifications();
      this.notifications.set(list);
    } catch {
      this.notifications.set([]);
    }
  }

  async markNotifRead(n: Notification) {
    try {
      await this.notificationsApi.markNotificationAsRead(n.id);
      const next = this.notifications().map(x => x.id === n.id ? { ...x, isRead: true } : x);
      this.notifications.set(next);
    } catch { }
  }

  async openNotif(n: Notification) {
    await this.markNotifRead(n);
    this.isNotifDropdownOpen.set(false);
    if (n.link) {
      const path = n.link.startsWith('/') ? n.link : `/${n.link}`;
      this.router.navigateByUrl(path);
    }
  }

  handleLogoutClick() {
    const ctx = this.authStorage.getAuthContext();
    this.session.logout();
    this.notifications.set([]);
    if (ctx === 'admin') {
      this.router.navigate(['/connect/admin']);
      return;
    }
    if (ctx === 'partner') {
      this.router.navigate(['/connect/partner']);
      return;
    }
    this.router.navigate(['/connect']);
  }

  changeCurrency() {
    const currs = this.i18n.supportedCurrencies;
    const idx = currs.indexOf(this.currency() as any);
    const next = currs[(idx + 1) % currs.length];
    this.i18n.setCurrency(next as any);
  }

  changeLanguage() {
    const cur = this.language();
    const next = cur === 'en' ? 'pt' : cur === 'pt' ? 'de' : 'en';
    this.i18n.setLanguage(next);
  }

  setCurrency(curr: any) {
    this.i18n.setCurrency(curr);
    this.isCurrDropdownOpen.set(false);
  }

  setLanguage(lang: any) {
    this.i18n.setLanguage(lang);
    this.isLangDropdownOpen.set(false);
  }

  languageLabel(lang?: any): string {
    return this.i18n.languageLabel(lang);
  }


  isHeaderSectionEnabled(section: string): boolean {
    return this.settingsConfig.isSectionEnabled('header', section);
  }
}
