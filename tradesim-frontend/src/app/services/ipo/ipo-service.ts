import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { IpoOfferResponse, IpoSubscriptionResponse } from '../../models/ipo';

@Injectable({
  providedIn: 'root'
})
export class IpoService {
  private readonly http = inject(HttpClient);
  private readonly ipoURL = `${environment.apiBaseURL}/ipo-offers`;

  getPendingIpos() {
    return this.http.get<IpoOfferResponse[]>(`${this.ipoURL}/pending`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getReadyIpos() {
    return this.http.get<IpoOfferResponse[]>(`${this.ipoURL}/ready`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getCompanyIpoOffers(companyId: string) {
    return this.http.get<IpoOfferResponse[]>(`${this.ipoURL}/company/${companyId}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getOpenIpos() {
    return this.http.get<IpoOfferResponse[]>(`${this.ipoURL}/open`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getUpcomingIpos() {
    return this.http.get<IpoOfferResponse[]>(`${this.ipoURL}/upcoming`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getMySubscriptions() {
    return this.http.get<IpoSubscriptionResponse[]>(`${this.ipoURL}/subscriptions`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getSubscriptionsForOffer(ipoOfferId: string) {
    return this.http.get<IpoSubscriptionResponse[]>(`${this.ipoURL}/${ipoOfferId}/subscriptions`, {
      context: skipInterceptors({ loader: true })
    });
  }

  submitIpoOffer(companyId: string, stockId: string, payload: any) {
    return this.http.post<IpoOfferResponse>(`${this.ipoURL}/${companyId}/stocks/${stockId}`, payload, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  subscribeToIpo(ipoOfferId: string) {
    return this.http.post<IpoSubscriptionResponse>(`${this.ipoURL}/${ipoOfferId}/subscriptions`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  approveIpoOffer(id: string) {
    return this.http.put<IpoOfferResponse>(`${this.ipoURL}/${id}/approve`, {}, {
      context: skipInterceptors({ loader: true })
    });
  }

  rejectIpoOffer(id: string, rejectionReason: string) {
    return this.http.put<IpoOfferResponse>(`${this.ipoURL}/${id}/reject`, { rejectionReason }, {
      context: skipInterceptors({ loader: true })
    });
  }

  finalizeIpoOffer(id: string) {
    return this.http.put<IpoOfferResponse>(`${this.ipoURL}/${id}/finalize`, {}, {
      context: skipInterceptors({ loader: true })
    });
  }
}