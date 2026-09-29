import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { Card } from '../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { Modal } from '../../../../shared/components/modal/modal';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { CompanyService } from '../../../../services/company/company-service';
import { UserService } from '../../../../services/user/user-service';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';

import { CompanyRepresentativeAssignmentResponse, EligibleRepresentativeResponse } from '../../../../models/company';
import { UserProfile } from '../../../../models/user';

export interface UserDropdownOption extends DropdownOption<string> {
  email: string;
  statusText: string;
  isEligible: boolean;
  hasRole: boolean;
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    Card,
    Table,
    TableCellDirective,
    Badge,
    Button,
    Modal,
    Dropdown
  ],
  templateUrl: './team.html',
  styleUrl: './team.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Team implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly companyService = inject(CompanyService);
  private readonly userService = inject(UserService);

  readonly isLoading = signal(true);
  readonly isProcessing = signal(false);
  readonly showAddModal = signal(false);

  readonly companyId = signal<string>('');
  readonly representatives = signal<CompanyRepresentativeAssignmentResponse[]>([]);
  readonly currentUserProfile = signal<UserProfile | null>(null);
  readonly eligibleUsers = signal<EligibleRepresentativeResponse[]>([]);

  readonly isPrimaryContact = computed(() => {
    const profile = this.currentUserProfile();
    const reps = this.representatives();
    if (!profile) return false;

    const myAssignment = reps.find(r => r.userId === profile.id && r.status === 'ACTIVE');
    return myAssignment?.assignmentRole === 'PRIMARY_CONTACT';
  });

  readonly activeRepsCount = computed(() => {
    return this.representatives().filter(r => r.status === 'ACTIVE').length;
  });

  readonly availableUsersOptions = computed<UserDropdownOption[]>(() => {
    const activeReps = new Set(
      this.representatives()
        .filter(r => r.status === 'ACTIVE')
        .map(r => r.userId)
    );

    const mappedOptions = this.eligibleUsers().map(u => {
      const isAlreadyAssigned = activeReps.has(u.id);
      const isActive = u.accountStatus === 'ACTIVE';
      const isEligible = isActive && !isAlreadyAssigned;

      let statusText = 'Eligible';
      if (isAlreadyAssigned) {
        statusText = 'Already Assigned';
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
        hasRole: true
      };
    });

    mappedOptions.sort((a, b) => {
      if (a.isEligible && !b.isEligible) return -1;
      if (!a.isEligible && b.isEligible) return 1;
      return a.label.localeCompare(b.label);
    });

    return mappedOptions;
  });

  readonly columns = signal<TableColumn<CompanyRepresentativeAssignmentResponse>[]>([
    { key: 'user', header: 'Representative' },
    { key: 'userId', header: 'User ID' },
    { key: 'assignmentRole', header: 'Role' },
    { key: 'status', header: 'Status' },
    { key: 'actions', header: '', align: 'right', width: '280px' }
  ]);

  readonly addRepForm = this.fb.nonNullable.group({
    userId: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('companyId') || this.route.snapshot.paramMap.get('companyId');
    if (id) {
      this.companyId.set(id);
      this.loadTeamData();
    }
  }

  loadTeamData(): void {
    this.isLoading.set(true);
    forkJoin({
      profile: this.userService.getProfile(),
      reps: this.companyService.getRepresentatives(this.companyId()),
      eligibleUsers: this.companyService.getEligibleRepresentatives()
    }).subscribe({
      next: (data) => {
        this.currentUserProfile.set(data.profile);
        this.representatives.set(data.reps);
        this.eligibleUsers.set(data.eligibleUsers);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  openAddModal(): void {
    this.addRepForm.reset();
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.addRepForm.reset();
  }

  submitAddRep(): void {
    if (this.addRepForm.invalid) {
      this.addRepForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    this.companyService.assignRepresentative(this.companyId(), this.addRepForm.controls.userId.value).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.closeAddModal();
        this.toast.success('Representative added successfully.');
        this.loadTeamData();
      },
      error: () => this.isProcessing.set(false)
    });
  }

  confirmRevoke(targetUserId: string, name: string): void {
    this.dialog.open({
      title: 'Revoke Access',
      message: `Are you sure you want to revoke dashboard access for ${name}? They will no longer be able to view or manage company data.`,
      primaryLabel: 'Revoke Access',
      primaryVariant: 'danger',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.companyService.revokeRepresentative(this.companyId(), targetUserId).subscribe(() => {
          this.toast.success('Access revoked successfully.');
          this.loadTeamData();
        });
      }
    });
  }

  confirmTransfer(targetUserId: string, name: string): void {
    this.dialog.open({
      title: 'Transfer Primary Contact',
      message: `Are you sure you want to transfer the Primary Contact role to ${name}? You will be demoted to a standard Manager and lose administrative capabilities.`,
      primaryLabel: 'Transfer Role',
      primaryVariant: 'warning',
      secondaryLabel: 'Cancel',
      isBlocking: true,
      onPrimary: () => {
        this.companyService.transferPrimaryContact(this.companyId(), targetUserId).subscribe(() => {
          this.toast.success('Primary Contact role transferred successfully.');
          this.loadTeamData();
        });
      }
    });
  }

  copyId(id: string): void {
    navigator.clipboard.writeText(id).then(() => {
      this.toast.success('User ID copied to clipboard');
    });
  }
}