import { inject, Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { Subject } from 'rxjs';
import { AuthService } from '../auth/auth-service';
import { environment } from '../../../environment/environment';

export interface SyncEvent {
  type: string;
}

@Injectable({
  providedIn: 'root'
})
export class DataSyncService {
  private readonly authService = inject(AuthService);
  private rxStomp: RxStomp | null = null;

  readonly syncEvents$ = new Subject<SyncEvent>();

  connect(): void {
    if (this.rxStomp) return;

    this.rxStomp = new RxStomp();

    const wsUrl = environment.apiBaseURL.replace(/^http/, 'ws') + '/ws';

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

    this.rxStomp.watch('/topic/admin-sync').subscribe((message) => {
      const payload: SyncEvent = JSON.parse(message.body);
      this.syncEvents$.next(payload);
    });
  }

  disconnect(): void {
    this.rxStomp?.deactivate();
    this.rxStomp = null;
  }
}