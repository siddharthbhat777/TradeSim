import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MarketIndexService } from './market-index-service';

describe('MarketIndexService', () => {
  let service: MarketIndexService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MarketIndexService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});