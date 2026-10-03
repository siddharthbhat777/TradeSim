import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ForexService } from './forex-service';
import { environment } from '../../../environment/environment';

describe('ForexService', () => {
  let service: ForexService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ForexService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ForexService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and initiate loadCountries on startup', () => {
    expect(service).toBeTruthy();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/forex/countries`);
    expect(req.request.method).toBe('GET');
    req.flush([{ code: 'IN', name: 'India' }]);

    expect(service.countries()).toEqual([{ code: 'IN', name: 'India' }]);
    expect(service.isCountriesLoaded()).toBe(true);
  });

  it('should get country name by code', () => {
    const req = httpMock.expectOne(`${environment.apiBaseURL}/forex/countries`);
    req.flush([{ code: 'IN', name: 'India' }]);

    expect(service.getCountryName('IN')).toBe('India');
    expect(service.getCountryName('us')).toBe('us');
    expect(service.getCountryName('')).toBe('');
  });

  it('should fetch supported currencies', () => {
    httpMock.expectOne(`${environment.apiBaseURL}/forex/countries`).flush([]);

    service.getSupportedCurrencies().subscribe(res => {
      expect(res).toEqual(['INR', 'USD']);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/forex/currencies`);
    expect(req.request.method).toBe('GET');
    req.flush(['INR', 'USD']);
  });

  it('should resolve currency for a specific country code', () => {
    httpMock.expectOne(`${environment.apiBaseURL}/forex/countries`).flush([]);

    service.getCurrencyForCountry('IN').subscribe(res => {
      expect(res).toBe('INR');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/forex/country-currency/IN`);
    expect(req.request.method).toBe('GET');
    req.flush('INR');
  });
});