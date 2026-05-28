import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Inbox, Send, Trash2, Edit } from 'lucide-angular';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { Message } from '../../core/models/types';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-mailbox',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './mailbox.component.html'
})
export class MailboxComponent implements OnInit {
  api = inject(ApiService);
  i18n = inject(I18nService);
  route = inject(ActivatedRoute);
  router = inject(Router);

  readonly InboxIcon = Inbox;
  readonly SendIcon = Send;
  readonly TrashIcon = Trash2;
  readonly EditIcon = Edit;

  currentTab = signal<'inbox' | 'outbox'>('inbox');
  inboxMessages = signal<Message[]>([]);
  outboxMessages = signal<Message[]>([]);
  isLoading = signal(true);
  selectedMessageId = signal<string | null>(null);
  filterText = signal('');
  
  composeNotice = signal<string | null>(null);

  messages = computed(() => {
    const list = this.currentTab() === 'inbox' ? this.inboxMessages() : this.outboxMessages();
    const q = this.filterText().trim().toLowerCase();
    if (!q) return list;
    return list.filter(m => {
      const peer = this.currentTab() === 'inbox' ? (m.senderEmail || m.senderId) : (m.receiverEmail || m.receiverId);
      const content = m.content || '';
      return String(peer).toLowerCase().includes(q) || String(content).toLowerCase().includes(q);
    });
  });

  selectedMessage = computed(() => {
    const id = this.selectedMessageId();
    if (!id) return null;
    return this.messages().find(m => m.id === id) || null;
  });

  selectedPeerLabel = computed(() => {
    const msg = this.selectedMessage();
    if (!msg) return '';
    return this.currentTab() === 'inbox'
      ? String(msg.senderEmail || msg.senderId)
      : String(msg.receiverEmail || msg.receiverId);
  });

  selectedTitle = computed(() => {
    const msg = this.selectedMessage();
    if (!msg) return '';
    const firstLine = String(msg.content || '').split('\n')[0] || '';
    const t = firstLine.trim();
    return t.length > 0 ? t.slice(0, 80) : '(no subject)';
  });

  ngOnInit() {
    this.loadMessages();
    const receiverEmail = String(this.route.snapshot.queryParamMap.get('receiverEmail') || '').trim();
    const receiverId = String(this.route.snapshot.queryParamMap.get('receiverId') || '').trim();
    if (receiverEmail || receiverId) {
      this.composeTo(receiverEmail || receiverId);
      return;
    }
    const st: any = history.state || {};
    if (st.composeSuccess) {
      this.composeNotice.set('Message sent');
      setTimeout(() => this.composeNotice.set(null), 3000);
    }
  }

  async loadMessages() {
    this.isLoading.set(true);
    try {
      const [inbox, outbox] = await Promise.all([
        this.api.getInbox(),
        this.api.getOutbox()
      ]);
      this.inboxMessages.set(inbox);
      this.outboxMessages.set(outbox);
      const currentId = this.selectedMessageId();
      if (currentId) {
        const stillExists = [...inbox, ...outbox].some(m => m.id === currentId);
        if (!stillExists) this.selectedMessageId.set(null);
      }
    } catch (e) {
      console.error('Failed to load mailbox', e);
    } finally {
      this.isLoading.set(false);
    }
  }

  setTab(tab: 'inbox' | 'outbox') {
    this.currentTab.set(tab);
    this.selectedMessageId.set(null);
  }

  selectMessage(msg: Message) {
    this.selectedMessageId.set(msg.id);
  }

  composeTo(receiver?: string) {
    const qp: any = {};
    const v = String(receiver || '').trim();
    if (v) {
      if (v.includes('@')) qp.receiverEmail = v;
      else qp.receiverId = v;
    }
    this.selectedMessageId.set(null);
    this.router.navigate(['/mailbox/compose'], { queryParams: qp, state: { returnTo: '/mailbox' } as any });
  }

  replyToSelected() {
    const msg = this.selectedMessage();
    if (!msg) return;
    if (this.currentTab() !== 'inbox') return;
    const receiver = String(msg.senderEmail || msg.senderId || '').trim();
    if (!receiver) return;
    this.composeTo(receiver);
  }

  confirmDelete(id: string) {
    if (!id) return;
    this.router.navigate(['/mailbox/delete', id], { state: { returnTo: '/mailbox' } as any });
  }
}
