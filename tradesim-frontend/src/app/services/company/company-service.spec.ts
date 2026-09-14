import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CompanyService } from './company-service';
import { environment } from '../../../environment/environment';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CompanyService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CompanyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get companies', () => {
    service.getCompanies().subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/companies`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});