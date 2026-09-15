import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { IpoApprovals } from './ipo-approvals';
import { IpoService } from '../../../../../services/ipo/ipo-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';

describe('IpoApprovals', () => {
  let component: IpoApprovals;
  let fixture: ComponentFixture<IpoApprovals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IpoApprovals],
      providers: [
        { provide: IpoService, useValue: { getPendingIpos: () => of([]) } },
        { provide: ToastService, useValue: { success: () => { }, danger: () => { } } },
        { provide: DialogService, useValue: { open: () => { } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IpoApprovals);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('highlightedId', null);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});