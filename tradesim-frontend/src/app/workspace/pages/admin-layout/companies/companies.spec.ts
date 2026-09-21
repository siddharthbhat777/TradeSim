import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Companies } from './companies';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AuthService } from '../../../../services/auth/auth-service';
import { CompanyService } from '../../../../services/company/company-service';
import { of } from 'rxjs';

describe('Companies', () => {
  let component: Companies;
  let fixture: ComponentFixture<Companies>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Companies],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: CompanyService,
          useValue: {
            getCompanies: () => of([]),
            createCompany: () => of({}),
            onboardCompany: () => of({})
          }
        },
        {
          provide: AuthService,
          useValue: {
            requestOtp: () => of({})
          }
        },
        {
          provide: ToastService,
          useValue: {
            success: () => { },
            danger: () => { }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Companies);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});