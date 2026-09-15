import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { ListingRequestResponse } from '../../models/listing';

@Injectable({
  providedIn: 'root'
})
export class ListingService {
  private readonly http = inject(HttpClient);
  private readonly listingURL = `${environment.apiBaseURL}/listing-requests`;

  getPendingExchangeRequests() {
    return this.http.get<ListingRequestResponse[]>(`${this.listingURL}/pending-exchange`, {
      context: skipInterceptors({ loader: true })
    });
  }

  approveListingRequest(id: string) {
    return this.http.put<ListingRequestResponse>(`${this.listingURL}/${id}/exchange-approve`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  rejectListingRequest(id: string, rejectionReason: string) {
    return this.http.put<ListingRequestResponse>(`${this.listingURL}/${id}/exchange-reject`, { rejectionReason }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}