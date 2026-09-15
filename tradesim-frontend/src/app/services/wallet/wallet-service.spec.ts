import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WalletService } from './wallet-service';
import { environment } from '../../../environment/environment';

describe('WalletService', () => {
  let service: WalletService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WalletService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(WalletService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get pending multi-currency requests', () => {
    service.getPendingMultiCurrencyRequests().subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/wallet/multi-currency/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should approve multi-currency access', () => {
    service.approveMultiCurrencyAccess('wallet-123').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/wallet/multi-currency/wallet-123/approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should reject multi-currency access', () => {
    service.rejectMultiCurrencyAccess('wallet-123', 'Insufficient trading history').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/wallet/multi-currency/wallet-123/reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rejectionReason: 'Insufficient trading history' });
    req.flush({});
  });
});