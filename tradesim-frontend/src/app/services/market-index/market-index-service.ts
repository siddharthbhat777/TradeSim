import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { MarketIndex, MarketIndexConstituent } from '../../models/market-index';

@Injectable({
  providedIn: 'root'
})
export class MarketIndexService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/indices`;

  getAllIndices() {
    return this.http.get<MarketIndex[]>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    });
  }

  getIndicesByExchange(exchangeId: string) {
    return this.http.get<MarketIndex[]>(`${this.apiBaseURL}/exchange/${exchangeId}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getConstituents(indexId: string) {
    return this.http.get<MarketIndexConstituent[]>(`${this.apiBaseURL}/${indexId}/constituents`, {
      context: skipInterceptors({ loader: true })
    });
  }
}