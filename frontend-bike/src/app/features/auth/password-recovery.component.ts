import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../core/i18n/t.pipe';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-password-recovery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TPipe],
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
  successKey = '';
  errorKey = '';

  async sendCode(): Promise<void> {
    this.loading = true;
    this.errorKey = '';
    this.successKey = '';
    try {
      await this.auth.forgotPassword(this.email);
      this.successKey = 'recovery.codeSent';
      this.step = 2;
    } catch {
      this.errorKey = 'recovery.sendCodeError';
    } finally {
      this.loading = false;
    }
  }

  async verifyCode(): Promise<void> {
    this.loading = true;
    this.errorKey = '';
    this.successKey = '';
    try {
      this.token = await this.auth.verifyResetCode(this.email, this.code);
      this.successKey = 'recovery.codeVerified';
      this.step = 3;
    } catch {
      this.errorKey = 'recovery.codeInvalid';
    } finally {
      this.loading = false;
    }
  }

  async resetPassword(): Promise<void> {
    this.loading = true;
    this.errorKey = '';
    this.successKey = '';
    try {
      await this.auth.resetPassword(this.token, this.newPassword);
      this.successKey = 'recovery.passwordUpdated';
      this.step = 4;
    } catch {
      this.errorKey = 'recovery.passwordResetError';
    } finally {
      this.loading = false;
    }
  }
}
