import { Injectable } from '@angular/core';
import { Notification, NotificationType } from '../models/types';

const NOTIFICATIONS_KEY = 'nearshare_notifications';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  async getNotifications(): Promise<Notification[]> {
    const stored = localStorage.getItem(NOTIFICATIONS_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    }
    const initial: Notification[] = [
      {
        id: 'notif_1',
        userId: 'current',
        type: NotificationType.PENDING_REQUEST,
        title: 'New Borrow Request',
        message: 'Alice wants to borrow your Drill',
        timestamp: new Date().toISOString(),
        isRead: false,
        link: '/listing/item_1',
        metadata: { listingId: 'item_1', actorId: 'user_2' }
      },
      {
        id: 'notif_2',
        userId: 'current',
        type: NotificationType.PRICE_SUGGESTION,
        title: 'Price Suggestion',
        message: 'A neighbor suggested a lower price for your Lawn Mower',
        timestamp: new Date(Date.now() - 3600000).toISOString(),
        isRead: true,
        link: '/listing/item_2',
        metadata: { listingId: 'item_2', price: 15 }
      },
      {
        id: 'notif_3',
        userId: 'current',
        type: NotificationType.REQUEST_APPROVED,
        title: 'Request Approved',
        message: 'Bob approved your request for Bike',
        timestamp: new Date(Date.now() - 7200000).toISOString(),
        isRead: false,
        link: '/listing/item_3',
        metadata: { listingId: 'item_3', actorId: 'user_3' }
      }
    ];
    localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(initial));
    return initial;
  }

  async createNotification(notif: Omit<Notification, 'id' | 'timestamp' | 'isRead'>): Promise<Notification> {
    const newNotif: Notification = {
      ...notif,
      id: `notif_${Date.now()}`,
      timestamp: new Date().toISOString(),
      isRead: false
    };
    const current = await this.getNotifications();
    const updated = [newNotif, ...current];
    localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(updated));
    return newNotif;
  }

  async markNotificationAsRead(id: string): Promise<void> {
    const current = await this.getNotifications();
    const updated = current.map(n => n.id === id ? { ...n, isRead: true } : n);
    localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(updated));
  }
}

