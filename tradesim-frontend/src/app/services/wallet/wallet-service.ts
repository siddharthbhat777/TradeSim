import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { WalletResponse } from '../../models/wallet';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/wallet`;

  private walletState = signal<WalletResponse | null>(null);
  public readonly wallet = this.walletState.asReadonly();

  loadWallet(): void {
    this.http.get<WalletResponse>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.walletState.set(data),
      error: () => this.walletState.set(null)
    });
  }
}