import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RxStomp } from '@stomp/rx-stomp';
import { Subscription } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthService } from '../auth/auth-service';
import { environment } from '../../../environment/environment';
import { AppNotification } from '../../models/notification';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  private rxStomp: RxStomp | null = null;
  private topicSubscription?: Subscription;

  readonly notifications = signal<AppNotification[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter(n => !n.isRead).length);

  private readonly apiUrl = `${environment.apiBaseURL}/notifications`;

  fetchHistory() {
    return this.http.get<AppNotification[]>(`${this.apiUrl}/admin`).pipe(
      tap((history) => {
        this.notifications.set(history);
      })
    );
  }

  connect(): void {
    if (this.rxStomp) return;

    this.rxStomp = new RxStomp();

    const wsUrl = environment.apiBaseURL
      .replace('http://', 'ws://')
      .replace('https://', 'wss://')
      .replace('/api', '/ws');

    this.rxStomp.configure({
      brokerURL: wsUrl,
      beforeConnect: (stompClient: any): Promise<void> => {
        return new Promise<void>((resolve) => {
          const token = this.authService.getAccessToken();
          stompClient.connectHeaders = {
            Authorization: `Bearer ${token}`
          };
          resolve();
        });
      },
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
    });

    this.rxStomp.activate();

    this.topicSubscription = this.rxStomp.watch('/user/queue/admin-alerts').subscribe((message) => {
      const payload: AppNotification = JSON.parse(message.body);

      if (!payload.createdAt) {
        payload.createdAt = new Date().toISOString();
      }

      this.notifications.update(prev => [payload, ...prev]);
    });
  }

  disconnect(): void {
    this.topicSubscription?.unsubscribe();
    this.rxStomp?.deactivate();
    this.rxStomp = null;
    this.notifications.set([]);
  }

  markAsRead(id: string) {
    this.notifications.update(prev =>
      prev.map(n => n.id === id ? { ...n, isRead: true } : n)
    );

    return this.http.put(`${this.apiUrl}/${id}/read`, {}).subscribe();
  }

  clearAll() {
    this.notifications.set([]);
    return this.http.delete(`${this.apiUrl}/admin/clear`).subscribe();
  }
}