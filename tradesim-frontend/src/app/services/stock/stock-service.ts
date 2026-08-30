import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Stock } from '../../models/stock';

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/stocks`;

  getStocks() {
    return this.http.get<Stock[]>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    });
  }
}