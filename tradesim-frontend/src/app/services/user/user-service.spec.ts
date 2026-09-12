import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { UserService } from './user-service';
import { environment } from '../../../environment/environment';
import { UserProfile, BankBalanceResponse } from '../../models/user';
import { TradingAccountResponse } from '../../models/trading-account';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const mockProfile: UserProfile = {
    id: 'user-123',
    fullName: 'Siddharth Bhat',
    username: 'sid',
    email: 'sid@example.com',
    linkedBankName: 'HDFC Bank',
    role: 'USER',
    accountStatus: 'ACTIVE',
    themePreference: 'SYSTEM',
    countryCode: 'IN',
    lastLogin: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UserService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch user profile', () => {
    service.getProfile().subscribe((res) => {
      expect(res).toEqual(mockProfile);
      expect(res.username).toBe('sid');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/profile`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should fetch trading account', () => {
    const mockAccount: TradingAccountResponse = {
      id: 'acc-1',
      userId: 'user-123',
      baseCurrency: 'INR',
      marginLoan: 0,
      leverage: 5,
      maintenanceMarginPercent: 25
    };

    service.getTradingAccount().subscribe((res) => {
      expect(res.baseCurrency).toBe('INR');
      expect(res.leverage).toBe(5);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/trading-account`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAccount);
  });

  it('should update profile', () => {
    const payload = { fullName: 'Siddharth B', linkedBankName: 'State Bank of India' };

    service.updateProfile(payload).subscribe((res) => {
      expect(res.fullName).toBe('Siddharth B');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/profile/edit`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ ...mockProfile, ...payload });
  });

  it('should initiate email change', () => {
    service.initiateEmailChange('new@example.com').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/email/change/initiate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ newEmail: 'new@example.com' });
    req.flush(null);
  });

  it('should verify email change', () => {
    service.verifyEmailChange({ newEmail: 'new@example.com', otp: '123456' }).subscribe((res) => {
      expect(res.email).toBe('new@example.com');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/email/change/verify`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ newEmail: 'new@example.com', otp: '123456' });
    req.flush({ ...mockProfile, email: 'new@example.com' });
  });

  it('should reveal bank balance on password verification', () => {
    const mockBalance: BankBalanceResponse = { bankBalance: 75000 };

    service.revealBankBalance('secretPass@123').subscribe((res) => {
      expect(res.bankBalance).toBe(75000);
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/profile/bank-balance`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ password: 'secretPass@123' });
    req.flush(mockBalance);
  });

  it('should change password', () => {
    service.changePassword({ currentPassword: 'OldPass@123', newPassword: 'NewPass@123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/password/change`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      currentPassword: 'OldPass@123',
      newPassword: 'NewPass@123'
    });
    req.flush(null);
  });
});