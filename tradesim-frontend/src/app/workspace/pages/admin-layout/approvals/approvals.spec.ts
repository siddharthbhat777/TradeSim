import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Approvals } from './approvals';
import { ListingService } from '../../../../services/listing/listing-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { UserService } from '../../../../services/user/user-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';

describe('Approvals', () => {
  let component: Approvals;
  let fixture: ComponentFixture<Approvals>;

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
      imports: [Approvals],
      providers: [
        provideRouter([]),
        { provide: ListingService, useValue: { getPendingExchangeRequests: () => of([]) } },
        { provide: IpoService, useValue: { getPendingIpos: () => of([]) } },
        { provide: WalletService, useValue: { getPendingMultiCurrencyRequests: () => of([]) } },
        { provide: UserService, useValue: { getAllUsers: () => of([]) } },
        { provide: ToastService, useValue: { success: () => { }, danger: () => { } } },
        { provide: DialogService, useValue: { open: () => { } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Approvals);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});