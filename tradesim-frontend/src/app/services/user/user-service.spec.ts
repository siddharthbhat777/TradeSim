import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user-service';
import { environment } from '../../../environment/environment';
import { UserProfile, BankBalanceResponse } from '../../models/user';

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

  it('should fetch all users', () => {
    service.getAllUsers().subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/users`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
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
    service.initiateEmailChange({ newEmail: 'new@example.com' }).subscribe();

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

    service.revealBankBalance({ password: 'secretPass@123' }).subscribe((res) => {
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

  it('should update theme', () => {
    service.updateTheme('DARK').subscribe((res) => {
      expect(res.themePreference).toBe('DARK');
    });

    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/profile/theme`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ theme: 'DARK' });
    req.flush({ ...mockProfile, themePreference: 'DARK' });
  });

  it('should change user status', () => {
    service.changeStatus('user-123', { status: 'BANNED' }).subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/change/user-123/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ status: 'BANNED' });
    req.flush({});
  });

  it('should change user role', () => {
    service.changeRole('user-123', { role: 'ADMIN' }).subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseURL}/users/change/user-123/role`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ role: 'ADMIN' });
    req.flush({});
  });
});