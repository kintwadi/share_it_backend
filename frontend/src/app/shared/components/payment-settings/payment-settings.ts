import { ChangeDetectorRef, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Loader2, CreditCard, Trash2, Plus, ArrowDownToLine, RefreshCcw, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../../core/services/api.service';
import { StripeClientService } from '../../../core/services/stripe-client.service';
import { ConfirmationModalComponent } from '../confirmation-modal/confirmation-modal';
import { Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-payment-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ConfirmationModalComponent],
  templateUrl: './payment-settings.html',
  styleUrl: './payment-settings.css'
})
export class PaymentSettingsComponent {
  private api = inject(ApiService);
  private stripeClient = inject(StripeClientService);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly Loader2 = Loader2;
  readonly CreditCard = CreditCard;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;
  readonly ArrowDownToLine = ArrowDownToLine;
  readonly RefreshCcw = RefreshCcw;
  readonly AlertTriangle = AlertTriangle;

  @ViewChild('cardMount') cardMount?: ElementRef<HTMLDivElement>;

  loading = true;
  error: string | null = null;

  activeTab: 'overview' | 'setup' = 'overview';

  stripe: Stripe | null = null;
  stripeAvailable = false;
  elements: StripeElements | null = null;
  card: StripeCardElement | null = null;

  paymentMethods: any[] = [];
  transactions: any[] = [];
  transactionsError: string | null = null;
  connectStatus: any | null = null;
  connectLoading = false;

  showAddForm = false;
  addProcessing = false;
  addError: string | null = null;

  confirmOpen = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmLabel = this.i18n.t('common.confirm');
  confirmLoading = false;
  confirmAction: (() => Promise<void>) | null = null;

  infoOpen = false;
  infoTitle = '';
  infoMessage = '';

  constructor() {
    this.init();
  }

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async init() {
    this.loading = true;
    this.error = null;
    this.render();
    try {
      this.stripe = await this.stripeClient.getStripe();
      this.stripeAvailable = !!this.stripe;
      await this.refreshAll();
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('payments.error.load_settings');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  async refreshAll() {
    await Promise.all([
      this.refreshPaymentMethods(),
      this.refreshTransactions(),
      this.refreshConnectStatus()
    ]);
  }

  async refreshPaymentMethods() {
    try {
      this.paymentMethods = await this.api.getPaymentMethods();
    } catch {
      this.paymentMethods = [];
    } finally {
      this.render();
    }
  }

  async refreshTransactions() {
    try {
      this.transactionsError = null;
      this.transactions = await this.api.getPaymentTransactions();
    } catch (e: any) {
      this.transactions = [];
      this.transactionsError = e?.message || this.i18n.t('payments.error.load_transactions');
    } finally {
      this.render();
    }
  }

  async refreshConnectStatus() {
    try {
      this.connectStatus = await this.api.getConnectStatus();
    } catch {
      this.connectStatus = null;
    } finally {
      this.render();
    }
  }

  setTab(tab: 'overview' | 'setup') {
    this.activeTab = tab;
    this.render();
  }

  async openOnboarding() {
    if (this.connectLoading) return;
    this.connectLoading = true;
    this.render();
    try {
      const res = await this.api.connectOnboard();
      if (res?.url) {
        window.location.href = res.url;
      }
    } finally {
      this.connectLoading = false;
      this.render();
    }
  }

  async retryReleases() {
    if (this.connectLoading) return;
    this.connectLoading = true;
    this.render();
    try {
      await this.api.retryEscrowRelease();
      await this.refreshAll();
    } finally {
      this.connectLoading = false;
      this.render();
    }
  }

  openAddCard() {
    this.addError = null;
    this.showAddForm = true;
    this.render();
    setTimeout(() => this.mountCardElement(), 0);
  }

  closeAddCard() {
    this.showAddForm = false;
    this.addProcessing = false;
    this.addError = null;
    if (this.card) {
      try {
        this.card.destroy();
      } catch { }
    }
    this.card = null;
    this.elements = null;
    this.render();
  }

  private mountCardElement() {
    if (!this.showAddForm) return;
    if (!this.stripe) return;
    if (!this.cardMount?.nativeElement) return;
    if (this.card) return;

    this.elements = this.stripe.elements();
    this.card = this.elements.create('card');
    this.card.mount(this.cardMount.nativeElement);
  }

  async submitAddCard() {
    if (this.addProcessing) return;
    if (!this.stripe || !this.card) {
      this.addError = this.i18n.t('payments.error.stripe_unavailable');
      this.render();
      return;
    }
    this.addProcessing = true;
    this.addError = null;
    this.render();
    try {
      const res = await this.stripe.createPaymentMethod({ type: 'card', card: this.card });
      if (res.error) {
        this.addError = res.error.message || this.i18n.t('payments.error.create_pm_failed');
        return;
      }
      const pmId = res.paymentMethod?.id;
      if (!pmId) {
        this.addError = this.i18n.t('payments.error.pm_id_missing');
        return;
      }
      await this.api.addPaymentMethod(pmId);
      this.closeAddCard();
      await this.refreshPaymentMethods();
    } catch (e: any) {
      this.addError = e?.message || this.i18n.t('payments.error.add_pm_failed');
    } finally {
      this.addProcessing = false;
      this.render();
    }
  }

  confirmRemove(pm: any) {
    const id = String(pm?.id || '');
    if (!id) return;
    this.confirmTitle = this.i18n.t('payments.confirm.remove_title');
    this.confirmMessage = `${this.i18n.t('payments.confirm.remove_msg')} ${pm.brand || this.i18n.t('payments.card')} •••• ${pm.last4 || ''}?`;
    this.confirmLabel = this.i18n.t('payments.confirm.remove_cta');
    this.confirmLoading = false;
    this.confirmAction = async () => {
      await this.api.removePaymentMethod(id);
      await this.refreshPaymentMethods();
    };
    this.confirmOpen = true;
    this.render();
  }

  closeConfirm() {
    this.confirmOpen = false;
    this.confirmLoading = false;
    this.confirmAction = null;
    this.render();
  }

  async doConfirm() {
    if (!this.confirmAction) return;
    this.confirmLoading = true;
    this.render();
    try {
      await this.confirmAction();
      this.closeConfirm();
    } catch (e: any) {
      this.confirmLoading = false;
      this.infoTitle = this.i18n.t('payments.modal.action_failed_title');
      this.infoMessage = e?.message || this.i18n.t('payments.modal.action_failed_msg');
      this.infoOpen = true;
      this.render();
    }
  }

  async openInvoice(tx: any) {
    const id = String(tx?.id || '');
    if (!id) return;
    try {
      const res = await this.api.getPaymentTransactionInvoiceUrl(id);
      if (res?.url) window.open(res.url, '_blank');
    } catch (e: any) {
      this.infoTitle = this.i18n.t('payments.modal.invoice_failed_title');
      this.infoMessage = e?.message || this.i18n.t('payments.error.invoice_unavailable');
      this.infoOpen = true;
      this.render();
    }
  }

  closeInfo() {
    this.infoOpen = false;
    this.infoTitle = '';
    this.infoMessage = '';
    this.render();
  }

  formatRequirements(v: any): string {
    if (!v) return this.i18n.t('payments.none');
    if (Array.isArray(v) && v.length > 0) return v.join(', ');
    return this.i18n.t('payments.none');
  }
}
