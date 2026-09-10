import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { IpoService } from './ipo-service';

describe('IpoService', () => {
  let service: IpoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(IpoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});