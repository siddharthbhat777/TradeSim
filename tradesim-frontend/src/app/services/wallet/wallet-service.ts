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
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/wallet`;

  private walletState = signal<Wallet | null>(null);
  public readonly wallet = this.walletState.asReadonly();

  loadWallet(): void {
    this.http.get<Wallet>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.walletState.set(data),
      error: () => this.walletState.set(null)
    });
  }

  deposit(request: WalletTransactionRequest): Observable<void> {
    return this.http.post<void>(`${this.apiBaseURL}/deposit`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  convert(request: CurrencyConversionRequest): Observable<void> {
    return this.http.post<void>(`${this.apiBaseURL}/convert`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}