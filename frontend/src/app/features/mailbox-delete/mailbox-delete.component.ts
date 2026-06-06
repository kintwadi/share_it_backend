import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Trash2, AlertTriangle } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-mailbox-delete',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './mailbox-delete.component.html',
  styleUrl: './mailbox-delete.component.css'
})
export class MailboxDeleteComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly TrashIcon = Trash2;
  readonly AlertTriangle = AlertTriangle;

  messageId = '';
  deleting = false;
  error: string | null = null;
  private returnTo = '/mailbox';

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  ngOnInit() {
    this.messageId = String(this.route.snapshot.paramMap.get('id') || '').trim();
    const st: any = history.state || {};
    this.returnTo = typeof st.returnTo === 'string' ? st.returnTo : '/mailbox';
    if (!this.messageId) {
      this.router.navigate([this.returnTo]);
    }
  }

  back() {
    this.router.navigate([this.returnTo]);
  }

  async confirmDelete() {
    if (!this.messageId) return;
    if (this.deleting) return;
    this.deleting = true;
    this.error = null;
    this.render();
    try {
      await this.api.deleteMessage(this.messageId);
      this.router.navigate([this.returnTo], { state: { deletedMessageId: this.messageId } as any });
    } catch (e: any) {
      this.error = e?.message || this.i18n.t('mailbox_delete.error_failed');
    } finally {
      this.deleting = false;
      this.render();
    }
  }
}

