import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { TradingAccountResponse } from '../../models/trading-account';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class TradingAccountService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/trading-account`;

  private tradingAccountState = signal<TradingAccountResponse | null>(null);
  public readonly tradingAccount = this.tradingAccountState.asReadonly();

  loadTradingAccount(): void {
    this.http.get<TradingAccountResponse>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.tradingAccountState.set(data),
      error: () => this.tradingAccountState.set(null)
    });
  }
}