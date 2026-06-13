import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-password-recovery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './password-recovery.component.html',
  styleUrl: './auth-shell.css'
})
export class PasswordRecoveryComponent {
  private readonly auth = inject(AuthService);

  step = 1;
  email = '';
  code = '';
  newPassword = '';
  token = '';
  loading = false;
  success = '';
  error = '';

  async sendCode(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';
    try {
      await this.auth.forgotPassword(this.email);
      this.success = 'Recovery code sent. Check the mailbox linked to your bike account.';
      this.step = 2;
    } catch {
      this.error = 'Unable to send a recovery code right now.';
    } finally {
      this.loading = false;
    }
  }

  async verifyCode(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';
    try {
      this.token = await this.auth.verifyResetCode(this.email, this.code);
      this.success = 'Code verified. You can now set a new password.';
      this.step = 3;
    } catch {
      this.error = 'The recovery code is invalid or expired.';
    } finally {
      this.loading = false;
    }
  }

  async resetPassword(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';
    try {
      await this.auth.resetPassword(this.token, this.newPassword);
      this.success = 'Password updated successfully. You can sign in now.';
      this.step = 4;
    } catch {
      this.error = 'Unable to reset the password with the provided token.';
    } finally {
      this.loading = false;
    }
  }
}
