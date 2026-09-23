import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExchangeService } from './exchange-service';
import { environment } from '../../../environment/environment';
import { Exchange, ExchangeMarketClock } from '../../models/exchange';

describe('ExchangeService', () => {
  let service: ExchangeService;
  let httpMock: HttpTestingController;

  const mockExchange: Exchange = {
    id: 'ex-123',
    name: 'National Stock Exchange',
    code: 'NSE',
    countryCode: 'IN',
    timezone: 'Asia/Kolkata',
    currency: 'INR',
    marketOpenTime: '09:15:00',
    marketCloseTime: '15:30:00',
    status: 'ACTIVE'
  };

  const mockClock: ExchangeMarketClock = {
    exchangeId: 'ex-123',
    exchangeCode: 'NSE',
    exchangeName: 'National Stock Exchange',
    timezone: 'Asia/Kolkata',
    localDate: '2026-09-19',
    localTime: '10:30:00',
    localDayOfWeek: 'SATURDAY',
    marketOpenTime: '09:15:00',
    marketCloseTime: '15:30:00',
    tradingDay: false,
    marketOpenNow: false,
    currentInstant: '2026-09-19T05:00:00Z',
    todayMarketOpenAt: '2026-09-19T03:45:00Z',
    todayMarketCloseAt: '2026-09-19T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ExchangeService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ExchangeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get exchanges', () => {
    service.getExchanges().subscribe((res) => {
      expect(res).toEqual([mockExchange]);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/exchanges`);
    expect(req.request.method).toBe('GET');
    req.flush([mockExchange]);
  });

  it('should get exchange by id', () => {
    service.getExchange('ex-123').subscribe((res) => {
      expect(res).toEqual(mockExchange);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/exchanges/ex-123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockExchange);
  });

  it('should get market clock for an exchange', () => {
    service.getMarketClock('ex-123').subscribe((res) => {
      expect(res).toEqual(mockClock);
      expect(res.marketOpenNow).toBe(false);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/exchanges/ex-123/market-clock`);
    expect(req.request.method).toBe('GET');
    req.flush(mockClock);
  });

  it('should change exchange status', () => {
    const updatedExchange = { ...mockExchange, status: 'HALTED' };

    service.changeStatus('ex-123', 'HALTED').subscribe((res) => {
      expect(res.status).toBe('HALTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/exchanges/ex-123/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ status: 'HALTED' });
    req.flush(updatedExchange);
  });
});