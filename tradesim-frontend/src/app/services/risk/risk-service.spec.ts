import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RiskService } from './risk-service';

describe('RiskService', () => {
  let service: RiskService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(RiskService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});