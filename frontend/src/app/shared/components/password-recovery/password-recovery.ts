import { ChangeDetectorRef, Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Mail, Lock, ArrowRight, ChevronLeft, Shield, CheckCircle } from 'lucide-angular';
import { ApiService } from '../../../core/services/api.service';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-password-recovery',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './password-recovery.html',
  styleUrl: './password-recovery.css'
})
export class PasswordRecoveryComponent {
  @Output() backToLogin = new EventEmitter<void>();
  @Output() recoverySuccess = new EventEmitter<void>();

  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly ArrowRight = ArrowRight;
  readonly ChevronLeft = ChevronLeft;
  readonly Shield = Shield;
  readonly CheckCircle = CheckCircle;

  step: 'email' | 'code' | 'password' = 'email';
  email = '';
  code = '';
  newPassword = '';
  confirmPassword = '';
  isLoading = false;
  error: string | null = null;
  success = false;
  resetToken: string | null = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  onBackToLogin() {
    this.backToLogin.emit();
  }

  onCodeChange(val: string) {
    this.code = (val || '').replace(/[^0-9]/g, '').slice(0, 4);
  }

  async submitEmail() {
    if (!this.email) return;
    this.isLoading = true;
    this.error = null;
    this.render();
    try {
      await this.api.requestPasswordReset(this.email);
      this.step = 'code';
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('connect.recovery.error.send_failed');
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  async submitCode() {
    if (!this.email || this.code.length !== 4) return;
    this.isLoading = true;
    this.error = null;
    this.render();
    try {
      const result = await this.api.verifyResetCode(this.email, this.code);
      if (result.valid && result.token) {
        this.resetToken = result.token;
        this.step = 'password';
      } else {
        this.error = this.i18n.t('connect.recovery.error.invalid_code');
      }
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('connect.recovery.error.verify_failed');
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  async submitPassword() {
    this.isLoading = true;
    this.error = null;
    this.render();

    if (this.newPassword !== this.confirmPassword) {
      this.error = this.i18n.t('connect.recovery.error.mismatch');
      this.isLoading = false;
      this.render();
      return;
    }
    if (!this.resetToken) {
      this.error = this.i18n.t('connect.recovery.error.token_missing');
      this.isLoading = false;
      this.render();
      return;
    }

    try {
      await this.api.resetPassword(this.resetToken, this.newPassword);
      this.success = true;
      this.render();
      setTimeout(() => {
        this.recoverySuccess.emit();
        this.backToLogin.emit();
      }, 2000);
    } catch (err: any) {
      this.error = err?.message || this.i18n.t('connect.recovery.error.reset_failed');
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  useDifferentEmail() {
    this.step = 'email';
    this.code = '';
    this.resetToken = null;
    this.error = null;
    this.render();
  }

  backToCode() {
    this.step = 'code';
    this.error = null;
    this.render();
  }
}

