import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { MarketIndex, MarketIndexConstituent } from '../../models/market-index';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class MarketIndexService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseURL}/indices`;

  getAllIndices(): Observable<MarketIndex[]> {
    return this.http.get<MarketIndex[]>(this.apiUrl, {
      context: skipInterceptors({ loader: true })
    });
  }

  getIndicesByExchange(exchangeId: string): Observable<MarketIndex[]> {
    return this.http.get<MarketIndex[]>(`${this.apiUrl}/exchange/${exchangeId}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getConstituents(indexId: string): Observable<MarketIndexConstituent[]> {
    return this.http.get<MarketIndexConstituent[]>(`${this.apiUrl}/${indexId}/constituents`, {
      context: skipInterceptors({ loader: true })
    });
  }

  initializeIndex(indexId: string): Observable<MarketIndex> {
    return this.http.post<MarketIndex>(`${this.apiUrl}/${indexId}/initialize`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  createIndex(payload: { name: string, symbol: string, exchangeId: string, baseValue: number }): Observable<MarketIndex> {
    return this.http.post<MarketIndex>(this.apiUrl, payload, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  addConstituent(indexId: string, stockId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${indexId}/constituents`, { stockId }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  removeConstituent(indexId: string, stockId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${indexId}/constituents/${stockId}`, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}