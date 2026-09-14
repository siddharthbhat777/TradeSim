import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Auth } from './auth';
import { AuthService } from '../../../services/auth/auth-service';
import { AuthStatus } from '../../../constants/auth';

describe('Auth', () => {
  let component: Auth;
  let fixture: ComponentFixture<Auth>;

  const mockAuthService = {
    showAuthDialog: signal({ show: true, status: AuthStatus.Login }),
    loginUser: () => { },
    registerUser: () => { },
    reactivateAccount: () => { }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Auth],
      providers: [
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Auth);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});