import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Inbox, Send, Trash2, Edit } from 'lucide-angular';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { Message } from '../../core/models/types';
import { SessionService } from '../../core/services/session.service';
import { I18nService } from '../../core/services/i18n.service';
import { ConfirmationModalComponent } from '../../shared/components/confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-mailbox',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ConfirmationModalComponent],
  templateUrl: './mailbox.component.html'
})
export class MailboxComponent implements OnInit {
  api = inject(ApiService);
  session = inject(SessionService);
  i18n = inject(I18nService);
  route = inject(ActivatedRoute);

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
  
  showComposeModal = signal(false);
  composeReceiverId = signal('');
  composeContent = signal('');
  composeError = signal<string | null>(null);

  deleteConfirmOpen = signal(false);
  deleteTargetId = signal<string | null>(null);
  deletingMessage = signal(false);

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
    if (receiverEmail) {
      this.openCompose(receiverEmail);
      return;
    }
    const receiverId = String(this.route.snapshot.queryParamMap.get('receiverId') || '').trim();
    if (receiverId) {
      this.openCompose(receiverId);
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

  openDeleteConfirm(id: string) {
    this.deleteTargetId.set(id);
    this.deleteConfirmOpen.set(true);
  }

  closeDeleteConfirm() {
    this.deleteConfirmOpen.set(false);
    this.deleteTargetId.set(null);
    this.deletingMessage.set(false);
  }

  async confirmDeleteMessage() {
    const id = this.deleteTargetId();
    if (!id) return;
    if (this.deletingMessage()) return;
    this.deletingMessage.set(true);
    try {
      await this.api.deleteMessage(id);
      this.closeDeleteConfirm();
      await this.loadMessages();
    } catch (e) {
      console.error(e);
      this.deletingMessage.set(false);
    }
  }

  openCompose(receiverId?: string) {
    this.composeReceiverId.set(receiverId || '');
    this.composeContent.set('');
    this.composeError.set(null);
    this.showComposeModal.set(true);
  }

  replyToSelected() {
    const msg = this.selectedMessage();
    if (!msg) return;
    if (this.currentTab() !== 'inbox') return;
    const receiver = String(msg.senderEmail || msg.senderId || '').trim();
    if (!receiver) return;
    this.openCompose(receiver);
  }

  closeCompose() {
    this.showComposeModal.set(false);
  }

  async sendMessage() {
    const receiver = this.composeReceiverId().trim();
    const content = this.composeContent().trim();
    if (!receiver || !content) {
      this.composeError.set('Please fill out all fields');
      return;
    }

    const isEmail = receiver.includes('@');
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(receiver);
    if (!isEmail && !isUuid) {
      this.composeError.set('Enter a valid recipient email');
      return;
    }
    
    try {
      this.composeError.set(null);
      await this.api.sendMessage(receiver, content);
      this.closeCompose();
      await this.loadMessages();
    } catch (e) {
      const msg = (e as any)?.message || 'Failed to send message';
      this.composeError.set(msg);
      console.error(e);
    }
  }
}
