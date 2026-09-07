import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { FundManager } from './fund-manager';
import { WalletService } from '../../../services/wallet/wallet-service';
import { ForexService } from '../../../services/forex/forex-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../shared/components/toast/toast.service';

describe('FundManager', () => {
  let component: FundManager;
  let fixture: ComponentFixture<FundManager>;

  beforeEach(async () => {
    const mockWalletService = {
      wallet: signal({ multiCurrencyStatus: 'APPROVED', buckets: [] }),
      loadWallet: vi.fn(),
      deposit: vi.fn().mockReturnValue(of({})),
      convert: vi.fn().mockReturnValue(of({}))
    };

    const mockForexService = {
      getSupportedCurrencies: vi.fn().mockReturnValue(of(['INR', 'USD'])),
      getExchangeRate: vi.fn().mockReturnValue(of(83.5))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR', leverage: 5 }),
      loadTradingAccount: vi.fn()
    };

    const mockToastService = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [FundManager],
      providers: [
        { provide: WalletService, useValue: mockWalletService },
        { provide: ForexService, useValue: mockForexService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FundManager);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update activeMode and emit modeChange', () => {
    const emitSpy = vi.spyOn(component.modeChange, 'emit');
    component.setMode('convert');
    expect(component.activeMode()).toBe('convert');
    expect(emitSpy).toHaveBeenCalledWith('convert');
  });

  it('should add preset values to deposit amount', () => {
    component.depositAmount.set(1000);
    component.addDepositPreset(5000);
    expect(component.depositAmount()).toBe(6000);
  });

  it('should swap source and target currencies', () => {
    component.sourceCurrency.set('INR');
    component.targetCurrency.set('USD');

    component.swapCurrencies();

    expect(component.sourceCurrency()).toBe('USD');
    expect(component.targetCurrency()).toBe('INR');
  });
});