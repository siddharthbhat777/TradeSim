import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { skipInterceptors } from '../../shared/utils/http-context';
import { Observable, tap } from 'rxjs';

export interface Country {
  code: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class ForexService {
  private http = inject(HttpClient);
  private readonly forexURL = `${environment.apiBaseURL}/forex`;

  readonly countries = signal<Country[]>([]);
  readonly isCountriesLoaded = signal<boolean>(false);

  constructor() {
    this.loadCountries();
  }

  loadCountries(): void {
    if (this.isCountriesLoaded()) {
      return;
    }
    this.fetchCountries().subscribe();
  }

  fetchCountries(): Observable<Country[]> {
    return this.http.get<Country[]>(`${this.forexURL}/countries`, {
      context: skipInterceptors({ loader: true })
    }).pipe(
      tap((countries) => {
        this.countries.set(countries);
        this.isCountriesLoaded.set(true);
      })
    );
  }

  getCountryName(code: string): string {
    if (!code) {
      return '';
    }
    const found = this.countries().find(c => c.code.toUpperCase() === code.toUpperCase());
    return found ? found.name : code;
  }

  getSupportedCurrencies(): Observable<string[]> {
    return this.http.get<string[]>(`${this.forexURL}/currencies`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getCurrencyForCountry(countryCode: string): Observable<string> {
    return this.http.get(`${this.forexURL}/country-currency/${countryCode}`, {
      responseType: 'text',
      context: skipInterceptors({ loader: true })
    });
  }

  getExchangeRate(source: string, target: string): Observable<number> {
    const params = new HttpParams()
      .set('source', source)
      .set('target', target);

    return this.http.get<number>(`${this.forexURL}/rate`, {
      params,
      context: skipInterceptors({ loader: true })
    });
  }
}