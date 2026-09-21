import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs/operators';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../shared/components/table/table';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { CountryNamePipe } from '../../../../shared/pipes/country-name-pipe';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago-pipe';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { UserService } from '../../../../services/user/user-service';
import { UserListResponse } from '../../../../models/user';

@Component({
  selector: 'app-users',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    Badge,
    Button,
    CustomInput,
    InputDirective,
    SegmentedControl,
    Dropdown,
    EmptyState,
    CountryNamePipe,
    TimeAgoPipe
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Users implements OnInit {
  private readonly userService = inject(UserService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly users = signal<UserListResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isProcessing = signal(false);

  readonly searchQuery = signal('');
  readonly roleFilter = signal('ALL');
  readonly statusFilter = signal('ALL');
  readonly highlightedId = signal<string | null>(null);

  readonly roleOptions: SegmentOption<string>[] = [
    { label: 'All Roles', value: 'ALL' },
    { label: 'Users', value: 'USER' },
    { label: 'Representatives', value: 'COMPANY_REPRESENTATIVE' }
  ];

  readonly statusOptions: DropdownOption<string>[] = [
    { label: 'All Statuses', value: 'ALL' },
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Suspended', value: 'SUSPENDED' },
    { label: 'Banned', value: 'BANNED' },
    { label: 'Deactivated', value: 'DEACTIVATED' }
  ];

  readonly columns = signal<TableColumn<UserListResponse>[]>([
    { key: 'user', header: 'User' },
    { key: 'username', header: 'Username' },
    { key: 'country', header: 'Country' },
    { key: 'role', header: 'Role' },
    { key: 'status', header: 'Status' }
  ]);

  readonly filteredUsers = computed(() => {
    let result = this.users();

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      result = result.filter(u =>
        u.fullName.toLowerCase().includes(query) ||
        u.username.toLowerCase().includes(query) ||
        u.email.toLowerCase().includes(query) ||
        u.id.toLowerCase().includes(query)
      );
    }

    const role = this.roleFilter();
    if (role !== 'ALL') {
      result = result.filter(u => u.role === role);
    }

    const status = this.statusFilter();
    if (status !== 'ALL') {
      result = result.filter(u => u.accountStatus === status);
    }

    const sorted = [...result].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    const hId = this.highlightedId();
    if (hId && !query) {
      const highlightedIndex = sorted.findIndex(u => u.id === hId);
      if (highlightedIndex > -1) {
        const [highlightedUser] = sorted.splice(highlightedIndex, 1);
        sorted.unshift(highlightedUser);
      }
    }

    return sorted;
  });

  ngOnInit(): void {
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      if (params['id']) {
        this.highlightedId.set(params['id']);

        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true
        });
      }
    });

    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users.set(data.filter(u => u.role !== 'ADMIN'));
        this.isLoading.set(false);

        const hId = this.highlightedId();
        if (hId) {
          setTimeout(() => {
            const el = document.getElementById(`user-row-${hId}`);
            if (el) {
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
              const rowEl = el.closest('tr');
              if (rowEl) {
                rowEl.click();
              }
            }
          }, 300);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  copyId(id: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(id).then(() => {
        this.toastService.success('User ID copied to clipboard');
      });
    }
  }

  isHighlighted(id: string): boolean {
    return this.highlightedId() === id;
  }

  changeRole(user: UserListResponse, newRole: string): void {
    const isDemoting = user.role === 'COMPANY_REPRESENTATIVE' && newRole === 'USER';
    const title = isDemoting ? 'Demote to User' : 'Promote to Representative';
    const message = isDemoting
      ? `Are you sure you want to demote ${user.fullName}? If they are actively managing companies, the backend will block this action.`
      : `Promoting ${user.fullName} will allow them to be assigned to manage company listings and IPOs.`;

    this.dialogService.open({
      title: title,
      message: message,
      primaryLabel: 'Confirm',
      secondaryLabel: 'Cancel',
      primaryVariant: 'primary',
      onPrimary: () => {
        this.isProcessing.set(true);
        this.userService.changeRole(user.id, { role: newRole }).subscribe({
          next: (res) => {
            this.updateUserInList(user.id, { role: res.role });
            this.isProcessing.set(false);
            this.toastService.success(`Role updated successfully`);
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(err.error?.message || 'Failed to change role');
          }
        });
      }
    });
  }

  changeStatus(user: UserListResponse, newStatus: string): void {
    let title = 'Change Status';
    let message = '';
    let variant: 'primary' | 'danger' | 'warning' = 'primary';

    if (newStatus === 'BANNED') {
      title = 'Ban User';
      message = `Are you absolutely sure you want to BAN ${user.fullName}? This will immediately cancel all their open and partially filled orders. This action cannot be easily undone.`;
      variant = 'danger';
    } else if (newStatus === 'SUSPENDED') {
      title = 'Suspend User';
      message = `Suspending ${user.fullName} will temporarily prevent them from trading or accessing their account.`;
      variant = 'warning';
    } else {
      title = 'Reactivate User';
      message = `Are you sure you want to reactivate ${user.fullName}? They will regain full access to their account.`;
      variant = 'success' as any;
    }

    this.dialogService.open({
      title: title,
      message: message,
      primaryLabel: title,
      secondaryLabel: 'Cancel',
      primaryVariant: variant,
      onPrimary: () => {
        this.isProcessing.set(true);
        this.userService.changeStatus(user.id, { status: newStatus }).subscribe({
          next: (res) => {
            this.updateUserInList(user.id, { accountStatus: res.accountStatus });
            this.isProcessing.set(false);
            this.toastService.success(`Status updated to ${newStatus}`);
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(err.error?.message || 'Failed to change status');
          }
        });
      }
    });
  }

  private updateUserInList(id: string, updates: Partial<UserListResponse>): void {
    this.users.update(arr => arr.map(u => u.id === id ? { ...u, ...updates } : u));
  }
}