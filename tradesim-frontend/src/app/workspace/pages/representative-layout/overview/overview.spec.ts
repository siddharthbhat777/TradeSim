import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Overview } from './overview';
import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';

describe('Overview', () => {
  let component: Overview;
  let fixture: ComponentFixture<Overview>;

  const mockCompany = {
    id: 'cmp-1',
    name: 'Acme Corp',
    code: 'ACME',
    country: 'USA',
    status: 'ACTIVE',
    primaryContactId: 'usr-1'
  };

  const mockStock = {
    id: 'stk-1',
    symbol: 'ACME',
    companyName: 'Acme Corp',
    currentPrice: 150,
    sector: 'TECHNOLOGY',
    status: 'ACTIVE',
    dayVolume: 10000,
    marketCap: 15000000,
    marketCapCategory: 'LARGE',
    currency: 'USD',
    exchangeId: 'ex-1'
  };

  const mockExchange = {
    id: 'ex-1',
    name: 'NYSE',
    code: 'NYSE',
    countryCode: 'US',
    timezone: 'EST',
    currency: 'USD',
    marketOpenTime: '09:00',
    marketCloseTime: '16:00',
    status: 'ACTIVE'
  };

  beforeAll(() => {
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Overview],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: CompanyService,
          useValue: {
            getCompany: vi.fn().mockReturnValue(of(mockCompany)),
            getRepresentatives: vi.fn().mockReturnValue(of([]))
          }
        },
        {
          provide: StockService,
          useValue: {
            getStocks: vi.fn().mockReturnValue(of([mockStock]))
          }
        },
        {
          provide: ExchangeService,
          useValue: {
            getExchanges: vi.fn().mockReturnValue(of([mockExchange]))
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Overview);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});