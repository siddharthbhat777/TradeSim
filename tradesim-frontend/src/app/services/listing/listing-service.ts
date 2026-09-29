import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { CreateListingRequest, ListingRequestResponse } from '../../models/listing';

@Injectable({
  providedIn: 'root'
})
export class ListingService {
  private readonly http = inject(HttpClient);
  private readonly listingURL = `${environment.apiBaseURL}/listing-requests`;

  getCompanyRequests(companyId: string) {
    return this.http.get<ListingRequestResponse[]>(`${this.listingURL}/company/${companyId}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getPendingExchangeRequests() {
    return this.http.get<ListingRequestResponse[]>(`${this.listingURL}/pending-exchange`, {
      context: skipInterceptors({ loader: true })
    });
  }

  submitListingRequest(companyId: string, request: CreateListingRequest) {
    return this.http.post<ListingRequestResponse>(`${this.listingURL}/${companyId}`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  approveInternalRequest(id: string) {
    return this.http.put<ListingRequestResponse>(`${this.listingURL}/${id}/internal-approve`, {}, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  rejectInternalRequest(id: string, rejectionReason: string) {
    return this.http.put<ListingRequestResponse>(`${this.listingURL}/${id}/internal-reject`, { rejectionReason }, {
      context: skipInterceptors({ loader: true, toast: true })
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