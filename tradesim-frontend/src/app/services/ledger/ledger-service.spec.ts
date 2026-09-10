import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LedgerService } from './ledger-service';

describe('LedgerService', () => {
  let service: LedgerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(LedgerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});