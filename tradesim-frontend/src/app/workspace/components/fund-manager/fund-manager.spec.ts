import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FundManager } from './fund-manager';
import { WalletService } from '../../../services/wallet/wallet-service';
import { ForexService } from '../../../services/forex/forex-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { of } from 'rxjs';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('FundManager', () => {
  let component: FundManager;
  let fixture: ComponentFixture<FundManager>;

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
        buckets: [{ currency: 'USD', balance: 1000, availableBalance: 1000 }]
      }),
      deposit: vi.fn().mockReturnValue(of({})),
      convert: vi.fn().mockReturnValue(of({}))
    };

    const mockForexService = {
      getSupportedCurrencies: vi.fn().mockReturnValue(of(['USD', 'EUR'])),
      getExchangeRate: vi.fn().mockReturnValue(of(1.1))
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
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FundManager);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});