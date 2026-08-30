import { TestBed } from '@angular/core/testing';

import { MarketIndexService } from './market-index-service';

describe('MarketIndexService', () => {
  let service: MarketIndexService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarketIndexService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
