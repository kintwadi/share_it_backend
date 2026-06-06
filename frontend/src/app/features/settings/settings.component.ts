import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Bell, Shield, User as UserIcon, CreditCard, Lock, Home, HelpCircle, BarChart2, Globe, AlertTriangle, Download, Trash2, CheckCircle2, Clock, Mail, Phone, MapPin, Star, ChevronRight, Menu, X, Loader2, LogOut, Camera } from 'lucide-angular';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { AvailabilityStatus, ListingType, User } from '../../core/models/types';
import { ButtonComponent } from '../../shared/components/button/button';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { PaymentSettingsComponent } from '../../shared/components/payment-settings/payment-settings';
import { UserPreferencesService } from '../../core/services/user-preferences.service';
import { PHONE_COUNTRIES, isoToFlag } from '../../shared/data/phone-countries';

type SettingsTabId =
  | 'overview'
  | 'profile'
  | 'subscription'
  | 'security'
  | 'privacy'
  | 'notifications'
  | 'payments'
  | 'building'
  | 'stats'
  | 'support';

interface TabConfig {
  id: SettingsTabId;
  labelKey: string;
  icon: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ButtonComponent, PaymentSettingsComponent],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit {
  api = inject(ApiService);
  i18n = inject(I18nService);
  cdr = inject(ChangeDetectorRef);
  router = inject(Router);
  route = inject(ActivatedRoute);
  settingsConfig = inject(SettingsConfigService);
  prefs = inject(UserPreferencesService);

  readonly Bell = Bell;
  readonly Shield = Shield;
  readonly UserIcon = UserIcon;
  readonly CreditCard = CreditCard;
  readonly Lock = Lock;
  readonly Home = Home;
  readonly HelpCircle = HelpCircle;
  readonly BarChart2 = BarChart2;
  readonly Globe = Globe;
  readonly Download = Download;
  readonly Menu = Menu;
  readonly X = X;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly Trash2 = Trash2;
  readonly Clock = Clock;
  readonly Mail = Mail;
  readonly Phone = Phone;
  readonly MapPin = MapPin;
  readonly Star = Star;
  readonly ChevronRight = ChevronRight;
  readonly Loader2 = Loader2;
  readonly LogOut = LogOut;
  readonly Camera = Camera;

  user: User | null = null;

  overviewLoading = true;
  overviewListingCount = 0;
  hasLendListing = false;
  loading = true;
  activeTab: SettingsTabId = 'overview';
  sidebarOpen = false;

  // Profile
  name = '';
  email = '';
  phone = '';
  displayName = '';
  avatarUrl = '';
  isSavingProfile = false;
  isUploadingAvatar = false;
  profileError: string | null = null;
  profileSuccess: string | null = null;
  private profileNoticeTimer: any = null;

  // Security
  oldPassword = '';
  newPassword = '';
  confirmPassword = '';
  isChangingPassword = false;
  passwordError: string | null = null;
  passwordSuccess: string | null = null;
  verificationAddress = '';
  verificationPhone = '';
  verificationSubmitting = false;
  verificationError: string | null = null;
  verificationSuccess: string | null = null;

  // Subscription
  subscription: any | null = null;
  invoices: any[] = [];
  isLoadingSubscription = false;
  isLoadingInvoices = false;
  cancelConfirmOpen = false;
  cancelConfirmLoading = false;
  cancelError: string | null = null;

  // Devices
  devices: any[] = [];
  isLoadingDevices = false;
  revokeDeviceOpen = false;
  revokeDeviceLoading = false;
  revokeDeviceTarget: any | null = null;
  devicesError: string | null = null;
  devicesSuccess: string | null = null;

  // Account
  deleteAccountOpen = false;
  deleteAccountLoading = false;
  deleteAccountError: string | null = null;
  deleteConfirmChecked = false;

  // Toggles
  twoFactorEnabled = false;
  profileVisibility = true;
  reviewsPublic = true;
  emailNotifications = true;

  countries = PHONE_COUNTRIES.map(c => ({ ...c, flag: isoToFlag(c.code) }));
  selectedCountryCode: string = 'GB';
  pushNotifications = true;
  marketingConsent = false;
  locationData = true;
  researchOptIn = false;
  notificationFrequency: 'immediate' | 'daily' | 'weekly' = 'immediate';
  dndFrom = '21:00';
  dndTo = '07:00';

  // Building
  buildingSearch = '';
  manualPostalCode = '';
  manualBuildingName = '';

  twoFactorSetupOpen = false;
  twoFactorQrCode: string | null = null;
  twoFactorVerifyCode = '';
  twoFactorLoading = false;
  twoFactorError: string | null = null;
  securitySuccess: string | null = null;
  disable2FAOpen = false;
  isDisabling2FA = false;

  get twoFactorVisualOn(): boolean {
    return !!(this.twoFactorEnabled || this.twoFactorSetupOpen);
  }

  tabs: TabConfig[] = [
    { id: 'overview', labelKey: 'settings.tabs.overview', icon: '📱' },
    { id: 'profile', labelKey: 'settings.tabs.profile', icon: '👤' },
    { id: 'subscription', labelKey: 'settings.tabs.subscription', icon: '💳' },
    { id: 'security', labelKey: 'settings.tabs.security', icon: '🔒' },
    { id: 'privacy', labelKey: 'settings.tabs.privacy', icon: '🛡️' },
    { id: 'notifications', labelKey: 'settings.tabs.notifications', icon: '🔔' },
    { id: 'payments', labelKey: 'settings.tabs.payments', icon: '💰' },
    { id: 'building', labelKey: 'settings.tabs.building', icon: '🏠' },
    { id: 'stats', labelKey: 'settings.tabs.stats', icon: '📊' },
    { id: 'support', labelKey: 'settings.tabs.support', icon: '❓' }
  ];

  private isAdminUser(): boolean {
    const role = String((this.user as any)?.role ?? '').toUpperCase();
    return role === 'ADMIN' || role === 'ROLE_ADMIN';
  }

  get visibleTabs(): TabConfig[] {
    const admin = this.isAdminUser();
    const subscriptionEnabled = this.settingsConfig.isSectionEnabled('enable', 'subscription');
    return this.tabs.filter(t =>
      this.settingsConfig.isTabEnabled(t.id) &&
      (!admin || t.id !== 'subscription') &&
      (subscriptionEnabled || t.id !== 'subscription')
    );
  }

  get paymentsLocked(): boolean {
    const subscriptionEnabled = this.settingsConfig.isSectionEnabled('enable', 'subscription');
    return !subscriptionEnabled && !this.hasLendListing;
  }

  get subscriptionPlanLabel(): string {
    const planType = this.normalizedSubscriptionPlanType;
    if (planType === 'starter') return this.i18n.t('settings.subscription.plan_starter');
    if (planType === 'plus') return this.i18n.t('settings.subscription.plan_plus');
    if (planType === 'pro') return this.i18n.t('settings.subscription.plan_pro');
    if (planType === 'premium_lender') return this.i18n.t('settings.subscription.plan_premium_lender');
    return planType.charAt(0).toUpperCase() + planType.slice(1);
  }

  get subscriptionStatusLabel(): string {
    const sub = this.subscription;
    if (!sub) return this.i18n.t('settings.subscription.status_free');
    const status = String(sub?.status || '').toLowerCase();
    if (status === 'trial_active' || status === 'trialing') return this.i18n.t('settings.subscription.status_trial_active');
    if (status === 'active') return this.i18n.t('settings.subscription.status_active');
    if (status === 'canceled') return this.i18n.t('settings.subscription.status_canceled');
    return status || this.i18n.t('settings.subscription.status_active');
  }

  get isTrialSubscription(): boolean {
    const s = String(this.subscription?.status || '').toLowerCase();
    return s === 'trial_active' || s === 'trialing';
  }

  get isCanceledSubscription(): boolean {
    return String(this.subscription?.status || '').toLowerCase() === 'canceled';
  }

  get showSubscriptionBadge(): boolean {
    return !!this.subscription && this.normalizedSubscriptionPlanType !== 'starter';
  }

  get subscriptionStatusBadgeClass(): string {
    if (this.isCanceledSubscription) {
      return 'bg-gray-100 text-gray-700';
    }
    if (this.isTrialSubscription) {
      return 'bg-amber-100 text-amber-700';
    }
    return 'bg-emerald-100 text-emerald-700';
  }

  get trialCountdownText(): string | null {
    const sub = this.subscription;
    if (!this.isTrialSubscription) return null;
    const trialEnd = sub?.trialEnd;
    if (!trialEnd) return null;
    const end = new Date(String(trialEnd));
    const now = new Date();
    const diff = end.getTime() - now.getTime();
    if (isNaN(end.getTime()) || diff <= 0) return null;
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
    return String(days) + this.i18n.t('settings.subscription.trial_suffix');
  }

  get autoChargeText(): string | null {
    const sub = this.subscription;
    const autoChargeDate = sub?.autoChargeDate;
    if (!autoChargeDate) return null;
    const d = new Date(String(autoChargeDate));
    if (isNaN(d.getTime())) return null;
    const cents = Number(sub?.autoChargeAmountCents || 0);
    const locale = this.i18n.language() === 'de' ? 'de-DE' : this.i18n.language() === 'pt' ? 'pt-PT' : 'en-GB';
    const amount = cents > 0 ? new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' }).format(cents / 100) : '';
    return this.i18n.t('settings.subscription.auto_charge_prefix') + amount + this.i18n.t('settings.subscription.auto_charge_suffix') + d.toLocaleDateString(locale);
  }

  get subscriptionUpgradeCta(): string {
    const sub = this.subscription;
    const planType = this.normalizedSubscriptionPlanType;
    if (!sub || planType === 'starter') return this.i18n.t('settings.subscription.upgrade_plan');
    return this.i18n.t('settings.subscription.change_plan');
  }

  get normalizedSubscriptionPlanType(): string {
    const raw = String(this.subscription?.planType || 'starter').trim().toLowerCase();
    if (!raw) return 'starter';
    if (raw === 'verified') return 'plus';
    if (raw === 'premium') return 'pro';
    return raw;
  }

  goToSubscriptionPlans() {
    if (!this.settingsConfig.isSectionEnabled('enable', 'subscription')) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.router.navigate(['/subscription'], { state: { fromUpgrade: true } as any });
  }

  get invoiceRows(): { date: Date; statusKey: string; variant: 'success' | 'neutral'; pdfUrl: string }[] {
    if (!Array.isArray(this.invoices) || this.invoices.length === 0) return [];
    return this.invoices
      .map(inv => {
        const rawDate = inv?.invoiceDate || inv?.date || inv?.createdAt;
        const date = rawDate ? new Date(rawDate) : new Date();
        const pdfUrl = String(inv?.invoicePdfUrl || inv?.invoiceUrl || inv?.url || '');
        return {
          date,
          statusKey: 'settings.subscription.invoice_row2_status',
          variant: 'success' as const,
          pdfUrl,
        };
      })
      .filter(row => !!row.pdfUrl);
  }

  get verificationStatusLabel(): string {
    const s = String(this.user?.verificationStatus || 'UNVERIFIED');
    if (s === 'VERIFIED') return this.i18n.t('settings.security.verification_verified');
    if (s === 'PENDING') return this.i18n.t('settings.security.verification_pending');
    if (s === 'REJECTED') return this.i18n.t('settings.security.verification_rejected');
    if (s === 'UNVERIFIED') return this.i18n.t('settings.security.verification_unverified');
    return s;
  }

  get supportContactConfig(): any {
    return this.settingsConfig.config()?.support?.contact;
  }

  get supportContactEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('support', 'contact');
  }

  get supportFaqEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('support', 'faq');
  }

  get supportEmail(): string {
    return String(this.supportContactConfig?.email || '');
  }

  get supportEmailHref(): string {
    return `mailto:${this.supportEmail}`;
  }

  get supportPhone(): string {
    return String(this.supportContactConfig?.phone || '');
  }

  get supportPhoneHref(): string {
    const cleaned = this.supportPhone.replace(/[^\d+]/g, '');
    return `tel:${cleaned}`;
  }

  get supportAddress(): string {
    return String(this.supportContactConfig?.address || '');
  }

  get footerGdprEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('footer' as any, 'gdpr_note');
  }

  get footerDpoEnabled(): boolean {
    return this.settingsConfig.isSectionEnabled('footer' as any, 'dpo_contact');
  }

  private requestedTab: SettingsTabId | null = null;

  ngOnInit() {
    this.applyPrefsFromService();
    this.route.queryParams.subscribe(params => {
      const t = params['tab'] as SettingsTabId | undefined;
      this.requestedTab = t || null;
      this.applyRequestedTab();
    });
    this.settingsConfig.ensureLoaded().then(() => this.applyRequestedTab());
    this.api.getCurrentUser().then(u => {
      this.user = u;
      if (u) {
        this.name = u.name;
        this.email = u.email;
        this.phone = u.phone || '';
        this.displayName = u.displayName || '';
        this.avatarUrl = u.avatarUrl || '';
        this.verificationPhone = u.phone || '';
        this.verificationAddress = (u as any).address || '';
        this.twoFactorEnabled = !!u.twoFactorEnabled;
        this.profileVisibility = u.profileVisible !== false;
        this.reviewsPublic = u.showRatings !== false;
        this.initCountryFromExistingPhone();
        if (!this.selectedCountryCode) this.setDefaultCountryFromLang();
      }
      this.loading = false;
      this.applyRequestedTab();
      if (!this.isAdminUser()) {
        this.loadSubscription();
      }
      this.loadOverview();
      this.loadDevices();
      this.render();
    });
  }

  private setDefaultCountryFromLang() {
    const lang = this.i18n.language();
    this.selectedCountryCode = lang === 'de' ? 'DE' : lang === 'pt' ? 'PT' : 'GB';
  }

  private initCountryFromExistingPhone() {
    const digits = String(this.phone || '').replace(/\D/g, '');
    if (!digits) {
      this.setDefaultCountryFromLang();
      return;
    }

    let best: any | null = null;
    let bestLen = 0;
    for (const c of this.countries) {
      const dialDigits = String(c.dial || '').replace(/\D/g, '');
      if (!dialDigits) continue;
      if (digits.startsWith(dialDigits) && dialDigits.length > bestLen) {
        best = c;
        bestLen = dialDigits.length;
      }
    }

    if (!best) {
      this.setDefaultCountryFromLang();
      this.phone = digits;
      return;
    }

    this.selectedCountryCode = best.code;
    this.phone = digits.slice(bestLen);
  }

  get selectedCountry() {
    return this.countries.find(c => c.code === this.selectedCountryCode) || this.countries[0];
  }

  onCountryCodeChange() {
    this.phone = this.sanitizePhoneLocalPart(this.phone);
    this.render();
  }

  onPhoneChange(v: string) {
    this.phone = this.sanitizePhoneLocalPart(v);
    this.render();
  }

  private sanitizePhoneLocalPart(v: any): string {
    const raw = String(v || '');
    const trimmed = raw.trim();
    const hadPlus = trimmed.startsWith('+');
    const had00 = trimmed.startsWith('00');
    const maxDigits = 20;

    const selectedDialDigits = String(this.selectedCountry?.dial || '').replace(/\D/g, '');

    let digits = '';
    if (hadPlus) {
      digits = trimmed.slice(1).replace(/\D/g, '');
      const match = this.countries.find(c => digits.startsWith(String(c.dial).replace(/\D/g, '')));
      if (match) {
        const dialDigits = String(match.dial).replace(/\D/g, '');
        digits = digits.slice(dialDigits.length);
      }
      return digits.slice(0, maxDigits);
    }

    if (had00) {
      digits = trimmed.slice(2).replace(/\D/g, '');
      const match = this.countries.find(c => digits.startsWith(String(c.dial).replace(/\D/g, '')));
      if (match) {
        const dialDigits = String(match.dial).replace(/\D/g, '');
        digits = digits.slice(dialDigits.length);
      }
      return digits.slice(0, maxDigits);
    }

    digits = trimmed.replace(/\D/g, '');
    if (selectedDialDigits && digits.startsWith(selectedDialDigits) && digits.length > selectedDialDigits.length + 5) {
      return digits.slice(selectedDialDigits.length).slice(0, maxDigits);
    }

    return digits.slice(0, maxDigits);
  }

  private async loadOverview() {
    this.overviewLoading = true;
    this.render();
    try {
      const [sub, listings] = await Promise.all([
        this.api.getCurrentSubscription(),
        this.api.getListings(),
      ]);
      this.subscription = sub;
      const user = this.user;
      if (user) {
        const owned = listings.filter(l => l.ownerId === user.id);
        this.hasLendListing = owned.some(l => l.type === ListingType.LEND);
        const myListings = listings.filter(l =>
          l.ownerId === user.id &&
          l.status !== AvailabilityStatus.BLOCKED &&
          l.status !== AvailabilityStatus.HIDDEN &&
          l.status !== AvailabilityStatus.GIFTED &&
          l.status !== AvailabilityStatus.SOLD
        );
        this.overviewListingCount = myListings.length;
      } else {
        this.overviewListingCount = 0;
        this.hasLendListing = false;
      }
    } catch {
      this.overviewListingCount = 0;
      this.hasLendListing = false;
    } finally {
      this.overviewLoading = false;
      this.render();
    }
  }

  get trustedDevicesCount(): number {
    return Array.isArray(this.devices) ? this.devices.filter((d: any) => !!d?.trusted).length : 0;
  }

  get trustedDevicesSummary(): string {
    const n = this.trustedDevicesCount;
    if (n <= 0) return this.i18n.t('settings.overview.security_devices_hint');
    const unit = this.i18n.t(n === 1 ? 'settings.overview.security_devices_active_one' : 'settings.overview.security_devices_active_many');
    return `${n} ${unit}`;
  }

  get isVerifiedSubscriber(): boolean {
    const plan = this.normalizedSubscriptionPlanType;
    return plan === 'plus' || plan === 'pro' || plan === 'premium_lender';
  }

  private applyRequestedTab() {
    const tabs = this.visibleTabs;
    if (tabs.length === 0) return;
    const requested = this.requestedTab;
    if (requested && tabs.some(x => x.id === requested)) {
      this.activeTab = requested;
      this.render();
      return;
    }
    if (!tabs.some(t => t.id === this.activeTab)) {
      this.activeTab = tabs[0].id;
      this.render();
    }
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  handleTabClick(id: SettingsTabId) {
    if (id === 'subscription' && this.isAdminUser()) {
      this.activeTab = 'overview';
      this.sidebarOpen = false;
      this.render();
      return;
    }
    this.activeTab = id;
    this.sidebarOpen = false;
    if (id === 'subscription') {
      this.loadSubscription();
      this.loadInvoices();
    }
    if (id === 'security') {
      this.loadDevices();
    }
    this.render();
  }

  openBuildingPartnerPortal() {
    try {
      window.open('https://vicinity24.com/partners', '_blank', 'noopener,noreferrer');
    } catch { }
  }

  toggleEmailNotifications() {
    this.emailNotifications = !this.emailNotifications;
    this.prefs.update({ emailNotifications: this.emailNotifications });
    this.render();
  }

  togglePushNotifications() {
    this.pushNotifications = !this.pushNotifications;
    this.prefs.update({ pushNotifications: this.pushNotifications });
    this.render();
  }

  toggleMarketingConsent() {
    this.marketingConsent = !this.marketingConsent;
    this.prefs.update({ marketingConsent: this.marketingConsent });
    this.render();
  }

  toggleLocationData() {
    this.locationData = !this.locationData;
    this.prefs.update({ locationData: this.locationData });
    this.render();
  }

  toggleResearchOptIn() {
    this.researchOptIn = !this.researchOptIn;
    this.prefs.update({ researchOptIn: this.researchOptIn });
    this.render();
  }

  setNotificationFrequency(v: any) {
    const next = String(v);
    if (next === 'daily' || next === 'weekly' || next === 'immediate') {
      this.notificationFrequency = next;
      this.prefs.update({ notificationFrequency: this.notificationFrequency });
      this.render();
    }
  }

  updateDnd() {
    this.prefs.update({ dndFrom: this.dndFrom, dndTo: this.dndTo });
    this.render();
  }

  formatMaybeDate(v: any): string {
    const s = v ?? '';
    const parsed = new Date(String(s));
    if (!s || isNaN(parsed.getTime())) return '-';
    return this.i18n.formatDateTime(parsed);
  }

  private applyPrefsFromService() {
    const p = this.prefs.prefs();
    this.emailNotifications = p.emailNotifications;
    this.pushNotifications = p.pushNotifications;
    this.marketingConsent = p.marketingConsent;
    this.locationData = p.locationData;
    this.researchOptIn = p.researchOptIn;
    this.notificationFrequency = p.notificationFrequency;
    this.dndFrom = p.dndFrom;
    this.dndTo = p.dndTo;
  }

  async saveProfile() {
    this.isSavingProfile = true;
    this.profileError = null;
    this.profileSuccess = null;
    this.render();
    try {
      const local = this.sanitizePhoneLocalPart(this.phone);
      const phoneToSave = local ? `${this.selectedCountry.dial} ${local}`.trim() : '';
      const updated = await this.api.updateProfile({
        name: this.name,
        displayName: this.displayName,
        phone: phoneToSave,
        avatarUrl: this.avatarUrl,
        profileVisible: this.profileVisibility,
        showRatings: this.reviewsPublic,
      });
      this.user = updated;
      this.profileSuccess = this.i18n.t('settings.profile.save_success');
    } catch (e: any) {
      this.profileError = e?.message || this.i18n.t('settings.profile.update_failed');
    } finally {
      this.isSavingProfile = false;
      if (this.profileNoticeTimer) clearTimeout(this.profileNoticeTimer);
      this.profileNoticeTimer = setTimeout(() => {
        this.profileError = null;
        this.profileSuccess = null;
        this.render();
      }, 6000);
      this.render();
    }
  }

  triggerAvatarSelect(input: HTMLInputElement) {
    try {
      input.click();
    } catch { }
  }

  async onAvatarFileSelected(ev: Event) {
    const input = ev.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) return;

    this.isUploadingAvatar = true;
    this.profileError = null;
    this.render();

    try {
      const prev = this.avatarUrl;
      try {
        this.avatarUrl = URL.createObjectURL(file);
      } catch {
        this.avatarUrl = prev;
      }
      this.render();

      const updated = await this.api.uploadUserAvatar(file);
      this.user = updated;
      this.avatarUrl = updated?.avatarUrl || this.avatarUrl;
    } catch (e: any) {
      this.profileError = e?.message || this.i18n.t('settings.profile.avatar_upload_failed');
    } finally {
      this.isUploadingAvatar = false;
      if (input) input.value = '';
      this.render();
    }
  }

  reloadPage() {
    try {
      window.location.reload();
    } catch { }
  }

  async handlePasswordUpdate() {
    this.passwordError = null;
    this.passwordSuccess = null;
    
    if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
      this.passwordError = this.i18n.t('settings.security.error_required') || 'All fields are required';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = this.i18n.t('settings.security.error_mismatch') || 'New passwords do not match';
      return;
    }
    if (this.newPassword.length < 6) {
      this.passwordError = this.i18n.t('settings.security.error_length') || 'Password must be at least 6 characters';
      return;
    }

    this.isChangingPassword = true;
    this.render();
    try {
      await this.api.changePassword(this.oldPassword, this.newPassword);
      this.passwordSuccess = this.i18n.t('settings.security.success') || 'Password changed successfully';
      this.oldPassword = '';
      this.newPassword = '';
      this.confirmPassword = '';
    } catch (e: any) {
      this.passwordError = e?.message || this.i18n.t('settings.security.error_failed') || 'Failed to change password. Check your current password.';
    } finally {
      this.isChangingPassword = false;
      this.render();
    }
  }

  async toggleTwoFactor() {
    if (!this.user) return;
    this.twoFactorError = null;
    this.securitySuccess = null;
    this.twoFactorLoading = true;
    this.render();
    try {
      if (this.twoFactorEnabled) return;
      const setup = await this.api.setup2FA();
      this.twoFactorQrCode = setup.qrCode;
      this.twoFactorSetupOpen = true;
      const refreshed = await this.api.getCurrentUser();
      if (refreshed) this.user = refreshed;
    } catch (e: any) {
      this.twoFactorError = e?.message || this.i18n.t('settings.security.error.twofa_update_failed');
    } finally {
      this.twoFactorLoading = false;
      this.render();
    }
  }

  handleToggle2FA(checked: boolean) {
    if (checked) {
      this.toggleTwoFactor();
      return;
    }
    if (!this.twoFactorEnabled) return;
    this.router.navigate(['/settings/security/2fa/disable'], { queryParams: { from: this.router.url } });
  }

  closeDisable2FA() {
    this.disable2FAOpen = false;
    this.isDisabling2FA = false;
    this.render();
  }

  async confirmDisable2FA() {
    if (!this.user) return;
    if (this.isDisabling2FA) return;
    this.isDisabling2FA = true;
    this.twoFactorError = null;
    this.securitySuccess = null;
    this.render();
    try {
      await this.api.disable2FA();
      this.twoFactorEnabled = false;
      this.twoFactorSetupOpen = false;
      this.twoFactorQrCode = null;
      this.twoFactorVerifyCode = '';
      const refreshed = await this.api.getCurrentUser();
      if (refreshed) this.user = refreshed;
      this.securitySuccess = this.i18n.t('settings.security.2fa_disabled_success');
      this.disable2FAOpen = false;
    } catch (e: any) {
      this.twoFactorError = e?.message || this.i18n.t('settings.security.2fa_disable_failed');
    } finally {
      this.isDisabling2FA = false;
      this.render();
    }
  }

  onTwoFactorVerifyCodeChange(v: string) {
    const cleaned = String(v || '').replace(/\D/g, '').slice(0, 6);
    this.twoFactorVerifyCode = cleaned;
    this.render();
    if (cleaned.length === 6 && !this.twoFactorLoading) {
      this.verifyTwoFactorSetup();
    }
  }

  async verifyTwoFactorSetup() {
    if (!this.twoFactorVerifyCode.trim()) return;
    this.twoFactorError = null;
    this.securitySuccess = null;
    this.twoFactorLoading = true;
    this.render();
    try {
      await this.api.verify2FASetup(this.twoFactorVerifyCode.trim());
      this.twoFactorEnabled = true;
      this.twoFactorSetupOpen = false;
      this.twoFactorVerifyCode = '';
      const refreshed = await this.api.getCurrentUser();
      if (refreshed) this.user = refreshed;
      this.securitySuccess = this.i18n.t('settings.security.2fa_enabled_success');
    } catch (e: any) {
      this.twoFactorError = e?.message || this.i18n.t('settings.security.error.invalid_code');
    } finally {
      this.twoFactorLoading = false;
      this.render();
    }
  }

  async submitVerificationRequest() {
    if (!this.user) return;
    if (this.verificationSubmitting) return;
    if (!this.verificationAddress || !this.verificationPhone) {
      this.verificationError = this.i18n.t('settings.security.error.address_phone_required');
      this.render();
      return;
    }
    this.verificationSubmitting = true;
    this.verificationError = null;
    this.verificationSuccess = null;
    this.render();
    try {
      const updated = await this.api.requestVerification({ address: this.verificationAddress, phone: this.verificationPhone });
      this.user = updated;
      this.verificationSuccess = this.i18n.t('settings.security.success.verification_submitted');
    } catch (e: any) {
      this.verificationError = e?.message || this.i18n.t('settings.security.error.verification_failed');
    } finally {
      this.verificationSubmitting = false;
      this.render();
    }
  }

  async loadDevices() {
    if (!this.user) return;
    this.isLoadingDevices = true;
    this.devicesError = null;
    this.render();
    try {
      const list = await this.api.getDevices();
      this.devices = Array.isArray(list) ? list : [];
    } catch (e: any) {
      this.devices = [];
      this.devicesError = e?.message || this.i18n.t('settings.security.error.devices_load_failed');
    } finally {
      this.isLoadingDevices = false;
      this.render();
    }
  }

  openRevokeDevice(device: any) {
    const id = String(device?.id || '').trim();
    if (!id) return;
    this.router.navigate(['/settings/security/device', id, 'revoke'], { queryParams: { from: this.router.url }, state: { device } as any });
  }

  closeRevokeDevice() {
    this.revokeDeviceOpen = false;
    this.revokeDeviceLoading = false;
    this.revokeDeviceTarget = null;
    this.render();
  }

  async confirmRevokeDevice() {
    const id = String(this.revokeDeviceTarget?.id || '');
    if (!id) return;
    if (this.revokeDeviceLoading) return;
    this.revokeDeviceLoading = true;
    this.devicesError = null;
    this.devicesSuccess = null;
    this.render();
    try {
      await this.api.revokeDevice(id);
      this.closeRevokeDevice();
      await this.loadDevices();
      this.devicesSuccess = this.i18n.t('settings.security.revoke_success') || 'Device revoked successfully.';
    } catch {
      this.devicesError = this.i18n.t('settings.security.revoke_failed') || 'Failed to revoke device.';
      this.revokeDeviceLoading = false;
      this.render();
    }
  }

  async loadSubscription() {
    if (!this.user) return;
    this.isLoadingSubscription = true;
    this.cancelError = null;
    this.render();
    try {
      this.subscription = await this.api.getCurrentSubscription();
    } catch {
      this.subscription = null;
    } finally {
      this.isLoadingSubscription = false;
      this.render();
    }
  }

  async loadInvoices() {
    if (!this.user) return;
    this.isLoadingInvoices = true;
    this.render();
    try {
      const inv = await this.api.getSubscriptionInvoices();
      this.invoices = Array.isArray(inv) ? inv : [];
    } catch {
      this.invoices = [];
    } finally {
      this.isLoadingInvoices = false;
      this.render();
    }
  }

  openCancelSubscription() {
    this.router.navigate(['/settings/subscription/cancel'], { queryParams: { from: this.router.url } });
  }

  closeCancelSubscription() {
    this.cancelConfirmOpen = false;
    this.cancelConfirmLoading = false;
    this.render();
  }

  async confirmCancelSubscription() {
    if (this.cancelConfirmLoading) return;
    this.cancelConfirmLoading = true;
    this.cancelError = null;
    this.render();
    try {
      await this.api.cancelSubscription();
      this.cancelConfirmOpen = false;
      await this.loadSubscription();
    } catch (e: any) {
      this.cancelError = e?.message || this.i18n.t('settings.subscription.cancel_modal_error');
    } finally {
      this.cancelConfirmLoading = false;
      this.render();
    }
  }

  openInvoice(url: string) {
    if (!url) return;
    window.open(url, '_blank');
  }

  exportData() {
    try {
      const blob = new Blob([JSON.stringify({ user: this.user, subscription: this.subscription, invoices: this.invoices }, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'vicinity24-export.json';
      a.click();
      URL.revokeObjectURL(url);
    } catch { }
  }

  openDeleteAccount() {
    this.deleteAccountError = null;
    this.router.navigate(['/settings/account/delete'], { queryParams: { from: this.router.url } });
  }

  closeDeleteAccount() {
    this.deleteAccountOpen = false;
    this.deleteAccountLoading = false;
    this.deleteAccountError = null;
    this.render();
  }

  async confirmDeleteAccount() {
    if (this.deleteAccountLoading) return;
    this.deleteAccountLoading = true;
    this.deleteAccountError = null;
    this.render();
    try {
      await this.api.deleteMyAccount();
      this.closeDeleteAccount();
      this.router.navigate(['/connect']);
    } catch (e: any) {
      this.deleteAccountError = e?.message || this.i18n.t('settings.privacy.error.delete_failed');
    } finally {
      this.deleteAccountLoading = false;
      this.render();
    }
  }

  upgradeToPremium() {
    if (!this.settingsConfig.isSectionEnabled('enable', 'subscription')) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.router.navigate(['/subscription/upgrade'], { queryParams: { plan: 'premium' } });
  }

  upgradeToVerified() {
    if (!this.settingsConfig.isSectionEnabled('enable', 'subscription')) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.router.navigate(['/subscription/upgrade'], { queryParams: { plan: 'verified' } });
  }
}
