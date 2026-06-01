import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Shield, Mail, Lock, User as UserIcon, ArrowRight, UserCheck, Sparkles, Key, AlertCircle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { SessionService } from '../../core/services/session.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';
import { SubscriptionFeatureService } from '../../core/services/subscription-feature.service';
import { ButtonComponent } from '../../shared/components/button/button';
import { PasswordRecoveryComponent } from '../../shared/components/password-recovery/password-recovery';

@Component({
  selector: 'app-connect',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule, ButtonComponent, PasswordRecoveryComponent],
  templateUrl: './connect.component.html',
  styleUrl: './connect.component.css'
})
export class ConnectComponent implements OnInit {
  isLogin = true;
  isLoading = false;
  name = '';
  email = '';
  password = '';
  acceptedTerms = false;
  showPasswordRecovery = false;
  connectConfig: any = {};
  error: string | null = null;
  demoNotice: { type: 'success' | 'error'; message: string } | null = null;
  private demoNoticeTimer: any = null;

  api = inject(ApiService);
  i18n = inject(I18nService);
  authStorage = inject(AuthStorageService);
  session = inject(SessionService);
  settingsConfig = inject(SettingsConfigService);
  subscriptionFeature = inject(SubscriptionFeatureService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  readonly Shield = Shield;
  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly UserIcon = UserIcon;
  readonly ArrowRight = ArrowRight;
  readonly UserCheck = UserCheck;
  readonly Sparkles = Sparkles;
  readonly Key = Key;
  readonly AlertCircle = AlertCircle;

  ngOnInit() {
    this.api.getPublicConfig().then(cfg => {
      if (cfg.connect) this.connectConfig = cfg.connect;
      this.render();
    }).catch(() => { });
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  show(key: string): boolean {
    return this.connectConfig[key] !== false;
  }

  async handleSeedData() {
    this.isLoading = true;
    this.error = null;
    this.demoNotice = null;
    try {
      const res = await this.api.seedData();
      this.demoNotice = { type: 'success', message: String(res || this.i18n.t('connect.demo.seed_success')) };
      setTimeout(() => {
        this.demoNotice = null;
        this.render();
      }, 5000);
    } catch (e: any) {
      console.error(e);
      this.error = e?.message || 'Failed to seed data';
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  async handleDemoLogin(userId: string) {
    this.isLoading = true;
    this.render();
    try {
      // Map user_admin -> admin@nearshare.local
      // user_unverified_demo -> new.neighbor@example.com
      // user_lender -> linda.lender@example.com
      // user_borrower -> bob.borrower@example.com
      let demoEmail = `${userId}@example.com`;
      if (userId === 'user_admin') demoEmail = 'admin@nearshare.local';
      if (userId === 'user_unverified_demo') demoEmail = 'new.neighbor@example.com';
      if (userId === 'user_lender') demoEmail = 'linda.lender@example.com';
      if (userId === 'user_borrower') demoEmail = 'bob.borrower@example.com';

      const data = await this.api.loginWithEmail(demoEmail, 'password123');
      if (data?.token) this.authStorage.setToken(data.token);
      if (data?.user?.id) this.authStorage.setUserId(data.user.id);
      await this.session.refresh();
      this.isLoading = false;
      this.render();
      this.router.navigate(['/dashboard']);
    } catch (e) {
      console.error(e);
      this.isLoading = false;
      this.render();
    }
  }

  async handleSubmit() {
    this.isLoading = true;
    this.error = null;
    this.render();
    try {
      await this.settingsConfig.ensureLoaded();
      if (this.isLogin) {
        const data = await this.api.loginWithEmail(this.email, this.password);
        if (data?.token) this.authStorage.setToken(data.token);
        if (data?.user?.id) this.authStorage.setUserId(data.user.id);
        this.authStorage.setAuthContext('user');
        await this.session.refresh();
        this.isLoading = false;
        this.render();
        this.router.navigate(['/dashboard']);
      } else {
        const data = await this.api.registerUser(this.name, this.email, this.password);
        if (data?.requiresEmailVerification) {
          const token = String(data?.verificationToken || '');
          const email = String(data?.user?.email || this.email || '');
          this.isLoading = false;
          this.render();
          this.router.navigate(['/verification/email'], { queryParams: { flow: 'signup', token, email } });
          return;
        }
        if (data?.token) this.authStorage.setToken(data.token);
        if (data?.user?.id) this.authStorage.setUserId(data.user.id);
        this.authStorage.setAuthContext('user');
        await this.session.refresh();
        await this.settingsConfig.ensureLoaded();
        this.isLoading = false;
        this.render();
        this.router.navigate([this.subscriptionFeature.enabled() ? '/subscription' : '/dashboard']);
      }
    } catch (err: any) {
      console.error('Auth error', err);
      const rawApiError = err?.error?.error ?? err?.error;
      const apiError = String(rawApiError || '').trim().toLowerCase();
      const subscriptionEnabled = this.subscriptionFeature.enabled();
      if (this.isLogin && apiError.includes('email_not_verified') && !subscriptionEnabled) {
        try {
          const started = await this.api.startEmailVerification(this.email, this.i18n.language());
          const token = String(started?.token || '');
          this.isLoading = false;
          this.render();
          this.router.navigate(['/verification/email'], { queryParams: { flow: 'signup', token, email: this.email } });
          return;
        } catch {
          this.error = this.i18n.t('settings.security.email_not_verified') || 'Email not verified';
        }
      }
      if (err.code === 'MFA_REQUIRED') {
        this.isLoading = false;
        this.render();
        this.router.navigate(['/connect/mfa'], { state: { context: 'user', token: err.token, returnTo: '/dashboard', cancelTo: '/connect' } as any });
        return;
      }
      if (apiError) {
        const tKey =
          apiError === 'email_exists' ? 'connect.error.email_exists' :
          apiError === 'invalid_credentials' ? 'connect.error.invalid_credentials' :
          apiError === 'user_not_found' ? 'connect.error.user_not_found' :
          apiError === 'invalid_email' ? 'connect.error.invalid_email' :
          apiError === 'password_required' ? 'connect.error.password_required' :
          apiError === 'email_required' ? 'connect.error.email_required' :
          '';
        this.error = (tKey ? this.i18n.t(tKey) : '') || String(rawApiError);
      } else {
        this.error = err?.message || this.i18n.t('connect.mfa.error') || 'Something went wrong';
      }
      this.isLoading = false;
      this.render();
    }
  }

  toggleMode(isLoginMode: boolean) {
    this.isLogin = isLoginMode;
    this.acceptedTerms = false;
    this.render();
  }

  closePasswordRecovery() {
    this.showPasswordRecovery = false;
    this.render();
  }

  onPasswordRecoverySuccess() {
    this.showPasswordRecovery = false;
    this.isLogin = true;
    this.password = '';
    this.error = this.i18n.t('connect.recovery.success_login_notice');
    this.render();
  }
}
