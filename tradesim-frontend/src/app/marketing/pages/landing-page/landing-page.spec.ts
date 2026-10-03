import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { LandingPage } from './landing-page';
import { AuthService } from '../../../services/auth/auth-service';
import { ForexService } from '../../../services/forex/forex-service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { of } from 'rxjs';

describe('LandingPage', () => {
  let component: LandingPage;
  let fixture: ComponentFixture<LandingPage>;

  const mockAuthService = {
    currentUser: signal(null),
    isLoggedIn: signal(false),
    showAuthDialog: signal({ show: false, status: 'LOGIN' }),
    logout: vi.fn().mockReturnValue({
      subscribe: vi.fn((callbacks) => {
        if (callbacks && callbacks.next) {
          callbacks.next();
        }
      })
    })
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

  const mockDialogService = {
    open: vi.fn()
  };

  const mockToast = {
    success: vi.fn(),
    danger: vi.fn()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ForexService, useValue: mockForexService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: ToastService, useValue: mockToast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});