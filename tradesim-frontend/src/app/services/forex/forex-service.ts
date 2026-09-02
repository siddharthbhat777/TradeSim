import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ForexService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/forex`;

  getSupportedCurrencies(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiBaseURL}/currencies`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getExchangeRate(source: string, target: string): Observable<number> {
    const params = new HttpParams()
      .set('source', source)
      .set('target', target);

    return this.http.get<number>(`${this.apiBaseURL}/rate`, {
      params,
      context: skipInterceptors({ loader: true })
    });
  }
}