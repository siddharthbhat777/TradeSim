import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Wallet, WalletTransactionRequest, CurrencyConversionRequest } from '../../models/wallet';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private readonly http = inject(HttpClient);
  private readonly walletURL = `${environment.apiBaseURL}/wallet`;

  private readonly walletState = signal<Wallet | null>(null);
  public readonly wallet = this.walletState.asReadonly();

  loadWallet(): void {
    this.http.get<Wallet>(this.walletURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.walletState.set(data),
      error: () => this.walletState.set(null)
    });
  }

  deposit(request: WalletTransactionRequest) {
    return this.http.post<void>(`${this.walletURL}/deposit`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  convert(request: CurrencyConversionRequest) {
    return this.http.post<void>(`${this.walletURL}/convert`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  requestMultiCurrency() {
    return this.http.post<void>(`${this.walletURL}/multi-currency/request`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  getPendingMultiCurrencyRequests() {
    return this.http.get<Wallet[]>(`${this.walletURL}/multi-currency/pending`, {
      context: skipInterceptors({ loader: true })
    });
  }

  approveMultiCurrencyAccess(walletId: string) {
    return this.http.put<Wallet>(`${this.walletURL}/multi-currency/${walletId}/approve`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  rejectMultiCurrencyAccess(walletId: string, rejectionReason: string) {
    return this.http.put<Wallet>(`${this.walletURL}/multi-currency/${walletId}/reject`, { rejectionReason }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}