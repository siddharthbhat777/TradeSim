import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Users } from './users';
import { UserService } from '../../../../services/user/user-service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { UserListResponse } from '../../../../models/user';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Users', () => {
  let component: Users;
  let fixture: ComponentFixture<Users>;
  let userServiceSpy: any;
  let dialogServiceSpy: any;
  let toastServiceSpy: any;
  let routerSpy: any;
  let routeSpy: any;

  const mockUsers: UserListResponse[] = [
    { id: 'u-1', fullName: 'Alice Smith', username: 'alice', email: 'alice@test.com', role: 'USER', accountStatus: 'ACTIVE', countryCode: 'US', createdAt: '2023-01-01T00:00:00Z', lastLogin: null },
    { id: 'u-2', fullName: 'Bob Jones', username: 'bob', email: 'bob@test.com', role: 'COMPANY_REPRESENTATIVE', accountStatus: 'ACTIVE', countryCode: 'UK', createdAt: '2023-01-02T00:00:00Z', lastLogin: null },
    { id: 'u-3', fullName: 'Charlie Brown', username: 'charlie', email: 'charlie@test.com', role: 'USER', accountStatus: 'SUSPENDED', countryCode: 'IN', createdAt: '2023-01-03T00:00:00Z', lastLogin: null }
  ];

  beforeEach(async () => {
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })));

    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    userServiceSpy = {
      getAllUsers: vi.fn().mockReturnValue(of(mockUsers)),
      changeRole: vi.fn(),
      changeStatus: vi.fn()
    };

    dialogServiceSpy = { open: vi.fn() };
    toastServiceSpy = { success: vi.fn(), danger: vi.fn() };

    routerSpy = { navigate: vi.fn() };

    routeSpy = {
      queryParams: of({ id: 'u-2' })
    };

    await TestBed.configureTestingModule({
      imports: [Users],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: DialogService, useValue: dialogServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: routeSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Users);
    component = fixture.componentInstance;
  });

  it('should create the component and load users', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(userServiceSpy.getAllUsers).toHaveBeenCalled();
    expect(component.users().length).toBe(3);
  });

  it('should capture highlightedId from query params and instantly wipe the URL', () => {
    fixture.detectChanges();

    expect(component.highlightedId()).toBe('u-2');

    expect(routerSpy.navigate).toHaveBeenCalledWith([], {
      relativeTo: routeSpy,
      queryParams: {},
      replaceUrl: true
    });
  });

  it('should bring the highlighted user to the very top (index 0) of filteredUsers', () => {
    fixture.detectChanges();

    const filtered = component.filteredUsers();

    expect(filtered.length).toBe(3);
    expect(filtered[0].id).toBe('u-2');
    expect(filtered[0].fullName).toBe('Bob Jones');
  });

  it('should open confirmation dialog and call API when changing a user role', () => {
    fixture.detectChanges();

    dialogServiceSpy.open.mockImplementation((config: any) => config.onPrimary());

    userServiceSpy.changeRole.mockReturnValue(of({ role: 'COMPANY_REPRESENTATIVE' }));

    component.changeRole(mockUsers[0], 'COMPANY_REPRESENTATIVE');

    expect(dialogServiceSpy.open).toHaveBeenCalled();
    expect(userServiceSpy.changeRole).toHaveBeenCalledWith('u-1', { role: 'COMPANY_REPRESENTATIVE' });
    expect(toastServiceSpy.success).toHaveBeenCalled();
  });

  it('should not hijack sorting if the user starts typing a search query', () => {
    fixture.detectChanges();

    component.searchQuery.set('Alice');

    const filtered = component.filteredUsers();

    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe('u-1');
  });
});