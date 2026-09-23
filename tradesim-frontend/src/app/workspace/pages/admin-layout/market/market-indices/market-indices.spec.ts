import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { MarketIndices } from './market-indices';

describe('MarketIndices', () => {
  let component: MarketIndices;
  let fixture: ComponentFixture<MarketIndices>;

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
      imports: [MarketIndices],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MarketIndices);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('indices', []);
    fixture.componentRef.setInput('stocks', []);
    fixture.componentRef.setInput('exchanges', []);
    fixture.componentRef.setInput('selectedExchangeId', null);
    fixture.componentRef.setInput('fallbackCurrency', 'USD');
    fixture.componentRef.setInput('constituents', new Map());
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});