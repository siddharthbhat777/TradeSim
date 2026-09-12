import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Exchange, ExchangeMarketClock } from '../../models/exchange';

@Injectable({
  providedIn: 'root'
})
export class ExchangeService {
  private http = inject(HttpClient);
  private readonly exchangeURL = `${environment.apiBaseURL}/exchanges`;

  getExchanges() {
    return this.http.get<Exchange[]>(this.exchangeURL, {
      context: skipInterceptors({ loader: true })
    });
  }

  getExchange(exchangeId: string) {
    return this.http.get<Exchange>(`${this.exchangeURL}/${exchangeId}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getMarketClock(exchangeId: string) {
    return this.http.get<ExchangeMarketClock>(`${this.exchangeURL}/${exchangeId}/market-clock`);
  }
}