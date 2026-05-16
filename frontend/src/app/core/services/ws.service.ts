import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { Message } from '../models/types';

type PendingSub = { dest: string; handler: (payload: any) => void };

@Injectable({
  providedIn: 'root'
})
export class WsService {
  private client: Client | null = null;
  private connected = false;
  private pendingSubs: PendingSub[] = [];

  connect() {
    if (this.client && this.connected) return;
    const wsBase = this.getWsBaseUrl();

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsBase}/ws`),
      reconnectDelay: 5000,
      debug: () => { }
    });

    this.client.onConnect = () => {
      this.connected = true;
      const subs = this.pendingSubs.splice(0);
      subs.forEach(({ dest, handler }) => this.subscribe(dest, handler));
    };

    this.client.onDisconnect = () => {
      this.connected = false;
    };

    this.client.activate();
  }

  disconnect() {
    if (!this.client) return;
    try {
      this.client.deactivate();
    } finally {
      this.client = null;
      this.connected = false;
      this.pendingSubs = [];
    }
  }

  subscribeUser(userId: string, handler: (m: Message) => void) {
    const dest = `/topic/messages.${userId}`;
    const wrapped = (body: any) => {
      const msg: Message = {
        id: String(body.id),
        senderId: String(body.senderId),
        receiverId: String(body.receiverId),
        senderEmail: body.senderEmail ? String(body.senderEmail) : undefined,
        receiverEmail: body.receiverEmail ? String(body.receiverEmail) : undefined,
        content: body.content ?? '',
        imageUrl: body.imageUrl ? String(body.imageUrl) : undefined,
        timestamp: body.timestamp,
        isRead: !!body.isRead,
      };
      handler(msg);
    };
    this.subscribe(dest, wrapped);
  }

  subscribePresence(handler: (update: { userId: string; online: boolean }) => void) {
    const dest = `/topic/presence`;
    const wrapped = (body: any) => {
      handler({ userId: String(body.userId), online: !!body.online });
    };
    this.subscribe(dest, wrapped);
  }

  announceOnline(userId: string) {
    if (!this.client || !this.connected) return;
    this.client.publish({ destination: '/app/presence.online', body: userId });
  }

  sendMessage(senderId: string, receiverId: string, content: string, imageUrl?: string) {
    if (!this.client || !this.connected) {
      throw new Error('ws_not_connected');
    }
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ senderId, receiverId, content, imageUrl })
    });
  }

  private subscribe(dest: string, handler: (payload: any) => void) {
    if (!this.client || !this.connected) {
      this.pendingSubs.push({ dest, handler });
      this.connect();
      return;
    }
    this.client.subscribe(dest, (frame) => {
      try {
        const body = JSON.parse(frame.body);
        handler(body);
      } catch { }
    });
  }

  private getWsBaseUrl(): string {
    const apiUrl = this.resolveApiUrl();
    if (apiUrl.endsWith('/api')) return apiUrl.slice(0, -4);
    return apiUrl.replace(/\/api\/?$/, '');
  }

  private resolveApiUrl(): string {
    try {
      const w = globalThis as any;
      const runtime = String(w?.__env?.API_URL || '').trim();
      if (runtime) return runtime.replace(/\/+$/, '');
    } catch { }
    return String(environment.apiUrl || '').replace(/\/+$/, '');
  }
}
