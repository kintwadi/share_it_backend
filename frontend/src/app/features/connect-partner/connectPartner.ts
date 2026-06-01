import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule, Building2, Mail, Lock, User as UserIcon, ArrowRight, AlertCircle, Phone, MapPin } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-connect-partner',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './connectPartner.html',
  styleUrl: './connectPartner.css'
})
export class ConnectPartnerComponent implements OnInit {
  api = inject(ApiService);
  authStorage = inject(AuthStorageService);
  session = inject(SessionService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  readonly Building2 = Building2;
  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly UserIcon = UserIcon;
  readonly ArrowRight = ArrowRight;
  readonly AlertCircle = AlertCircle;
  readonly Phone = Phone;
  readonly MapPin = MapPin;

  isLogin = true;
  isLoading = false;
  error: string | null = null;

  userName = '';
  email = '';
  password = '';

  partnerName = '';
  partnerEmail = '';
  partnerPassword = '';
  partnerPhone = '';
  partnerAddress = '';
  partnerCity = '';
  contactPerson = '';

  async ngOnInit() {
    try {
      if (this.authStorage.getAuthContext() === 'partner' && this.authStorage.getToken()) {
        await this.session.refresh();
        if (this.session.user()) {
          this.router.navigate(['/partner/fill-request']);
          return;
        }
      }
    } catch { }
    this.render();
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  toggleMode(isLoginMode: boolean) {
    this.isLogin = isLoginMode;
    this.error = null;
    if (!isLoginMode) {
      this.userName = '';
      this.email = '';
      this.password = '';
    }
    if (isLoginMode) {
      this.partnerPassword = '';
    }
    this.render();
  }

  async handleSubmit() {
    this.isLoading = true;
    this.error = null;
    this.render();
    try {
      if (this.isLogin) {
        const data = await this.api.loginPartner(this.email, this.password);
        if (data?.token) this.authStorage.setToken(data.token);
        if (data?.user?.id) this.authStorage.setUserId(data.user.id);
        this.authStorage.setAuthContext('partner');
        await this.session.refresh();
        this.isLoading = false;
        this.render();
        this.router.navigate(['/partner/fill-request']);
        return;
      }

      const data = await this.api.registerPartner({
        partner: {
          name: this.partnerName,
          email: this.partnerEmail,
          phone: this.partnerPhone,
          address: this.partnerAddress,
          city: this.partnerCity,
          contactPerson: this.contactPerson
        },
        partnerPassword: this.partnerPassword
      });
      if (data?.token) this.authStorage.setToken(data.token);
      if (data?.user?.id) this.authStorage.setUserId(data.user.id);
      this.authStorage.setAuthContext('partner');
      await this.session.refresh();
      this.isLoading = false;
      this.render();
      this.router.navigate(['/partner/fill-request']);
    } catch (err: any) {
      if (err?.code === 'MFA_REQUIRED') {
        this.isLoading = false;
        this.render();
        this.router.navigate(['/connect/mfa'], { state: { context: 'partner', token: err.token, returnTo: '/partner/fill-request', cancelTo: '/connect/partner' } as any });
        return;
      }
      this.error = err?.message || 'Something went wrong';
      this.isLoading = false;
      this.render();
    }
  }
}
