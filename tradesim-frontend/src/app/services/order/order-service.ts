import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { OrderHistoryResponse } from '../../models/order-response';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/orders`;

  private orderState = signal<OrderHistoryResponse[]>([]);
  public readonly orders = this.orderState.asReadonly();

  loadOrders(): void {
    this.http.get<OrderHistoryResponse[]>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    }).subscribe({
      next: (data) => this.orderState.set(data),
      error: () => this.orderState.set([])
    });
  }
}