import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { CompanyResponse } from '../../models/company';

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
}