import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Dashboard } from './dashboard';
import { UserService } from '../../../../services/user/user-service';
import { CompanyService } from '../../../../services/company/company-service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { ListingService } from '../../../../services/listing/listing-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { UserListResponse } from '../../../../models/user';
import { CompanyResponse } from '../../../../models/company';
import { Exchange } from '../../../../models/exchange';
import { IpoOfferResponse } from '../../../../models/ipo';
import { ListingRequestResponse } from '../../../../models/listing';
import { Wallet } from '../../../../models/wallet';
import { MarketIndex } from '../../../../models/market-index';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let router: Router;

  const mockUsers: UserListResponse[] = [
    {
      id: 'usr-1',
      fullName: 'John Doe',
      username: 'johndoe',
      email: 'john@example.com',
      role: 'USER',
      accountStatus: 'ACTIVE',
      countryCode: 'US',
      lastLogin: null,
      createdAt: '2026-01-01T00:00:00Z'
    }
  ];

  const mockCompanies: CompanyResponse[] = [
    {
      id: 'cmp-1',
      name: 'Acme Corp',
      code: 'ACME',
      country: 'USA',
      status: 'ACTIVE',
      primaryContactId: null
    }
  ];

  const mockExchanges: Exchange[] = [
    {
      id: 'ex-1',
      name: 'New York Stock Exchange',
      code: 'NYSE',
      countryCode: 'US',
      timezone: 'America/New_York',
      currency: 'USD',
      marketOpenTime: '09:30:00',
      marketCloseTime: '16:00:00',
      status: 'ACTIVE'
    }
  ];

  const mockIpos: IpoOfferResponse[] = [
    {
      id: 'ipo-1',
      companyId: 'cmp-1',
      stockId: 'stk-1',
      symbol: 'ACME',
      companyName: 'Acme Corp',
      exchangeName: 'NYSE',
      submittedByUserId: 'usr-1',
      issuePrice: 100,
      sharesPerAllottee: 10,
      maxAllottees: 100,
      totalSharesOffered: 1000,
      subscriptionStartAt: '2026-09-01T00:00:00Z',
      subscriptionEndAt: '2026-09-10T00:00:00Z',
      status: 'PENDING_APPROVAL',
      reviewedByUserId: null,
      reviewedAt: null,
      finalizedByUserId: null,
      finalizedAt: null,
      rejectionReason: null,
      currency: 'USD',
      createdAt: '2026-08-25T10:00:00Z',
      updatedAt: '2026-08-25T10:00:00Z'
    }
  ];

  const mockListings: ListingRequestResponse[] = [
    {
      id: 'list-1',
      companyId: 'cmp-1',
      companyName: 'Acme Corp',
      submittedByUserId: 'usr-1',
      symbol: 'ACME',
      exchangeId: 'ex-1',
      exchangeName: 'NYSE',
      referencePrice: 150,
      sector: 'TECHNOLOGY',
      priceBandPercent: 10,
      totalShares: 1000000,
      capTable: [],
      status: 'PENDING_EXCHANGE_APPROVAL',
      reviewedByUserId: null,
      reviewedAt: null,
      approvedStockId: null,
      rejectionReason: null,
      currency: 'USD',
      createdAt: '2026-08-20T10:00:00Z',
      updatedAt: '2026-08-20T10:00:00Z'
    }
  ];

  const mockWallets: Wallet[] = [
    {
      id: 'wal-1',
      userId: 'usr-1',
      multiCurrencyStatus: 'PENDING',
      buckets: [],
      rejectionReason: null,
      createdAt: '2026-09-19T10:00:00Z'
    }
  ];

  const mockIndices: MarketIndex[] = [
    {
      id: 'idx-1',
      name: 'S&P 500',
      symbol: 'SPX',
      exchangeId: 'ex-1',
      baseValue: 1000,
      currentValue: 5500,
      change: 25,
      changePercent: 0.45,
      dayOpen: 5480,
      dayHigh: 5510,
      dayLow: 5475,
      previousClose: 5475
    }
  ];

  let userServiceMock: any;
  let companyServiceMock: any;
  let exchangeServiceMock: any;
  let ipoServiceMock: any;
  let listingServiceMock: any;
  let walletServiceMock: any;
  let marketIndexServiceMock: any;

  beforeAll(() => {
    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

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
  });

  afterAll(() => {
    vi.unstubAllGlobals();
  });

  beforeEach(async () => {
    userServiceMock = {
      getAllUsers: vi.fn().mockReturnValue(of(mockUsers))
    };
    companyServiceMock = {
      getCompanies: vi.fn().mockReturnValue(of(mockCompanies))
    };
    exchangeServiceMock = {
      getExchanges: vi.fn().mockReturnValue(of(mockExchanges))
    };
    ipoServiceMock = {
      getPendingIpos: vi.fn().mockReturnValue(of(mockIpos))
    };
    listingServiceMock = {
      getPendingExchangeRequests: vi.fn().mockReturnValue(of(mockListings))
    };
    walletServiceMock = {
      getPendingMultiCurrencyRequests: vi.fn().mockReturnValue(of(mockWallets))
    };
    marketIndexServiceMock = {
      getIndicesByExchange: vi.fn().mockReturnValue(of(mockIndices))
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceMock },
        { provide: CompanyService, useValue: companyServiceMock },
        { provide: ExchangeService, useValue: exchangeServiceMock },
        { provide: IpoService, useValue: ipoServiceMock },
        { provide: ListingService, useValue: listingServiceMock },
        { provide: WalletService, useValue: walletServiceMock },
        { provide: MarketIndexService, useValue: marketIndexServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should aggregate dashboard stats on initialization', () => {
    const stats = component.stats();
    expect(stats).toBeTruthy();
    expect(stats?.totalUsers).toBe(1);
    expect(stats?.activeCompanies).toBe(1);
    expect(stats?.activeExchanges).toBe(1);
    expect(stats?.pendingApprovals).toBe(3);
  });

  it('should select first exchange by default', () => {
    expect(component.selectedExchangeId()).toBe('ex-1');
  });

  it('should populate pending actions list', () => {
    const actions = component.pendingActions();
    expect(actions.length).toBe(3);
    expect(actions.map(a => a.type)).toContain('Listing');
    expect(actions.map(a => a.type)).toContain('IPO');
    expect(actions.map(a => a.type)).toContain('Wallet');
  });

  it('should return correct badge color according to action type', () => {
    expect(component.getBadgeColor('IPO')).toBe('accent');
    expect(component.getBadgeColor('Listing')).toBe('success');
    expect(component.getBadgeColor('Wallet')).toBe('primary');
  });

  it('should navigate to approvals with query parameters on row action click', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    const targetAction = component.pendingActions()[0];

    component.onActionClick(targetAction);

    expect(navigateSpy).toHaveBeenCalled();
    const callArgs = navigateSpy.mock.calls[0];

    expect(callArgs[0]).toEqual(['../approvals']);
    expect(callArgs[1]?.queryParams).toEqual(
      expect.objectContaining({ type: targetAction.type })
    );
  });
});