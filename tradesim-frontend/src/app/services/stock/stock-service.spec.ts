import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { StockService } from './stock-service';
import { environment } from '../../../environment/environment';
import { Stock } from '../../models/stock';

describe('StockService', () => {
  let service: StockService;
  let httpMock: HttpTestingController;

  const mockStock: Stock = {
    id: 'stk-123',
    exchangeId: 'ex-1',
    symbol: 'TATAMOTORS',
    companyName: 'Tata Motors Limited',
    sector: 'AUTOMOBILE',
    currentPrice: 950.5,
    dayVolume: 1250000,
    marketCap: 350000000000,
    marketCapCategory: 'LARGE',
    currency: 'INR',
    status: 'ACTIVE'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StockService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(StockService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all stocks', () => {
    service.getStocks().subscribe((res) => {
      expect(res).toEqual([mockStock]);
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/stocks`);
    expect(req.request.method).toBe('GET');
    req.flush([mockStock]);
  });

  it('should fetch a single stock by id', () => {
    service.getStock('stk-123').subscribe((res) => {
      expect(res).toEqual(mockStock);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/stocks/stk-123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStock);
  });

  it('should change stock status', () => {
    const updatedStock = { ...mockStock, status: 'HALTED' };

    service.changeStockStatus('stk-123', 'HALTED').subscribe((res) => {
      expect(res.status).toBe('HALTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/stocks/change/stk-123/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ status: 'HALTED' });
    req.flush(updatedStock);
  });
});