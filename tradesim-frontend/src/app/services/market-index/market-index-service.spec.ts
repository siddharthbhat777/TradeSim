import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MarketIndexService } from './market-index-service';
import { environment } from '../../../environment/environment';
import { MarketIndex, MarketIndexConstituent } from '../../models/market-index';

describe('MarketIndexService', () => {
  let service: MarketIndexService;
  let httpMock: HttpTestingController;

  const mockIndex: MarketIndex = {
    id: 'idx-123',
    name: 'NIFTY 50',
    symbol: 'NIFTY',
    exchangeId: 'ex-1',
    baseValue: 1000,
    currentValue: 24500.5,
    change: 150.25,
    changePercent: 0.62,
    dayOpen: 24400,
    dayHigh: 24550,
    dayLow: 24380,
    previousClose: 24350.25
  };

  const mockConstituent: MarketIndexConstituent = {
    stockId: 'stk-1',
    symbol: 'RELIANCE',
    companyName: 'Reliance Industries Limited'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MarketIndexService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MarketIndexService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all indices', () => {
    service.getAllIndices().subscribe((res) => {
      expect(res).toEqual([mockIndex]);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices`);
    expect(req.request.method).toBe('GET');
    req.flush([mockIndex]);
  });

  it('should fetch indices by exchange id', () => {
    service.getIndicesByExchange('ex-1').subscribe((res) => {
      expect(res).toEqual([mockIndex]);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices/exchange/ex-1`);
    expect(req.request.method).toBe('GET');
    req.flush([mockIndex]);
  });

  it('should fetch constituents of an index', () => {
    service.getConstituents('idx-123').subscribe((res) => {
      expect(res).toEqual([mockConstituent]);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices/idx-123/constituents`);
    expect(req.request.method).toBe('GET');
    req.flush([mockConstituent]);
  });

  it('should initialize index', () => {
    service.initializeIndex('idx-123').subscribe((res) => {
      expect(res).toEqual(mockIndex);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices/idx-123/initialize`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockIndex);
  });

  it('should create index', () => {
    const payload = {
      name: 'NIFTY IT',
      symbol: 'NIFTYIT',
      exchangeId: 'ex-1',
      baseValue: 1000
    };

    service.createIndex(payload).subscribe((res) => {
      expect(res).toEqual(mockIndex);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockIndex);
  });

  it('should add constituent to index', () => {
    service.addConstituent('idx-123', 'stk-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices/idx-123/constituents`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ stockId: 'stk-1' });
    req.flush(null);
  });

  it('should remove constituent from index', () => {
    service.removeConstituent('idx-123', 'stk-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/indices/idx-123/constituents/stk-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});