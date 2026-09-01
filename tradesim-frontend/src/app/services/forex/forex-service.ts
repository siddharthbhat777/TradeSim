import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class ForexService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/forex`;

  getSupportedCurrencies() {
    return this.http.get<string[]>(`${this.apiBaseURL}/currencies`, {
      context: skipInterceptors({ loader: true })
    });
  }
}