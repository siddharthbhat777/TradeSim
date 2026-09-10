import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TradingAccountService } from './trading-account-service';

describe('TradingAccountService', () => {
  let service: TradingAccountService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TradingAccountService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});