import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Card } from '../../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective } from '../../../../../shared/components/table/table';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { Toggle } from '../../../../../shared/components/toggle/toggle';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { CompanyService } from '../../../../../services/company/company-service';
import { UserService } from '../../../../../services/user/user-service';
import { CompanyResponse, CompanyRepresentativeAssignmentResponse } from '../../../../../models/company';
import { UserListResponse } from '../../../../../models/user';
import { CountryNamePipe } from '../../../../../shared/pipes/country-name-pipe';

export interface UserDropdownOption extends DropdownOption<string> {
  email: string;
  statusText: string;
  isEligible: boolean;
  hasRole: boolean;
}

@Component({
  selector: 'app-company-details',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    Badge,
    Button,
    Toggle,
    Dropdown,
    CountryNamePipe
  ],
  templateUrl: './company-details.html',
  styleUrl: './company-details.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CompanyDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly companyService = inject(CompanyService);
  private readonly userService = inject(UserService);

  readonly companyId = signal<string>('');
  readonly company = signal<CompanyResponse | null>(null);
  readonly representatives = signal<CompanyRepresentativeAssignmentResponse[]>([]);
  readonly users = signal<UserListResponse[]>([]);
  readonly userMap = signal<Map<string, UserListResponse>>(new Map());

  readonly isLoading = signal(true);
  readonly isChangingStatus = signal(false);
  readonly isProcessing = signal(false);

  readonly columns = signal<TableColumn<CompanyRepresentativeAssignmentResponse>[]>([
    { key: 'userId', header: 'User ID' },
    { key: 'userName', header: 'Name' },
    { key: 'email', header: 'Email' },
    { key: 'assignmentRole', header: 'Role' },
    { key: 'status', header: 'Status' },
    { key: 'actions', header: '', align: 'right', width: '220px' }
  ]);

  readonly assignForm = this.fb.nonNullable.group({
    userId: ['', Validators.required]
  });

  readonly availableUsersOptions = computed<UserDropdownOption[]>(() => {
    const activeReps = new Set(
      this.representatives()
        .filter(r => r.status === 'ACTIVE')
        .map(r => r.userId)
    );

    const mappedOptions = this.users().map(u => {
      const isAlreadyAssigned = activeReps.has(u.id);
      const hasRole = u.role === 'COMPANY_REPRESENTATIVE';
      const isActive = u.accountStatus === 'ACTIVE';
      const isEligible = hasRole && isActive && !isAlreadyAssigned;

      let statusText = 'Eligible';
      if (isAlreadyAssigned) {
        statusText = 'Already Assigned';
      } else if (!hasRole) {
        statusText = 'Requires Representative Role';
      } else if (!isActive) {
        statusText = 'Account Inactive';
      }

      return {
        label: `${u.fullName} (@${u.username})`,
        value: u.id,
        disabled: !isEligible,
        searchText: `${u.fullName} ${u.username} ${u.email} ${u.id}`,
        email: u.email,
        statusText: statusText,
        isEligible: isEligible,
        hasRole: hasRole
      };
    });

    mappedOptions.sort((a, b) => {
      if (a.isEligible && !b.isEligible) return -1;
      if (!a.isEligible && b.isEligible) return 1;

      if (a.hasRole && !b.hasRole) return -1;
      if (!a.hasRole && b.hasRole) return 1;

      return a.label.localeCompare(b.label);
    });

    return mappedOptions;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.companyId.set(id);
      this.loadData(id);
    }
  }

  loadData(id: string): void {
    this.isLoading.set(true);
    forkJoin({
      company: this.companyService.getCompany(id),
      reps: this.companyService.getRepresentatives(id),
      users: this.userService.getAllUsers()
    }).subscribe({
      next: ({ company, reps, users }) => {
        const map = new Map<string, UserListResponse>();
        users.forEach(u => map.set(u.id, u));
        this.userMap.set(map);
        this.users.set(users);
        this.company.set(company);
        this.representatives.set(reps);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  loadRepresentatives(): void {
    this.companyService.getRepresentatives(this.companyId()).subscribe({
      next: (reps) => {
        this.representatives.set(reps);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/app/admin/companies']);
  }

  goToUser(row: CompanyRepresentativeAssignmentResponse): void {
    this.router.navigate(['/app/admin/users'], { queryParams: { id: row.userId } });
  }

  toggleStatus(isActive: boolean): void {
    const newStatus = isActive ? 'ACTIVE' : 'INACTIVE';
    this.isChangingStatus.set(true);
    this.companyService.changeStatus(this.companyId(), newStatus).subscribe({
      next: (res) => {
        this.company.set(res);
        this.isChangingStatus.set(false);
        this.toast.success('Company status updated.');
      },
      error: () => {
        this.isChangingStatus.set(false);
      }
    });
  }

  assignRep(): void {
    if (this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      return;
    }
    this.isProcessing.set(true);
    this.companyService.assignRepresentative(this.companyId(), this.assignForm.controls.userId.value).subscribe({
      next: () => {
        this.assignForm.reset();
        this.loadRepresentatives();
        this.isProcessing.set(false);
        this.toast.success('Representative assigned successfully.');
      },
      error: () => {
        this.isProcessing.set(false);
      }
    });
  }

  transferPrimary(userId: string): void {
    this.dialog.open({
      title: 'Transfer Primary Contact',
      message: 'Are you sure you want to make this user the primary contact?',
      primaryLabel: 'Transfer',
      primaryVariant: 'primary',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.isProcessing.set(true);
        this.companyService.transferPrimaryContact(this.companyId(), userId).subscribe({
          next: () => {
            this.loadRepresentatives();
            this.isProcessing.set(false);
            this.toast.success('Primary contact transferred successfully.');
          },
          error: () => {
            this.isProcessing.set(false);
          }
        });
      }
    });
  }

  revokeRep(userId: string): void {
    this.dialog.open({
      title: 'Revoke Access',
      message: 'Are you sure you want to revoke this representative? They will no longer have access to company management.',
      primaryLabel: 'Revoke',
      primaryVariant: 'danger',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.isProcessing.set(true);
        this.companyService.revokeRepresentative(this.companyId(), userId).subscribe({
          next: () => {
            this.loadRepresentatives();
            this.isProcessing.set(false);
            this.toast.success('Access revoked successfully.');
          },
          error: () => {
            this.isProcessing.set(false);
          }
        });
      }
    });
  }

  copyId(id: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(id).then(() => {
        this.toast.success('ID copied to clipboard');
      });
    }
  }
}