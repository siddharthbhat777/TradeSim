import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CompanyDetails } from './company-details';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { CompanyService } from '../../../../../services/company/company-service';
import { UserService } from '../../../../../services/user/user-service';
import { of } from 'rxjs';

describe('CompanyDetails', () => {
  let component: CompanyDetails;
  let fixture: ComponentFixture<CompanyDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompanyDetails],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'test-company-id'
              }
            }
          }
        },
        {
          provide: CompanyService,
          useValue: {
            getCompany: () => of({ id: 'test-company-id', name: 'Test Company', code: 'TST', country: 'US', status: 'ACTIVE' }),
            getRepresentatives: () => of([]),
            changeStatus: () => of({}),
            assignRepresentative: () => of({}),
            transferPrimaryContact: () => of({}),
            revokeRepresentative: () => of({})
          }
        },
        {
          provide: UserService,
          useValue: {
            getAllUsers: () => of([])
          }
        },
        {
          provide: ToastService,
          useValue: {
            success: () => { },
            danger: () => { }
          }
        },
        {
          provide: DialogService,
          useValue: {
            open: () => { }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompanyDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});