import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TradingAccountService } from './trading-account-service';
import { environment } from '../../../environment/environment';
import { TradingAccountResponse } from '../../models/trading-account';

describe('TradingAccountService', () => {
  let service: TradingAccountService;
  let httpMock: HttpTestingController;

  const mockAccount: TradingAccountResponse = {
    id: 'acc-1',
    userId: 'user-123',
    baseCurrency: 'INR',
    marginLoan: 0,
    leverage: 5,
    maintenanceMarginPercent: 25
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TradingAccountService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TradingAccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get trading account', () => {
    service.getTradingAccount().subscribe((res) => {
      expect(res).toEqual(mockAccount);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/trading-account`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAccount);
  });

  it('should load trading account into state', () => {
    service.loadTradingAccount();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/trading-account`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAccount);

    expect(service.tradingAccount()).toEqual(mockAccount);
  });
});