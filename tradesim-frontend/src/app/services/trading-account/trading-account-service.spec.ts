import { TestBed } from '@angular/core/testing';

import { TradingAccountService } from './trading-account-service';

describe('TradingAccountService', () => {
  let service: TradingAccountService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TradingAccountService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
