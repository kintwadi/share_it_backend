import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule, Shield, Mail, Lock, User as UserIcon, Key, ArrowRight, AlertCircle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-connect-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './connectAdmin.html',
  styleUrl: './connectAdmin.css'
})
export class ConnectAdminComponent implements OnInit {
  api = inject(ApiService);
  authStorage = inject(AuthStorageService);
  session = inject(SessionService);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);

  readonly Shield = Shield;
  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly UserIcon = UserIcon;
  readonly Key = Key;
  readonly ArrowRight = ArrowRight;
  readonly AlertCircle = AlertCircle;

  isLogin = true;
  isLoading = false;
  error: string | null = null;

  name = '';
  email = '';
  password = '';
  signupSecret = '';
  adminScope: 'FULL' | 'PARTNER' = 'FULL';

  async ngOnInit() {
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
    this.render();
  }

  async handleSubmit() {
    this.isLoading = true;
    this.error = null;
    this.render();
    try {
      if (this.isLogin) {
        const data = await this.api.loginAdmin(this.email, this.password);
        if (data?.token) this.authStorage.setToken(data.token);
        if (data?.user?.id) this.authStorage.setUserId(data.user.id);
        this.authStorage.setAuthContext('admin');
        await this.session.refresh();
        this.isLoading = false;
        this.render();
        this.router.navigate(['/admin']);
        return;
      }

      const data = await this.api.registerAdmin(this.name, this.email, this.password, this.signupSecret, this.adminScope);
      if (data?.token) this.authStorage.setToken(data.token);
      if (data?.user?.id) this.authStorage.setUserId(data.user.id);
      this.authStorage.setAuthContext('admin');
      await this.session.refresh();
      this.isLoading = false;
      this.render();
      this.router.navigate(['/admin']);
    } catch (err: any) {
      if (err?.code === 'MFA_REQUIRED') {
        this.isLoading = false;
        this.render();
        this.router.navigate(['/connect/mfa'], { state: { context: 'admin', token: err.token, returnTo: '/admin', cancelTo: '/connect/admin' } as any });
        return;
      }
      this.error = err?.message || 'Something went wrong';
      this.isLoading = false;
      this.render();
    }
  }
}
