import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Dashboard } from './dashboard';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { UserService } from '../../../../services/user/user-service';
import { CompanyService } from '../../../../services/company/company-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { ListingService } from '../../../../services/listing/listing-service';
import { WalletService } from '../../../../services/wallet/wallet-service';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  const mockExchangeService = { getExchanges: vi.fn().mockReturnValue(of([])) };
  const mockMarketIndexService = { getIndicesByExchange: vi.fn().mockReturnValue(of([])) };
  const mockUserService = { getAllUsers: vi.fn().mockReturnValue(of([])) };
  const mockCompanyService = { getCompanies: vi.fn().mockReturnValue(of([])) };
  const mockIpoService = { getPendingIpos: vi.fn().mockReturnValue(of([])) };
  const mockListingService = { getPendingExchangeRequests: vi.fn().mockReturnValue(of([])) };
  const mockWalletService = { getPendingMultiCurrencyRequests: vi.fn().mockReturnValue(of([])) };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: ExchangeService, useValue: mockExchangeService },
        { provide: MarketIndexService, useValue: mockMarketIndexService },
        { provide: UserService, useValue: mockUserService },
        { provide: CompanyService, useValue: mockCompanyService },
        { provide: IpoService, useValue: mockIpoService },
        { provide: ListingService, useValue: mockListingService },
        { provide: WalletService, useValue: mockWalletService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});