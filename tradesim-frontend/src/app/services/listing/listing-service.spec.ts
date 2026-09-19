import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ListingService } from './listing-service';
import { environment } from '../../../environment/environment';
import { ListingRequestResponse } from '../../models/listing';

describe('ListingService', () => {
  let service: ListingService;
  let httpMock: HttpTestingController;

  const mockListing: ListingRequestResponse = {
    id: 'listing-123',
    companyId: 'cmp-1',
    companyName: 'Acme Corp',
    submittedByUserId: 'usr-1',
    symbol: 'ACME',
    exchangeId: 'ex-1',
    exchangeName: 'NYSE',
    referencePrice: 150,
    sector: 'TECHNOLOGY',
    priceBandPercent: 10,
    totalShares: 1000000,
    capTable: [],
    status: 'PENDING_EXCHANGE_APPROVAL',
    reviewedByUserId: null,
    reviewedAt: null,
    approvedStockId: null,
    rejectionReason: null,
    currency: 'USD',
    createdAt: '2026-08-20T10:00:00Z',
    updatedAt: '2026-08-20T10:00:00Z'
  };

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
    req.flush([mockListing]);
  });

  it('should approve listing request', () => {
    service.approveListingRequest('listing-123').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/listing-requests/listing-123/exchange-approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockListing, status: 'APPROVED' });
  });

  it('should reject listing request', () => {
    service.rejectListingRequest('listing-123', 'Missing financials').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/listing-requests/listing-123/exchange-reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rejectionReason: 'Missing financials' });
    req.flush({ ...mockListing, status: 'REJECTED', rejectionReason: 'Missing financials' });
  });
});