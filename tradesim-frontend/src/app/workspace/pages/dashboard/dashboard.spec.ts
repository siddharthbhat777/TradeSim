import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { Dashboard } from './dashboard';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  const mockPortfolioService = {
    portfolio: signal(null),
    loadPortfolio: vi.fn()
  };

  const mockWalletService = {
    wallet: signal(null),
    loadWallet: vi.fn()
  };

  const mockTradingAccountService = {
    tradingAccount: signal(null),
    loadTradingAccount: vi.fn()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        { provide: PortfolioService, useValue: mockPortfolioService },
        { provide: WalletService, useValue: mockWalletService },
        { provide: TradingAccountService, useValue: mockTradingAccountService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});