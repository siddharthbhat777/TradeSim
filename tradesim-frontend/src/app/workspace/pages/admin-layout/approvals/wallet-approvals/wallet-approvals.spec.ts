import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { WalletApprovals } from './wallet-approvals';
import { WalletService } from '../../../../../services/wallet/wallet-service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { Wallet } from '../../../../../models/wallet';
import { UserListResponse } from '../../../../../models/user';

describe('WalletApprovals', () => {
  let component: WalletApprovals;
  let fixture: ComponentFixture<WalletApprovals>;
  let walletServiceMock: any;
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

  const mockWallet: Wallet = {
    id: 'wal-1',
    userId: 'usr-1',
    multiCurrencyStatus: 'PENDING',
    buckets: [],
    rejectionReason: null,
    createdAt: '2026-09-19T10:00:00Z'
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
    walletServiceMock = {
      approveMultiCurrencyAccess: vi.fn().mockReturnValue(of({ ...mockWallet, multiCurrencyStatus: 'APPROVED' })),
      rejectMultiCurrencyAccess: vi.fn().mockReturnValue(of({ ...mockWallet, multiCurrencyStatus: 'REJECTED' }))
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
      imports: [WalletApprovals],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: WalletService, useValue: walletServiceMock },
        { provide: DialogService, useValue: dialogServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WalletApprovals);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('data', [mockWallet]);
    fixture.componentRef.setInput('users', [mockUser]);
    fixture.componentRef.setInput('isLoading', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch user details correctly', () => {
    const user = component.getUserDetails('usr-1');
    expect(user).toBeTruthy();
    expect(user?.fullName).toBe('John Doe');
  });

  it('should approve a wallet request and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.confirmApprove(mockWallet.id);

    expect(dialogServiceMock.open).toHaveBeenCalled();
    expect(walletServiceMock.approveMultiCurrencyAccess).toHaveBeenCalledWith('wal-1');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Wallet upgrade approved successfully.');
    expect(processedSpy).toHaveBeenCalledWith('wal-1');
  });

  it('should reject a wallet request and emit processed event', () => {
    const processedSpy = vi.spyOn(component.processed, 'emit');

    component.openRejectModal(mockWallet.id);
    component.rejectForm.controls.reason.setValue('Insufficient history');
    component.submitRejection();

    expect(walletServiceMock.rejectMultiCurrencyAccess).toHaveBeenCalledWith('wal-1', 'Insufficient history');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Wallet upgrade rejected successfully.');
    expect(processedSpy).toHaveBeenCalledWith('wal-1');
    expect(component.showRejectModal()).toBe(false);
  });
});