import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { LedgerEntryResponse } from '../../models/ledger';
import { skipInterceptors } from '../../shared/utils/http-context';

@Injectable({
  providedIn: 'root'
})
export class LedgerService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseURL = `${environment.apiBaseURL}/ledger`;

  getMyLedger(): Observable<LedgerEntryResponse[]> {
    return this.http.get<LedgerEntryResponse[]>(this.apiBaseURL, {
      context: skipInterceptors({ loader: true })
    });
  }
}