import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Allocation } from './allocation';
import { PortfolioService } from '../../../../services/portfolio/portfolio-service';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Allocation', () => {
  let component: Allocation;
  let fixture: ComponentFixture<Allocation>;

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

    const mockPortfolioService = {
      portfolio: signal({
        holdings: [
          { symbol: 'AAPL', currentValue: 5000 },
          { symbol: 'TSLA', currentValue: 3000 }
        ],
        totalCashValue: 1000,
        baseCurrency: 'USD'
      }),
      loadPortfolio: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Allocation],
      providers: [
        { provide: PortfolioService, useValue: mockPortfolioService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Allocation);
    fixture.componentRef.setInput('baseCurrency', 'USD');
    fixture.componentRef.setInput('data', []);

    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});