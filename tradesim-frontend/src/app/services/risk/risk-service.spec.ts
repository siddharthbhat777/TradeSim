import { TestBed } from '@angular/core/testing';

import { RiskService } from './risk-service';

describe('RiskService', () => {
  let service: RiskService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RiskService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
