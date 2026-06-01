import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WsService {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];
  private activating = false;

  connect() {
    if (this.client?.active || this.activating) return;
    this.activating = true;
    const wsUrl = environment.wsUrl || '/ws';
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000
    });
    client.onConnect = () => {
      this.activating = false;
    };
    client.onStompError = () => { };
    client.onWebSocketClose = () => {
      this.activating = false;
    };
    client.activate();
    this.client = client;
  }

  disconnect() {
    for (const s of this.subscriptions) {
      try {
        s.unsubscribe();
      } catch { }
    }
    this.subscriptions = [];
    try {
      this.client?.deactivate();
    } catch { }
    this.client = null;
    this.activating = false;
  }

  subscribeUser(userId: string, cb: (msg: any) => void) {
    const client = this.client;
    if (!client || !client.active) return;
    const id = String(userId || '').trim();
    if (!id) return;
    const sub = client.subscribe(`/topic/messages/${id}`, (m: IMessage) => {
      try {
        cb(JSON.parse(m.body));
      } catch {
        cb(m.body as any);
      }
    });
    this.subscriptions.push(sub);
  }

  subscribePresence(cb: (upd: { userId: string; online: boolean }) => void) {
    const client = this.client;
    if (!client || !client.active) return;
    const sub = client.subscribe(`/topic/presence`, (m: IMessage) => {
      try {
        cb(JSON.parse(m.body));
      } catch { }
    });
    this.subscriptions.push(sub);
  }

  announceOnline(userId: string) {
    const client = this.client;
    if (!client || !client.active) return;
    const id = String(userId || '').trim();
    if (!id) return;
    try {
      client.publish({ destination: '/app/presence/online', body: JSON.stringify({ userId: id }) });
    } catch { }
  }

  sendMessage(senderId: string, receiverId: string, content: string, imageUrl?: string) {
    const client = this.client;
    if (!client || !client.active) {
      throw new Error('ws_not_connected');
    }
    const body: any = { senderId, receiverId, content };
    if (imageUrl) body.imageUrl = imageUrl;
    client.publish({ destination: '/app/messages/send', body: JSON.stringify(body) });
  }
}
