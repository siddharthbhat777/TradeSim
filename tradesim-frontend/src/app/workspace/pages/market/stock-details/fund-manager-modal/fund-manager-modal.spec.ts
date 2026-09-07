import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { WalletService } from '../../../../../services/wallet/wallet-service';
import { ForexService } from '../../../../../services/forex/forex-service';
import { TradingAccountService } from '../../../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { FundManagerModal } from './fund-manager-modal';

describe('FundManagerModal', () => {
  let component: FundManagerModal;
  let fixture: ComponentFixture<FundManagerModal>;

  beforeEach(async () => {
    const mockWalletService = {
      wallet: signal({ multiCurrencyStatus: 'APPROVED', buckets: [] }),
      loadWallet: vi.fn()
    };

    const mockForexService = {
      getSupportedCurrencies: vi.fn().mockReturnValue(of(['INR', 'USD'])),
      getExchangeRate: vi.fn().mockReturnValue(of(83.5))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR' }),
      loadTradingAccount: vi.fn()
    };

    const mockToastService = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [FundManagerModal],
      providers: [
        { provide: WalletService, useValue: mockWalletService },
        { provide: ForexService, useValue: mockForexService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FundManagerModal);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('activeMode', 'deposit');
    fixture.componentRef.setInput('targetCurrency', 'USD');

    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit closed event when closeModal is called', () => {
    const emitSpy = vi.spyOn(component.closed, 'emit');
    component.closeModal();
    expect(emitSpy).toHaveBeenCalled();
  });
});