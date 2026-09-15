import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { WalletApprovals } from './wallet-approvals';
import { WalletService } from '../../../../../services/wallet/wallet-service';
import { UserService } from '../../../../../services/user/user-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';

describe('WalletApprovals', () => {
  let component: WalletApprovals;
  let fixture: ComponentFixture<WalletApprovals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WalletApprovals],
      providers: [
        { provide: WalletService, useValue: { getPendingMultiCurrencyRequests: () => of([]) } },
        { provide: UserService, useValue: { getAllUsers: () => of([]) } },
        { provide: ToastService, useValue: { success: () => { }, danger: () => { } } },
        { provide: DialogService, useValue: { open: () => { } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WalletApprovals);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('highlightedId', null);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});