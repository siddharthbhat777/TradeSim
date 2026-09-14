import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Wallet, WalletTransactionRequest, CurrencyConversionRequest } from '../../models/wallet';
import { Observable } from 'rxjs';

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

  deposit(request: WalletTransactionRequest): Observable<void> {
    return this.http.post<void>(`${this.walletURL}/deposit`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  convert(request: CurrencyConversionRequest): Observable<void> {
    return this.http.post<void>(`${this.walletURL}/convert`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  requestMultiCurrency(): Observable<void> {
    return this.http.post<void>(`${this.walletURL}/multi-currency/request`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}