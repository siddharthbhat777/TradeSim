import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { IpoOfferResponse, IpoSubscriptionResponse } from '../../models/ipo';

@Injectable({
  providedIn: 'root'
})
export class IpoService {
  private http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/ipo-offers`;

  getOpenIpos(): Observable<IpoOfferResponse[]> {
    return this.http.get<IpoOfferResponse[]>(`${this.apiBaseURL}/open`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getUpcomingIpos(): Observable<IpoOfferResponse[]> {
    return this.http.get<IpoOfferResponse[]>(`${this.apiBaseURL}/upcoming`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getMySubscriptions(): Observable<IpoSubscriptionResponse[]> {
    return this.http.get<IpoSubscriptionResponse[]>(`${this.apiBaseURL}/subscriptions`, {
      context: skipInterceptors({ loader: true })
    });
  }

  subscribeToIpo(ipoOfferId: string): Observable<IpoSubscriptionResponse> {
    return this.http.post<IpoSubscriptionResponse>(`${this.apiBaseURL}/${ipoOfferId}/subscriptions`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}