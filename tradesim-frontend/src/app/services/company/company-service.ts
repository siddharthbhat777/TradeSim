import { CompanyResponse, CompanyRepresentativeAssignmentResponse, PrimaryContactTransferResponse, CompanyOnboardingResponse } from '../../models/company';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class CompanyService {
  private readonly http = inject(HttpClient);
  private readonly companyURL = `${environment.apiBaseURL}/companies`;

  getCompanies() {
    return this.http.get<CompanyResponse[]>(this.companyURL, {
      context: skipInterceptors({ loader: true })
    });
  }

  getCompany(id: string) {
    return this.http.get<CompanyResponse>(`${this.companyURL}/${id}`, {
      context: skipInterceptors({ loader: true })
    });
  }

  createCompany(request: any) {
    return this.http.post<CompanyResponse>(this.companyURL, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  onboardCompany(request: any) {
    return this.http.post<CompanyOnboardingResponse>(`${this.companyURL}/onboard`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  changeStatus(id: string, status: string) {
    return this.http.put<CompanyResponse>(`${this.companyURL}/${id}/status`, { status }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  getRepresentatives(id: string) {
    return this.http.get<CompanyRepresentativeAssignmentResponse[]>(`${this.companyURL}/${id}/representatives`, {
      context: skipInterceptors({ loader: true })
    });
  }

  assignRepresentative(id: string, userId: string) {
    return this.http.post<CompanyRepresentativeAssignmentResponse>(`${this.companyURL}/${id}/representatives`, { userId }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  revokeRepresentative(companyId: string, userId: string) {
    return this.http.delete<CompanyRepresentativeAssignmentResponse>(`${this.companyURL}/${companyId}/representatives/${userId}`, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  transferPrimaryContact(companyId: string, newPrimaryContactUserId: string) {
    return this.http.put<PrimaryContactTransferResponse>(`${this.companyURL}/${companyId}/representatives/primary-contact`, { newPrimaryContactUserId }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}