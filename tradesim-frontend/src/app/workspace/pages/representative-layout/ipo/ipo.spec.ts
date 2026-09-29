import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { Ipo } from './ipo';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { UserService } from '../../../../services/user/user-service';

describe('Ipo', () => {
  let component: Ipo;
  let fixture: ComponentFixture<Ipo>;

  const mockActivatedRoute = {
    parent: {
      snapshot: { paramMap: { get: () => 'test-company-123' } }
    },
    snapshot: {
      paramMap: { get: () => 'test-company-123' }
    }
  };

  const mockToastService = {
    success: vi.fn(),
    danger: vi.fn()
  };

  const mockCompanyService = {
    getCompany: vi.fn(),
    getRepresentatives: vi.fn()
  };

  const mockStockService = {
    getStocks: vi.fn()
  };

  const mockIpoService = {
    getCompanyIpoOffers: vi.fn(),
    submitIpoOffer: vi.fn(),
    getSubscriptionsForOffer: vi.fn()
  };

  const mockUserService = {
    getProfile: vi.fn()
  };

  beforeEach(async () => {
    mockCompanyService.getCompany.mockReturnValue(of({ id: 'test-company-123', name: 'Test Corp' }));
    mockCompanyService.getRepresentatives.mockReturnValue(of([
      { userId: 'user-1', fullName: 'John Doe', status: 'ACTIVE', assignmentRole: 'PRIMARY_CONTACT' }
    ]));
    mockUserService.getProfile.mockReturnValue(of({ id: 'user-1' }));
    mockStockService.getStocks.mockReturnValue(of([]));
    mockIpoService.getCompanyIpoOffers.mockReturnValue(of([]));
    mockIpoService.getSubscriptionsForOffer.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Ipo],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: ToastService, useValue: mockToastService },
        { provide: CompanyService, useValue: mockCompanyService },
        { provide: StockService, useValue: mockStockService },
        { provide: IpoService, useValue: mockIpoService },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Ipo);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard data on init', () => {
    expect(component.companyId()).toBe('test-company-123');
    expect(component.company()?.name).toBe('Test Corp');
    expect(component.isPrimaryContact()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  it('should calculate total shares offered correctly', () => {
    component.ipoForm.patchValue({ sharesPerAllottee: 50, maxAllottees: 1000 });
    expect(component.totalSharesOffered()).toBe(50000);
  });

  it('should not submit if form is invalid', () => {
    component.ipoForm.patchValue({ issuePrice: 0 });
    component.submitApplication();
    expect(mockIpoService.submitIpoOffer).not.toHaveBeenCalled();
  });

  it('should submit payload successfully', () => {
    mockIpoService.submitIpoOffer.mockReturnValue(of({}));

    component.ipoForm.patchValue({
      stockId: 'stk-1',
      issuePrice: 100,
      sharesPerAllottee: 50,
      maxAllottees: 1000,
      subscriptionStartAt: '2026-09-01T10:00',
      subscriptionEndAt: '2026-09-10T10:00'
    });

    component.submitApplication();

    expect(component.isSubmitting()).toBe(false);
    expect(mockToastService.success).toHaveBeenCalledWith('IPO configuration submitted successfully.');
    expect(mockIpoService.submitIpoOffer).toHaveBeenCalled();
  });
});