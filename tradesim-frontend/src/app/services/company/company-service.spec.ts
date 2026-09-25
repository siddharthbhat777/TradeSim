import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CompanyService } from './company-service';
import { environment } from '../../../environment/environment';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseURL}/companies`;

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
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get assigned companies', () => {
    service.getAssignedCompanies().subscribe();
    const req = httpMock.expectOne(`${baseUrl}/assigned`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get a company by id', () => {
    service.getCompany('comp-1').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should create a company', () => {
    const mockRequest = { name: 'Test' };
    service.createCompany(mockRequest).subscribe();
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush({});
  });

  it('should onboard a company', () => {
    const mockRequest = { company: {}, representative: {} };
    service.onboardCompany(mockRequest).subscribe();
    const req = httpMock.expectOne(`${baseUrl}/onboard`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush({});
  });

  it('should change company status', () => {
    service.changeStatus('comp-1', 'ACTIVE').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ status: 'ACTIVE' });
    req.flush({});
  });

  it('should get representatives', () => {
    service.getRepresentatives('comp-1').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1/representatives`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should assign a representative', () => {
    service.assignRepresentative('comp-1', 'usr-1').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1/representatives`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'usr-1' });
    req.flush({});
  });

  it('should revoke a representative', () => {
    service.revokeRepresentative('comp-1', 'usr-1').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1/representatives/usr-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should transfer primary contact', () => {
    service.transferPrimaryContact('comp-1', 'usr-2').subscribe();
    const req = httpMock.expectOne(`${baseUrl}/comp-1/representatives/primary-contact`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ newPrimaryContactUserId: 'usr-2' });
    req.flush({});
  });
});