import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ForexService } from './forex-service';

describe('ForexService', () => {
  let service: ForexService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ForexService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});