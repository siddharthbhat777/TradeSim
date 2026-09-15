import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ListingService } from './listing-service';
import { environment } from '../../../environment/environment';

describe('ListingService', () => {
  let service: ListingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ListingService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ListingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get pending exchange requests', () => {
    service.getPendingExchangeRequests().subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/listing-requests/pending-exchange`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should approve listing request', () => {
    service.approveListingRequest('listing-123').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/listing-requests/listing-123/exchange-approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should reject listing request', () => {
    service.rejectListingRequest('listing-123', 'Missing financials').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/listing-requests/listing-123/exchange-reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rejectionReason: 'Missing financials' });
    req.flush({});
  });
});