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
        { provide: ListingService, useValue: { getPendingExchangeRequests: vi.fn().mockReturnValue(of([])) } },
        { provide: IpoService, useValue: { getPendingIpos: vi.fn().mockReturnValue(of([])) } },
        { provide: WalletService, useValue: { getPendingMultiCurrencyRequests: vi.fn().mockReturnValue(of([])) } },
        { provide: UserService, useValue: { getAllUsers: vi.fn().mockReturnValue(of([])) } },
        { provide: ToastService, useValue: { success: vi.fn(), danger: vi.fn() } },
        { provide: DialogService, useValue: { open: vi.fn() } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Approvals);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter out processed listings', () => {
    component.listings.set([{ id: 'list-1' } as any, { id: 'list-2' } as any]);
    component.onListingProcessed('list-1');
    expect(component.listings().length).toBe(1);
    expect(component.listings()[0].id).toBe('list-2');
  });

  it('should filter out processed IPOs', () => {
    component.ipos.set([{ id: 'ipo-1' } as any, { id: 'ipo-2' } as any]);
    component.onIpoProcessed('ipo-1');
    expect(component.ipos().length).toBe(1);
    expect(component.ipos()[0].id).toBe('ipo-2');
  });

  it('should filter out processed wallets', () => {
    component.wallets.set([{ id: 'wal-1' } as any, { id: 'wal-2' } as any]);
    component.onWalletProcessed('wal-1');
    expect(component.wallets().length).toBe(1);
    expect(component.wallets()[0].id).toBe('wal-2');
  });
});