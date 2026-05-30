import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Shield, ArrowLeft, Loader2, AlertCircle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { SessionService } from '../../core/services/session.service';
import { I18nService } from '../../core/services/i18n.service';
import { ButtonComponent } from '../../shared/components/button/button';

type MfaContext = 'user' | 'admin' | 'partner';

@Component({
  selector: 'app-connect-mfa',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ButtonComponent],
  templateUrl: './connect-mfa.component.html',
  styleUrl: './connect-mfa.component.css'
})
export class ConnectMfaComponent implements OnInit {
  private api = inject(ApiService);
  private authStorage = inject(AuthStorageService);
  private session = inject(SessionService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly Shield = Shield;
  readonly ArrowLeft = ArrowLeft;
  readonly Loader2 = Loader2;
  readonly AlertCircle = AlertCircle;

  code = '';
  loading = false;
  error: string | null = null;

  private context: MfaContext = 'user';
  private tempToken: string | null = null;
  private returnTo = '/dashboard';
  private cancelTo = '/connect';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    const st: any = history.state || {};
    const ctx = String(st.context || 'user').toLowerCase();
    this.context = (ctx === 'admin' || ctx === 'partner' || ctx === 'user') ? (ctx as MfaContext) : 'user';
    this.tempToken = typeof st.token === 'string' ? st.token : null;
    this.returnTo = typeof st.returnTo === 'string' ? st.returnTo : (this.context === 'admin' ? '/admin' : '/dashboard');
    this.cancelTo = typeof st.cancelTo === 'string' ? st.cancelTo : (this.context === 'admin' ? '/connect/admin' : (this.context === 'partner' ? '/connect/partner' : '/connect'));

    if (!this.tempToken) {
      this.router.navigate([this.cancelTo]);
      return;
    }
  }

  onCodeChange(v: string) {
    this.code = String(v || '').replace(/[^0-9]/g, '').slice(0, 6);
    this.render();
  }

  cancel() {
    this.router.navigate([this.cancelTo]);
  }

  async submit() {
    if (!this.tempToken) return;
    if (this.loading) return;
    if (this.code.length !== 6) return;

    this.loading = true;
    this.error = null;
    this.render();
    try {
      const data = this.context === 'admin'
        ? await this.api.verify2FALoginAdmin(this.code, this.tempToken)
        : (this.context === 'partner'
          ? await this.api.verify2FALoginPartner(this.code, this.tempToken)
          : await this.api.verify2FALogin(this.code, this.tempToken));

      if (data?.token) this.authStorage.setToken(data.token);
      if (data?.user?.id) this.authStorage.setUserId(data.user.id);
      this.authStorage.setAuthContext(this.context);
      await this.session.refresh();
      this.router.navigate([this.returnTo]);
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('connect.mfa.invalid') || 'Invalid code';
    } finally {
      this.loading = false;
      this.render();
    }
  }
}

