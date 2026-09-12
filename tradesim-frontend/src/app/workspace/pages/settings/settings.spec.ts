import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Settings } from './settings';
import { UserService } from '../../../services/user/user-service';
import { AuthService } from '../../../services/auth/auth-service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { UserProfile } from '../../../models/user';

describe('Settings', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;

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

  const mockTradingAccount = {
    baseCurrency: 'INR'
  };

  const mockUserService = {
    getProfile: () => of(mockProfile),
    getTradingAccount: () => of(mockTradingAccount),
    updateProfile: () => of({ ...mockProfile, fullName: 'Updated Name' }),
    initiateEmailChange: () => of(void 0),
    verifyEmailChange: () => of({ ...mockProfile, email: 'new@example.com' }),
    revealBankBalance: () => of({ bankBalance: 50000 }),
    changePassword: () => of(void 0)
  };

  const mockAuthService = {
    clearSession: () => { },
    requestOtp: () => of(void 0),
    resetPassword: () => of(void 0),
    deactivateAccount: () => of(void 0)
  };

  const mockDialogService = {
    open: () => { }
  };

  const mockToastService = {
    success: () => { },
    danger: () => { },
    warning: () => { },
    info: () => { }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Settings, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
  });

  it('should create the Settings component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize forms properly', () => {
    expect(component.profileForm).toBeDefined();
    expect(component.emailForm).toBeDefined();
    expect(component.standardPasswordForm).toBeDefined();
    expect(component.forgotPasswordForm).toBeDefined();
    expect(component.balanceForm).toBeDefined();
    expect(component.deactivateForm).toBeDefined();
  });

  it('should load profile and trading account on ngOnInit', () => {
    component.ngOnInit();
    expect(component.profile()).toEqual(mockProfile);
    expect(component.baseCurrency()).toBe('INR');
    expect(component.profileForm.controls.fullName.value).toBe('Siddharth Bhat');
    expect(component.profileForm.controls.linkedBankName.value).toBe('HDFC Bank');
    expect(component.emailForm.controls.email.value).toBe('sid@example.com');
    expect(component.isLoading()).toBe(false);
  });

  it('should submit profile update on saveProfile', () => {
    component.ngOnInit();
    // Simulate user editing the form so it is not pristine
    component.profileForm.controls.fullName.setValue('Updated Name');
    component.profileForm.markAsDirty();

    component.saveProfile();
    expect(component.profile()?.fullName).toBe('Updated Name');
  });

  it('should reveal bank balance when valid password is supplied', () => {
    component.balanceForm.controls.password.setValue('MySecretPassword@1');
    component.revealBalance();

    expect(component.bankBalance()).toBe(50000);
    expect(component.showBalanceModal()).toBe(false);
  });

  it('should switch between standard and forgot password modes', () => {
    expect(component.securityMode()).toBe('standard');

    component.openForgotPassword();
    expect(component.securityMode()).toBe('forgot');

    component.returnToStandardPassword();
    expect(component.securityMode()).toBe('standard');
  });
});