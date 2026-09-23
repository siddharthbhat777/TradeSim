import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { IpoApprovals } from './ipo-approvals';
import { IpoService } from '../../../../../services/ipo/ipo-service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { IpoOfferResponse } from '../../../../../models/ipo';

describe('IpoApprovals', () => {
  let component: IpoApprovals;
  let fixture: ComponentFixture<IpoApprovals>;
  let ipoServiceMock: any;
  let dialogServiceMock: any;
  let toastServiceMock: any;

  const mockIpo: IpoOfferResponse = {
    id: 'ipo-1',
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

  beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  beforeEach(async () => {
    ipoServiceMock = {
      approveIpoOffer: vi.fn().mockReturnValue(of({ ...mockIpo, status: 'SUBSCRIPTION_OPEN' })),
      rejectIpoOffer: vi.fn().mockReturnValue(of({ ...mockIpo, status: 'REJECTED' }))
    };

    dialogServiceMock = {
      open: vi.fn().mockImplementation((config: any) => {
        if (config.onPrimary) config.onPrimary();
      })
    };

    toastServiceMock = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [IpoApprovals],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: IpoService, useValue: ipoServiceMock },
        { provide: DialogService, useValue: dialogServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IpoApprovals);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('data', [mockIpo]);
    fixture.componentRef.setInput('isLoading', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should approve an IPO and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.confirmApprove(mockIpo.id);

    expect(dialogServiceMock.open).toHaveBeenCalled();
    expect(ipoServiceMock.approveIpoOffer).toHaveBeenCalledWith('ipo-1');
    expect(toastServiceMock.success).toHaveBeenCalledWith('IPO offer approved successfully.');
    expect(processedSpy).toHaveBeenCalledWith('ipo-1');
  });

  it('should reject an IPO and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.openRejectModal(mockIpo.id);
    component.rejectForm.controls.reason.setValue('Incomplete details');
    component.submitRejection();

    expect(ipoServiceMock.rejectIpoOffer).toHaveBeenCalledWith('ipo-1', 'Incomplete details');
    expect(toastServiceMock.success).toHaveBeenCalledWith('IPO offer rejected successfully.');
    expect(processedSpy).toHaveBeenCalledWith('ipo-1');
    expect(component.showRejectModal()).toBe(false);
  });
});