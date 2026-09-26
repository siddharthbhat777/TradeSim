import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { FormArray } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { Listing } from './listing';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { ListingService } from '../../../../services/listing/listing-service';
import { UserService } from '../../../../services/user/user-service';

describe('Listing', () => {
  let component: Listing;
  let fixture: ComponentFixture<Listing>;

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

  const mockDialogService = {
    open: vi.fn()
  };

  const mockCompanyService = {
    getCompany: vi.fn(),
    getRepresentatives: vi.fn()
  };

  const mockStockService = {
    getStocks: vi.fn(),
    getSectors: vi.fn()
  };

  const mockExchangeService = {
    getExchanges: vi.fn()
  };

  const mockListingService = {
    getCompanyRequests: vi.fn(),
    submitListingRequest: vi.fn(),
    approveInternalRequest: vi.fn(),
    rejectInternalRequest: vi.fn()
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
    mockExchangeService.getExchanges.mockReturnValue(of([{ id: 'ex-1', name: 'NSE' }]));
    mockStockService.getStocks.mockReturnValue(of([]));
    mockStockService.getSectors.mockReturnValue(of(['TECHNOLOGY', 'FINANCE', 'HEALTHCARE']));
    mockListingService.getCompanyRequests.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Listing],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: ToastService, useValue: mockToastService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: CompanyService, useValue: mockCompanyService },
        { provide: StockService, useValue: mockStockService },
        { provide: ExchangeService, useValue: mockExchangeService },
        { provide: ListingService, useValue: mockListingService },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Listing);
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
    expect(component.sectors().length).toBe(3);
    expect(component.isPrimaryContact()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  describe('Hybrid Listing Cap Table Logic', () => {
    it('should calculate remaining shares correctly', () => {
      component.listingForm.patchValue({ totalShares: 1000 });

      component.addCapTableEntry();
      const capArray = component.listingForm.get('capTable') as FormArray;
      capArray.at(0).patchValue({ userId: 'user-1', quantity: 400 });

      fixture.detectChanges();

      expect(component.capTableSum()).toBe(400);
      expect(component.remainingShares()).toBe(600);
    });

    it('should trigger frontend validation error if cap table entry exceeds total shares', () => {
      component.listingForm.patchValue({ totalShares: 1000 });

      component.addCapTableEntry();
      component.addCapTableEntry();
      const capArray = component.listingForm.get('capTable') as FormArray;

      capArray.at(0).patchValue({ userId: 'user-1', quantity: 800 });
      capArray.at(1).patchValue({ userId: 'user-2', quantity: 300 });

      expect(capArray.at(1).get('quantity')?.hasError('maxExceeded')).toBe(true);

      capArray.at(1).patchValue({ quantity: 200 });
      expect(capArray.at(1).get('quantity')?.hasError('maxExceeded')).toBe(false);
    });

    it('should add and remove cap table entries', () => {
      const capArray = component.listingForm.get('capTable') as FormArray;
      expect(capArray.length).toBe(0);

      component.addCapTableEntry();
      expect(capArray.length).toBe(1);

      component.removeCapTableEntry(0);
      expect(capArray.length).toBe(0);
    });
  });

  describe('Overview Timeline Sorting', () => {
    it('should sort timeline strictly from newest to oldest', () => {
      const oldDate = new Date('2026-01-01T10:00:00Z').toISOString();
      const newDate = new Date('2026-01-10T10:00:00Z').toISOString();

      component.activeStocks.set([
        { id: 'stock-1', symbol: 'OLD_STOCK', companyName: 'Test Corp' } as any
      ]);

      component.listingRequests.set([
        { id: 'req-1', symbol: 'OLD_STOCK', status: 'APPROVED', createdAt: oldDate } as any,
        { id: 'req-2', symbol: 'NEW_REQ', status: 'PENDING_INTERNAL_REVIEW', createdAt: newDate } as any
      ]);

      const timeline = component.overviewTimeline();

      expect(timeline.length).toBe(2);
      expect(timeline[0].symbol).toBe('NEW_REQ');
      expect(timeline[0].type).toBe('REQUEST');
      expect(timeline[1].symbol).toBe('OLD_STOCK');
      expect(timeline[1].type).toBe('STOCK');
    });
  });

  describe('Form Submission', () => {
    it('should not submit if form is invalid', () => {
      component.listingForm.patchValue({ symbol: '' });
      component.submitApplication();
      expect(mockListingService.submitListingRequest).not.toHaveBeenCalled();
    });

    it('should not submit if cap table sum exceeds total shares', () => {
      component.listingForm.patchValue({
        symbol: 'TEST',
        exchangeId: 'ex-1',
        referencePrice: 10,
        sector: 'TECHNOLOGY',
        priceBandPercent: 10,
        totalShares: 100
      });

      component.addCapTableEntry();
      const capArray = component.listingForm.get('capTable') as FormArray;
      capArray.at(0).patchValue({ userId: 'user-1', quantity: 200 });

      component.submitApplication();

      expect(component.listingForm.invalid).toBe(true);
      expect(mockListingService.submitListingRequest).not.toHaveBeenCalled();
    });

    it('should submit payload successfully', () => {
      mockListingService.submitListingRequest.mockReturnValue(of({}));

      component.listingForm.patchValue({
        symbol: 'TEST',
        exchangeId: 'ex-1',
        referencePrice: 10,
        sector: 'TECHNOLOGY',
        priceBandPercent: 10,
        totalShares: 1000
      });

      component.addCapTableEntry();
      const capArray = component.listingForm.get('capTable') as FormArray;
      capArray.at(0).patchValue({ userId: 'user-1', quantity: 500 });

      component.submitApplication();

      expect(component.isSubmitting()).toBe(false);
      expect(mockToastService.success).toHaveBeenCalledWith('Listing application submitted successfully.');
      expect(mockListingService.submitListingRequest).toHaveBeenCalled();
    });
  });
});