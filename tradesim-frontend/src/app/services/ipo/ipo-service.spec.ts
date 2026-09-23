import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IpoService } from './ipo-service';
import { environment } from '../../../environment/environment';
import { IpoOfferResponse } from '../../models/ipo';

describe('IpoService', () => {
  let service: IpoService;
  let httpMock: HttpTestingController;

  const mockIpo: IpoOfferResponse = {
    id: 'offer-123',
    companyId: 'cmp-1',
    stockId: 'stk-1',
    symbol: 'ACME',
    companyName: 'Acme Corp',
    exchangeName: 'NYSE',
    submittedByUserId: 'usr-1',
    issuePrice: 100,
    sharesPerAllottee: 10,
    maxAllottees: 100,
    totalSharesOffered: 1000,
    subscriptionStartAt: '2026-09-01T00:00:00Z',
    subscriptionEndAt: '2026-09-10T00:00:00Z',
    status: 'PENDING_APPROVAL',
    reviewedByUserId: null,
    reviewedAt: null,
    finalizedByUserId: null,
    finalizedAt: null,
    rejectionReason: null,
    createdAt: '2026-08-25T10:00:00Z',
    updatedAt: '2026-08-25T10:00:00Z',
    currency: 'USD'
  };

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
    req.flush([mockIpo]);
  });

  it('should approve IPO', () => {
    service.approveIpoOffer('offer-123').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/ipo-offers/offer-123/approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockIpo, status: 'SUBSCRIPTION_OPEN' });
  });

  it('should reject IPO', () => {
    service.rejectIpoOffer('offer-123', 'Incomplete details').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/ipo-offers/offer-123/reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rejectionReason: 'Incomplete details' });
    req.flush({ ...mockIpo, status: 'REJECTED', rejectionReason: 'Incomplete details' });
  });
});