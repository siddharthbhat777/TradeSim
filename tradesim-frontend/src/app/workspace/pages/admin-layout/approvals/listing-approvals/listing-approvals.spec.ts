import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ListingApprovals } from './listing-approvals';
import { ListingService } from '../../../../../services/listing/listing-service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { ListingRequestResponse } from '../../../../../models/listing';
import { UserListResponse } from '../../../../../models/user';

describe('ListingApprovals', () => {
  let component: ListingApprovals;
  let fixture: ComponentFixture<ListingApprovals>;
  let listingServiceMock: any;
  let dialogServiceMock: any;
  let toastServiceMock: any;

  const mockUser: UserListResponse = {
    id: 'usr-1',
    fullName: 'John Doe',
    username: 'johndoe',
    email: 'john@example.com',
    role: 'USER',
    accountStatus: 'ACTIVE',
    countryCode: 'US',
    lastLogin: null,
    createdAt: '2026-01-01T00:00:00Z'
  };

  const mockListing: ListingRequestResponse = {
    id: 'list-1',
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
    capTable: [{ userId: 'usr-1', quantity: 5000 }],
    status: 'PENDING_EXCHANGE_APPROVAL',
    reviewedByUserId: null,
    reviewedAt: null,
    approvedStockId: null,
    rejectionReason: null,
    currency: 'USD',
    createdAt: '2026-08-20T10:00:00Z',
    updatedAt: '2026-08-20T10:00:00Z'
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
    listingServiceMock = {
      approveListingRequest: vi.fn().mockReturnValue(of({ ...mockListing, status: 'APPROVED' })),
      rejectListingRequest: vi.fn().mockReturnValue(of({ ...mockListing, status: 'REJECTED' }))
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
      imports: [ListingApprovals],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ListingService, useValue: listingServiceMock },
        { provide: DialogService, useValue: dialogServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListingApprovals);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('data', [mockListing]);
    fixture.componentRef.setInput('users', [mockUser]);
    fixture.componentRef.setInput('isLoading', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should map user names correctly in cap table', () => {
    expect(component.userMap().get('usr-1')).toBe('John Doe');
  });

  it('should approve a listing request and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.confirmApprove(mockListing.id);

    expect(dialogServiceMock.open).toHaveBeenCalled();
    expect(listingServiceMock.approveListingRequest).toHaveBeenCalledWith('list-1');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Listing approved successfully.');
    expect(processedSpy).toHaveBeenCalledWith('list-1');
  });

  it('should reject a listing request and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.openRejectModal(mockListing.id);
    component.rejectForm.controls.reason.setValue('Missing documents');
    component.submitRejection();

    expect(listingServiceMock.rejectListingRequest).toHaveBeenCalledWith('list-1', 'Missing documents');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Listing request rejected successfully.');
    expect(processedSpy).toHaveBeenCalledWith('list-1');
    expect(component.showRejectModal()).toBe(false);
  });
});