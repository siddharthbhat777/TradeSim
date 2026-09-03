import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PortfolioExposureResponse, PortfolioHistoryResponse, PortfolioResponse } from '../../models/portfolio';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/portfolio`;

  private portfolioState = signal<PortfolioResponse | null>(null);
  public readonly portfolio = this.portfolioState.asReadonly();

  loadPortfolio(): void {
    this.http.get<PortfolioResponse>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.portfolioState.set(data),
      error: () => this.portfolioState.set(null)
    });
  }

  getPortfolioHistory(): Observable<PortfolioHistoryResponse[]> {
    return this.http.get<PortfolioHistoryResponse[]>(`${this.apiBaseURL}/history`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getExposure(): Observable<PortfolioExposureResponse[]> {
    return this.http.get<PortfolioExposureResponse[]>(`${this.apiBaseURL}/exposure`, {
      context: skipInterceptors({ loader: true })
    });
  }
}