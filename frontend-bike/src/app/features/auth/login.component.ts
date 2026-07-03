import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TPipe],
  templateUrl: './login.component.html',
  styleUrl: './auth-shell.css'
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  name = '';
  email = 'linda.lender@example.com';
  password = 'password123';
  phone = '';
  address = '';
  loading = false;
  successKey = '';
  errorKey = '';

  get isSignupMode(): boolean {
    return this.router.url.split('?')[0] === '/signup';
  }

  ngOnInit(): void {
    const email = this.route.snapshot.queryParamMap.get('email');
    if (email) {
      this.email = email;
    }

    if (this.route.snapshot.queryParamMap.get('registered')) {
      this.successKey = 'auth.accountCreated';
      this.password = '';
    }
  }

  async setMode(mode: 'login' | 'signup'): Promise<void> {
    const target = mode === 'signup' ? '/signup' : '/login';
    if (this.router.url.split('?')[0] === target) {
      return;
    }

    this.errorKey = '';
    this.successKey = '';
    await this.router.navigate([target], { queryParamsHandling: 'merge' });
  }

  async submit(): Promise<void> {
    this.loading = true;
    this.errorKey = '';
    this.successKey = '';

    try {
      if (this.isSignupMode) {
        await this.auth.register({
          name: this.name,
          email: this.email,
          password: this.password,
          phone: this.phone,
          address: this.address
        });

        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        await this.router.navigate(['/login'], {
          queryParams: {
            ...(redirect ? { redirect } : {}),
            registered: '1',
            email: this.email
          }
        });
        return;
      }

      const response = await this.auth.login({ email: this.email, password: this.password });
      if (response.mfaRequired) {
        this.errorKey = 'auth.mfaUnsupported';
        return;
      }

      const redirect = this.route.snapshot.queryParamMap.get('redirect') || '/bikes';
      await this.router.navigateByUrl(redirect);
    } catch {
      this.errorKey = this.isSignupMode ? 'auth.createAccountError' : 'auth.signInError';
    } finally {
      this.loading = false;
    }
  }
}
