import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, AlertTriangle, Loader2, Shield, CreditCard, Smartphone } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

type SettingsConfirmAction =
  | 'cancel-subscription'
  | 'revoke-device'
  | 'disable-2fa'
  | 'delete-account'
  | 'remove-payment-method';

@Component({
  selector: 'app-settings-action-confirm',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './settings-action-confirm.component.html',
  styleUrl: './settings-action-confirm.component.css'
})
export class SettingsActionConfirmComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly AlertTriangle = AlertTriangle;
  readonly Loader2 = Loader2;
  readonly Shield = Shield;
  readonly CreditCard = CreditCard;
  readonly Smartphone = Smartphone;

  action: SettingsConfirmAction = 'cancel-subscription';
  backTo = '/settings';

  loading = true;
  confirming = false;
  error: string | null = null;

  title = '';
  message = '';
  confirmLabel = '';
  icon: 'warning' | 'security' | 'payment' | 'device' = 'warning';

  device: any | null = null;
  paymentMethod: any | null = null;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    const action = String(this.route.snapshot.data['action'] || '').trim() as SettingsConfirmAction;
    const from = String(this.route.snapshot.queryParamMap.get('from') || '').trim();
    this.backTo = from.startsWith('/') ? from : '/settings';
    this.action = ([
      'cancel-subscription',
      'revoke-device',
      'disable-2fa',
      'delete-account',
      'remove-payment-method'
    ] as SettingsConfirmAction[]).includes(action) ? action : 'cancel-subscription';

    this.loading = true;
    this.error = null;
    this.render();
    try {
      await this.loadDetails();
      this.configureCopy();
    } catch (e: any) {
      this.error = e?.message || 'Failed to load';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  private async loadDetails() {
    if (this.action === 'revoke-device') {
      const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
      const st: any = history.state || {};
      this.device = st?.device || null;
      if (!this.device && id) {
        try {
          const devices = await this.api.getDevices();
          this.device = Array.isArray(devices) ? devices.find((d: any) => String(d?.id) === id) : null;
        } catch {
          this.device = null;
        }
      }
    }

    if (this.action === 'remove-payment-method') {
      const id = String(this.route.snapshot.paramMap.get('id') || '').trim();
      const st: any = history.state || {};
      this.paymentMethod = st?.paymentMethod || null;
      if (!this.paymentMethod && id) {
        try {
          const methods = await this.api.getPaymentMethods();
          this.paymentMethod = Array.isArray(methods) ? methods.find((m: any) => String(m?.id) === id) : null;
        } catch {
          this.paymentMethod = null;
        }
      }
    }
  }

  private configureCopy() {
    this.confirmLabel = this.i18n.t('common.confirm');
    this.icon = 'warning';

    if (this.action === 'cancel-subscription') {
      this.title = this.i18n.t('settings.subscription.cancel_modal_title') || 'Cancel subscription?';
      this.message = this.i18n.t('settings.subscription.cancel_modal_msg') || 'This will cancel your subscription.';
      this.confirmLabel = this.i18n.t('settings.subscription.cancel_modal_cta') || this.i18n.t('common.confirm');
      this.icon = 'warning';
      return;
    }

    if (this.action === 'revoke-device') {
      const label = String(this.device?.deviceName || this.device?.userAgent || this.device?.ipAddress || this.device?.id || '').trim();
      this.title = this.i18n.t('settings.security.revoke_title') || 'Revoke device?';
      this.message = label ? `${this.i18n.t('settings.security.revoke_msg') || 'This will revoke access for:'} ${label}` : (this.i18n.t('settings.security.revoke_msg') || 'This will revoke device access.');
      this.confirmLabel = this.i18n.t('settings.security.revoke_cta') || this.i18n.t('common.confirm');
      this.icon = 'device';
      return;
    }

    if (this.action === 'disable-2fa') {
      this.title = this.i18n.t('settings.security.2fa_disable_title') || 'Disable 2FA?';
      this.message = this.i18n.t('settings.security.2fa_disable_msg') || 'Disabling 2FA reduces account security.';
      this.confirmLabel = this.i18n.t('settings.security.2fa_disable_cta') || this.i18n.t('common.confirm');
      this.icon = 'security';
      return;
    }

    if (this.action === 'delete-account') {
      this.title = this.i18n.t('settings.privacy.delete_title') || 'Delete account?';
      this.message = this.i18n.t('settings.privacy.delete_modal_msg') || 'This will permanently delete your account. This action cannot be undone.';
      this.confirmLabel = this.i18n.t('settings.privacy.delete_modal_cta') || this.i18n.t('common.confirm');
      this.icon = 'warning';
      return;
    }

    const pm = this.paymentMethod;
    const label = pm ? `${pm.brand || ''} •••• ${pm.last4 || ''}`.trim() : '';
    this.title = this.i18n.t('payments.confirm.remove_title') || 'Remove payment method?';
    this.message = label ? `${this.i18n.t('payments.confirm.remove_msg') || 'Remove'} ${label}?` : (this.i18n.t('payments.confirm.remove_msg') || 'Remove this payment method?');
    this.confirmLabel = this.i18n.t('payments.confirm.remove_cta') || this.i18n.t('common.confirm');
    this.icon = 'payment';
  }

  back() {
    this.router.navigateByUrl(this.backTo || '/settings');
  }

  async confirm() {
    if (this.confirming) return;
    this.confirming = true;
    this.error = null;
    this.render();
    try {
      if (this.action === 'cancel-subscription') {
        await this.api.cancelSubscription();
        this.router.navigateByUrl(this.backTo, { state: { noticeSuccess: this.i18n.t('settings.subscription.cancel_success') || 'Subscription canceled.' } as any });
        return;
      }

      if (this.action === 'revoke-device') {
        const id = String(this.route.snapshot.paramMap.get('id') || this.device?.id || '').trim();
        if (!id) throw new Error('Device not found');
        await this.api.revokeDevice(id);
        this.router.navigateByUrl(this.backTo, { state: { noticeSuccess: this.i18n.t('settings.security.revoke_success') || 'Device revoked.' } as any });
        return;
      }

      if (this.action === 'disable-2fa') {
        await this.api.disable2FA();
        this.router.navigateByUrl(this.backTo, { state: { noticeSuccess: this.i18n.t('settings.security.2fa_disabled_success') || '2FA disabled.' } as any });
        return;
      }

      if (this.action === 'delete-account') {
        await this.api.deleteMyAccount();
        this.router.navigate(['/connect']);
        return;
      }

      const id = String(this.route.snapshot.paramMap.get('id') || this.paymentMethod?.id || '').trim();
      if (!id) throw new Error('Payment method not found');
      await this.api.removePaymentMethod(id);
      this.router.navigateByUrl(this.backTo, { state: { noticeSuccess: this.i18n.t('payments.notice.removed') || 'Payment method removed.' } as any });
    } catch (e: any) {
      this.error = e?.message || 'Action failed';
    } finally {
      this.confirming = false;
      this.render();
    }
  }
}

