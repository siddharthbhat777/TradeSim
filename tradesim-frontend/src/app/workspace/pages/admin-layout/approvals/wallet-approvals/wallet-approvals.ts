import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Card } from '../../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective } from '../../../../../shared/components/table/table';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { Modal } from '../../../../../shared/components/modal/modal';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { EmptyState } from '../../../../../shared/components/empty-state/empty-state';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { WalletService } from '../../../../../services/wallet/wallet-service';
import { UserService } from '../../../../../services/user/user-service';
import { Wallet } from '../../../../../models/wallet';
import { TimeAgoPipe } from '../../../../../shared/pipes/time-ago-pipe';

@Component({
  selector: 'app-wallet-approvals',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Card,
    Table,
    TableCellDirective,
    Badge,
    Button,
    Modal,
    CustomInput,
    InputDirective,
    EmptyState,
    TimeAgoPipe
  ],
  templateUrl: './wallet-approvals.html',
  styleUrl: './wallet-approvals.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WalletApprovals implements OnInit {
  readonly highlightedId = input<string | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly walletService = inject(WalletService);
  private readonly userService = inject(UserService);

  readonly isLoading = signal(true);
  readonly isProcessing = signal(false);
  readonly wallets = signal<Wallet[]>([]);
  readonly userMap = signal<Map<string, string>>(new Map());

  readonly columns = signal<TableColumn<Wallet>[]>([
    { key: 'userId', header: 'User ID' },
    { key: 'userName', header: 'User Name' },
    { key: 'multiCurrencyStatus', header: 'Status' },
    { key: 'createdAt', header: 'Date Submitted' },
    { key: 'actions', header: '', align: 'right', width: '180px' }
  ]);

  readonly showRejectModal = signal(false);
  readonly rejectId = signal<string | null>(null);

  readonly rejectForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    forkJoin({
      requests: this.walletService.getPendingMultiCurrencyRequests(),
      users: this.userService.getAllUsers()
    }).subscribe({
      next: ({ requests, users }) => {
        const map = new Map<string, string>();
        users.forEach(u => map.set(u.id, u.fullName));
        this.userMap.set(map);
        this.wallets.set(requests);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  confirmApprove(id: string): void {
    this.dialog.open({
      title: 'Approve Wallet Upgrade',
      message: 'Are you sure you want to approve multi-currency access for this user? They will be able to convert funds globally.',
      primaryLabel: 'Approve Upgrade',
      secondaryLabel: 'Cancel',
      primaryVariant: 'success',
      onPrimary: () => this.executeApprove(id)
    });
  }

  private executeApprove(id: string): void {
    this.isProcessing.set(true);
    this.walletService.approveMultiCurrencyAccess(id).subscribe({
      next: () => {
        this.wallets.update(arr => arr.filter(i => i.id !== id));
        this.isProcessing.set(false);
        this.toast.success('Wallet upgrade approved successfully.');
      },
      error: () => this.isProcessing.set(false)
    });
  }

  openRejectModal(id: string): void {
    this.rejectId.set(id);
    this.rejectForm.reset();
    this.showRejectModal.set(true);
  }

  closeRejectModal(): void {
    this.showRejectModal.set(false);
    this.rejectId.set(null);
    this.rejectForm.reset();
  }

  submitRejection(): void {
    if (this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }

    const id = this.rejectId();
    if (!id) return;

    this.isProcessing.set(true);
    this.walletService.rejectMultiCurrencyAccess(id, this.rejectForm.controls.reason.value).subscribe({
      next: () => {
        this.wallets.update(arr => arr.filter(i => i.id !== id));
        this.isProcessing.set(false);
        this.closeRejectModal();
        this.toast.success('Wallet upgrade rejected successfully.');
      },
      error: () => this.isProcessing.set(false)
    });
  }

  copyId(id: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(id).then(() => {
        this.toast.success('User ID copied to clipboard');
      });
    }
  }

  isHighlighted(id: string): boolean {
    return this.highlightedId() === id;
  }
}