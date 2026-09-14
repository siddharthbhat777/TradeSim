import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Wallet } from './wallet';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { LedgerService } from '../../../services/ledger/ledger-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { of } from 'rxjs';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Wallet', () => {
  let component: Wallet;
  let fixture: ComponentFixture<Wallet>;

  beforeEach(async () => {
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
    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    const mockWalletService = {
      wallet: signal({
        multiCurrencyStatus: 'APPROVED',
        buckets: [{ currency: 'USD', balance: 1000 }]
      }),
      loadWallet: vi.fn(),
      requestMultiCurrency: vi.fn().mockReturnValue(of({}))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({
        baseCurrency: 'USD'
      }),
      loadTradingAccount: vi.fn()
    };

    const mockLedgerService = {
      getMyLedger: vi.fn().mockReturnValue(of([
        { id: '1', type: 'DEPOSIT', amount: 100, currency: 'USD', createdAt: new Date().toISOString() }
      ]))
    };

    const mockToastService = {
      success: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Wallet],
      providers: [
        { provide: WalletService, useValue: mockWalletService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: LedgerService, useValue: mockLedgerService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Wallet);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});