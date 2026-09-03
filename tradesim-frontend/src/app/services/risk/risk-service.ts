import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { RiskResponse } from '../../models/risk';

@Injectable({
  providedIn: 'root'
})
export class RiskService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/risk`;

  getMyRisk(): Observable<RiskResponse> {
    return this.http.get<RiskResponse>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    });
  }
}