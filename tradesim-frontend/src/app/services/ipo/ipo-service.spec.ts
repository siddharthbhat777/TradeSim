import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IpoService } from './ipo-service';
import { environment } from '../../../environment/environment';

describe('IpoService', () => {
  let service: IpoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        IpoService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(IpoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get pending IPOs', () => {
    service.getPendingIpos().subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/ipo-offers/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should approve IPO', () => {
    service.approveIpoOffer('offer-123').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/ipo-offers/offer-123/approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should reject IPO', () => {
    service.rejectIpoOffer('offer-123', 'Incomplete details').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/ipo-offers/offer-123/reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rejectionReason: 'Incomplete details' });
    req.flush({});
  });
});