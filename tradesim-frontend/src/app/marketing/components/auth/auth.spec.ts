import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Auth } from './auth';
import { AuthService } from '../../../services/auth/auth-service';
import { ForexService } from '../../../services/forex/forex-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { AuthStatus } from '../../../constants/auth';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

describe('Auth', () => {
  let component: Auth;
  let fixture: ComponentFixture<Auth>;

  const mockAuthService = {
    showAuthDialog: signal({ show: true, status: AuthStatus.Login }),
    loginUser: vi.fn(),
    registerUser: vi.fn(),
    reactivateAccount: vi.fn(),
    requestOtp: vi.fn()
  };

  const mockForexService = {
    countries: signal([{ code: 'IN', name: 'India' }]),
    isCountriesLoaded: signal(true),
    loadCountries: vi.fn(),
    fetchCountries: vi.fn().mockReturnValue(of([{ code: 'IN', name: 'India' }])),
    getCountryName: vi.fn().mockReturnValue('India'),
    getSupportedCurrencies: vi.fn().mockReturnValue(of(['INR', 'USD'])),
    getCurrencyForCountry: vi.fn().mockReturnValue(of('INR'))
  };

  const mockToast = {
    success: vi.fn(),
    danger: vi.fn()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Auth, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: ForexService, useValue: mockForexService },
        { provide: ToastService, useValue: mockToast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Auth);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});