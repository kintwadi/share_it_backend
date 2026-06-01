import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { Notification } from '../models/types';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private api = inject(ApiClientService);

  async getNotifications(): Promise<Notification[]> {
    try {
      const list = await firstValueFrom(this.api.get<any>('/notifications'));
      return Array.isArray(list) ? (list as Notification[]) : [];
    } catch {
      return [];
    }
  }

  async markNotificationAsRead(id: string): Promise<void> {
    const notifId = String(id || '').trim();
    if (!notifId) return;
    try {
      await firstValueFrom(this.api.post(`/notifications/${encodeURIComponent(notifId)}/read`, {}));
    } catch { }
  }
}
