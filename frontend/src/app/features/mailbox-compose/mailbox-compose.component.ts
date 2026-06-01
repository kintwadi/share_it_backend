import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Send, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-mailbox-compose',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './mailbox-compose.component.html',
  styleUrl: './mailbox-compose.component.css'
})
export class MailboxComposeComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly SendIcon = Send;
  readonly AlertTriangle = AlertTriangle;

  receiver = '';
  content = '';
  error: string | null = null;
  sending = false;

  private returnTo = '/mailbox';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    const qp = this.route.snapshot.queryParamMap;
    const receiverEmail = String(qp.get('receiverEmail') || '').trim();
    const receiverId = String(qp.get('receiverId') || '').trim();
    const rawReceiver = String(qp.get('receiver') || '').trim();
    this.receiver = receiverEmail || receiverId || rawReceiver;

    const st: any = history.state || {};
    this.returnTo = typeof st.returnTo === 'string' ? st.returnTo : '/mailbox';
  }

  back() {
    this.router.navigate([this.returnTo]);
  }

  async send() {
    const receiver = this.receiver.trim();
    const content = this.content.trim();
    if (!receiver || !content) {
      this.error = 'Please fill out all fields';
      this.render();
      return;
    }

    const isEmail = receiver.includes('@');
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(receiver);
    if (!isEmail && !isUuid) {
      this.error = 'Enter a valid recipient email';
      this.render();
      return;
    }

    if (this.sending) return;
    this.sending = true;
    this.error = null;
    this.render();
    try {
      await this.api.sendMessage(receiver, content);
      this.router.navigate([this.returnTo], { state: { composeSuccess: true } as any });
    } catch (e: any) {
      this.error = e?.message || 'Failed to send message';
      this.render();
    } finally {
      this.sending = false;
      this.render();
    }
  }
}

