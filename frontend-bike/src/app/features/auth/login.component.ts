import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './auth-shell.css'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  email = 'linda.lender@example.com';
  password = 'password123';
  loading = false;
  error = '';

  async submit(): Promise<void> {
    this.loading = true;
    this.error = '';
    try {
      const response = await this.auth.login({
        email: this.email,
        password: this.password
      });
      if (response.mfaRequired) {
        this.error = 'This dedicated bike frontend does not yet handle 2FA login verification.';
        return;
      }
      const redirect = this.route.snapshot.queryParamMap.get('redirect') || '/bikes';
      await this.router.navigateByUrl(redirect);
    } catch {
      this.error = 'Unable to sign you in with those credentials.';
    } finally {
      this.loading = false;
    }
  }
}
