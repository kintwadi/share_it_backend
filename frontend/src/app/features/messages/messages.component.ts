import { ChangeDetectorRef, Component, OnInit, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Send, User as UserIcon, Loader2, MessageSquare, ChevronLeft, BadgeCheck, Trash2 } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { User, Message } from '../../core/models/types';
import { WsService } from '../../core/services/ws.service';
import { I18nService } from '../../core/services/i18n.service';
import { SettingsConfigService } from '../../core/services/settings-config.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.css'
})
export class MessagesComponent implements OnInit, AfterViewChecked {
  api = inject(ApiService);
  route = inject(ActivatedRoute);
  router = inject(Router);
  cdr = inject(ChangeDetectorRef);
  ws = inject(WsService);
  i18n = inject(I18nService);
  settingsConfig = inject(SettingsConfigService);

  readonly Send = Send;
  readonly UserIcon = UserIcon;
  readonly Loader2 = Loader2;
  readonly MessageSquare = MessageSquare;
  readonly ChevronLeft = ChevronLeft;
  readonly BadgeCheck = BadgeCheck;
  readonly Trash2 = Trash2;

  conversations: User[] = [];
  contacts: User[] = [];
  activeUser: User | null = null;
  messages: Message[] = [];
  
  inputText = '';
  attachError: string | null = null;
  loadingList = true;
  loadingChat = false;
  currentUserId = '';
  onlineIds = new Set<string>();
  borrowedOwnerIds = new Set<string>();

  @ViewChild('messagesEnd') messagesEndRef!: ElementRef;
  @ViewChild('fileInput') fileInputRef!: ElementRef;

  private render() {
    try {
      this.cdr.detectChanges();
    } catch { }
  }

  async ngOnInit() {
    await this.settingsConfig.ensureLoaded();
    if (!this.settingsConfig.isSectionEnabled('header', 'messages')) {
      this.router.navigate(['/']);
      return;
    }
    try {
      const user = await this.api.getCurrentUser();
      if (user) {
        this.currentUserId = user.id;
        this.ws.connect();
        this.ws.subscribeUser(user.id, (m) => {
          this.messages = [...this.messages, m];
          this.render();
        });
        this.ws.subscribePresence((upd) => {
          const next = new Set(this.onlineIds);
          if (upd.online) next.add(upd.userId); else next.delete(upd.userId);
          this.onlineIds = next;
          this.render();
        });
        this.ws.announceOnline(user.id);
        try {
          const ids = await this.api.getOnlineUserIds();
          this.onlineIds = new Set(ids);
          this.render();
        } catch { }
      }

      this.loadingList = true;
      this.render();
      const [convos, contactsList, hist] = await Promise.all([
        this.api.getConversations().catch(() => this.api.getContacts()),
        this.api.getContacts(),
        this.api.getBorrowingHistory()
      ]);

      this.conversations = convos;
      this.contacts = contactsList;
      
      const owners = new Set<string>(hist.map(h => h.listing?.ownerId).filter(id => !!id));
      this.borrowedOwnerIds = owners;

      // Handle query param
      this.route.queryParams.subscribe(params => {
        const uid = params['userId'];
        if (uid && (!this.activeUser || this.activeUser.id !== uid)) {
          const found = [...this.conversations, ...this.contacts].find(u => u.id === uid);
          if (found) this.setActiveUser(found);
        } else if (!this.activeUser && this.conversations.length > 0) {
          this.setActiveUser(this.conversations[0]);
        } else if (!this.activeUser && this.contacts.length > 0) {
          this.setActiveUser(this.contacts[0]);
        }
      });
    } catch (e) {
      console.error(e);
    } finally {
      this.loadingList = false;
      this.render();
    }
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      if (this.messagesEndRef) {
        this.messagesEndRef.nativeElement.scrollIntoView({ behavior: 'smooth' });
      }
    } catch(err) { }
  }

  async setActiveUser(user: User) {
    this.activeUser = user;
    this.loadingChat = true;
    this.render();
    try {
      this.messages = await this.api.getMessages(user.id);
    } catch (e) {
      console.error(e);
    } finally {
      this.loadingChat = false;
      this.render();
    }
  }

  get sortedUsers() {
    const all = new Map([...this.conversations, ...this.contacts].map(u => [u.id, u]));
    return Array.from(all.values()).sort((a, b) => {
      const ab = this.borrowedOwnerIds.has(a.id) ? 1 : 0;
      const bb = this.borrowedOwnerIds.has(b.id) ? 1 : 0;
      return bb - ab;
    });
  }

  async handleSend(e?: Event) {
    if (e) e.preventDefault();
    if (!this.inputText.trim() || !this.activeUser) return;
    this.attachError = null;

    const content = this.inputText;
    this.inputText = '';

    try {
      if (this.currentUserId) {
        try {
          this.ws.sendMessage(this.currentUserId, this.activeUser.id, content);
          this.messages = [...this.messages, { id: `local_${Date.now()}`, senderId: this.currentUserId, receiverId: this.activeUser.id, content, timestamp: new Date().toISOString(), isRead: false }];
        } catch {
          const newMsg = await this.api.sendMessage(this.activeUser.id, content);
          this.messages = [...this.messages, newMsg];
        }
        this.scrollToBottom();
        this.render();
      }
    } catch (err) {
      console.error("Failed to send", err);
    }
  }

  async handleAttachImage(event: any) {
    const file = event.target.files?.[0];
    if (!file || !this.activeUser) return;
    const fileError = this.validateSelectedImageFile(file);
    if (fileError) {
      this.attachError = fileError;
      this.render();
      if (this.fileInputRef) this.fileInputRef.nativeElement.value = '';
      return;
    }
    
    try {
      this.attachError = null;
      const url = await this.api.uploadListingImage(file);
      if (this.currentUserId) {
        const content = this.inputText.trim();
        try {
          this.ws.sendMessage(this.currentUserId, this.activeUser.id, content, url);
          this.messages = [...this.messages, { id: `local_img_${Date.now()}`, senderId: this.currentUserId, receiverId: this.activeUser.id, content, imageUrl: url, timestamp: new Date().toISOString(), isRead: false }];
          this.inputText = '';
        } catch {
          const newMsg = await this.api.sendMessage(this.activeUser.id, content, url);
          this.messages = [...this.messages, newMsg];
          this.inputText = '';
        }
        this.scrollToBottom();
        this.render();
      }
    } catch (e: any) {
      this.attachError = this.mapImageUploadError(e, file);
      this.render();
    } finally {
      if (this.fileInputRef) this.fileInputRef.nativeElement.value = '';
    }
  }

  async deleteMessage(id: string) {
    try {
      await this.api.deleteMessage(id);
      this.messages = this.messages.filter(m => m.id !== id);
      this.render();
    } catch (e) {
      console.error(e);
    }
  }

  isMe(msg: Message): boolean {
    return msg.senderId === this.currentUserId;
  }

  get visibleMessages(): Message[] {
    if (!this.activeUser) return this.messages;
    return this.messages.filter(m => (
      (m.senderId === this.currentUserId && m.receiverId === this.activeUser!.id) ||
      (m.senderId === this.activeUser!.id && m.receiverId === this.currentUserId)
    ));
  }

  formatTime(isoString: string): string {
    try {
      return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  }

  onAvatarError(event: Event, userId: string, size: number) {
    const img = event.target as HTMLImageElement | null;
    if (!img) return;
    img.src = `https://picsum.photos/seed/${userId}/${size}/${size}`;
  }

  private readonly allowedImageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

  private validateSelectedImageFile(file: File): string | null {
    const ext = this.extractFileExtension(file?.name || '');
    if (!ext || !this.allowedImageExtensions.includes(ext)) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    const type = String(file?.type || '').toLowerCase();
    if (type && !type.startsWith('image/')) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    return null;
  }

  private mapImageUploadError(error: any, file: File): string {
    const localValidation = this.validateSelectedImageFile(file);
    if (localValidation) return localValidation;
    const raw = String(
      error?.error?.message ||
      error?.error?.error ||
      error?.message ||
      ''
    ).toLowerCase();
    if (raw.includes('file_type_not_allowed')) {
      return this.i18n.t('new_item.error.file_type_not_allowed');
    }
    if (raw.includes('file_too_large')) {
      return this.i18n.t('new_item.error.file_too_large');
    }
    return this.i18n.t('new_item.error.upload_failed');
  }

  private extractFileExtension(filename: string): string {
    const value = String(filename || '').trim().toLowerCase();
    const dot = value.lastIndexOf('.');
    if (dot < 0 || dot >= value.length - 1) return '';
    return value.slice(dot + 1);
  }
}
