import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ListingApprovals } from './listing-approvals';
import { ListingService } from '../../../../../services/listing/listing-service';
import { UserService } from '../../../../../services/user/user-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';

describe('ListingApprovals', () => {
  let component: ListingApprovals;
  let fixture: ComponentFixture<ListingApprovals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListingApprovals],
      providers: [
        { provide: ListingService, useValue: { getPendingExchangeRequests: () => of([]) } },
        { provide: UserService, useValue: { getAllUsers: () => of([]) } },
        { provide: ToastService, useValue: { success: () => { }, danger: () => { } } },
        { provide: DialogService, useValue: { open: () => { } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListingApprovals);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('highlightedId', null);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});