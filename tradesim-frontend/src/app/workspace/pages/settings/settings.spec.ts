import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Settings } from './settings';
import { UserService } from '../../../services/user/user-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { AuthService } from '../../../services/auth/auth-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { ThemeService } from '../../../services/theme-service';

describe('Settings', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;

  const mockProfile = {
    id: '1', fullName: 'John Doe', username: 'john', email: 'john@example.com',
    linkedBankName: 'Test Bank', role: 'USER', accountStatus: 'ACTIVE',
    themePreference: 'SYSTEM', countryCode: 'IN', lastLogin: null
  };

  const mockAccount = {
    id: '1', userId: '1', baseCurrency: 'INR', marginLoan: 0, leverage: 5, maintenanceMarginPercent: 25
  };

  const mockUserService = {
    getProfile: vi.fn().mockReturnValue(of(mockProfile)),
    updateProfile: vi.fn().mockReturnValue(of(mockProfile)),
    revealBankBalance: vi.fn().mockReturnValue(of({ bankBalance: 75000 }))
  };

  const mockTradingAccountService = {
    getTradingAccount: vi.fn().mockReturnValue(of(mockAccount))
  };

  const mockAuthService = {
    currentUser: signal({ role: 'USER', username: 'john' }),
    logout: vi.fn(),
    clearSession: vi.fn()
  };

  const mockToastService = {
    success: vi.fn(),
    danger: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
  };

  const mockDialogService = {
    open: vi.fn()
  };

  const mockThemeService = {
    setTheme: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: UserService, useValue: mockUserService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastService, useValue: mockToastService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: ThemeService, useValue: mockThemeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
  });

  it('should create the Settings component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should initialize forms properly', () => {
    fixture.detectChanges();
    expect(component.profileForm).toBeDefined();
    expect(component.emailForm).toBeDefined();
    expect(component.standardPasswordForm).toBeDefined();
  });

  it('should load profile and trading account on ngOnInit', () => {
    fixture.detectChanges();

    expect(mockUserService.getProfile).toHaveBeenCalled();
    expect(mockTradingAccountService.getTradingAccount).toHaveBeenCalled();
    expect(component.profile()).toEqual(mockProfile);
    expect(component.baseCurrency()).toBe('INR');
    expect(mockThemeService.setTheme).toHaveBeenCalledWith('SYSTEM');

    expect(component.profileForm.getRawValue().fullName).toBe('John Doe');
  });

  it('should submit profile update on saveProfile', () => {
    fixture.detectChanges();

    component.profileForm.patchValue({ fullName: 'Jane Doe', linkedBankName: 'New Bank' });
    component.profileForm.markAsDirty();

    const updatedProfile = { ...mockProfile, fullName: 'Jane Doe', linkedBankName: 'New Bank' };
    mockUserService.updateProfile.mockReturnValueOnce(of(updatedProfile));

    component.saveProfile();

    expect(mockUserService.updateProfile).toHaveBeenCalledWith({ fullName: 'Jane Doe', linkedBankName: 'New Bank' });
    expect(component.profile()?.fullName).toBe('Jane Doe');
    expect(mockToastService.success).toHaveBeenCalledWith('Profile changes saved.');
  });

  it('should reveal bank balance when valid password is supplied', () => {
    fixture.detectChanges();
    component.balanceForm.patchValue({ password: 'Password123!' });

    component.revealBalance();

    expect(mockUserService.revealBankBalance).toHaveBeenCalledWith('Password123!');
    expect(component.bankBalance()).toBe(75000);
  });

  it('should switch between standard and forgot password modes', () => {
    fixture.detectChanges();
    expect(component.securityMode()).toBe('standard');

    component.openForgotPassword();
    expect(component.securityMode()).toBe('forgot');

    component.returnToStandardPassword();
    expect(component.securityMode()).toBe('standard');
  });
});