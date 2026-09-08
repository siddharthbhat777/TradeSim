import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Wallet } from './wallet';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { LedgerService } from '../../../services/ledger/ledger-service';
import { ToastService } from '../../../shared/components/toast/toast.service';

describe('Wallet', () => {
  let component: Wallet;
  let fixture: ComponentFixture<Wallet>;

  beforeEach(async () => {
    const mockWalletService = {
      wallet: signal({
        multiCurrencyStatus: 'UNREQUESTED',
        buckets: [{ currency: 'INR', balance: 10000, availableBalance: 8000, lockedBalance: 2000 }]
      }),
      loadWallet: vi.fn(),
      requestMultiCurrency: vi.fn().mockReturnValue(of({}))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR' }),
      loadTradingAccount: vi.fn()
    };

    const mockLedgerService = {
      getMyLedger: vi.fn().mockReturnValue(of([]))
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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should format ledger type correctly', () => {
    const formatted = component.formatLedgerType('BUY_LIMIT_MARGIN_LOCK');
    expect(formatted).toBe('Buy Limit Margin Lock');
  });

  it('should return correct badge color', () => {
    expect(component.getLedgerBadgeColor('DEPOSIT')).toBe('success');
    expect(component.getLedgerBadgeColor('TRADE_MARGIN_DEBIT')).toBe('danger');
    expect(component.getLedgerBadgeColor('BUY_LIMIT_MARGIN_LOCK')).toBe('warning');
  });
});