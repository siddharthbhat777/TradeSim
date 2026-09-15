import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Card } from '../../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../../shared/components/table/table';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { Modal } from '../../../../../shared/components/modal/modal';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { EmptyState } from '../../../../../shared/components/empty-state/empty-state';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { FormatCurrencyPipe } from '../../../../../shared/pipes/format-currency-pipe';
import { TimeAgoPipe } from '../../../../../shared/pipes/time-ago-pipe';
import { ListingService } from '../../../../../services/listing/listing-service';
import { UserService } from '../../../../../services/user/user-service';
import { ListingRequestResponse } from '../../../../../models/listing';

@Component({
  selector: 'app-listing-approvals',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Card,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    Badge,
    Button,
    Modal,
    CustomInput,
    InputDirective,
    EmptyState,
    FormatCurrencyPipe,
    TimeAgoPipe
  ],
  templateUrl: './listing-approvals.html',
  styleUrl: './listing-approvals.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListingApprovals implements OnInit {
  readonly highlightedId = input<string | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly listingService = inject(ListingService);
  private readonly userService = inject(UserService);

  readonly isLoading = signal(true);
  readonly isProcessing = signal(false);
  readonly listings = signal<ListingRequestResponse[]>([]);
  readonly userMap = signal<Map<string, string>>(new Map());

  readonly columns = signal<TableColumn<ListingRequestResponse>[]>([
    { key: 'symbol', header: 'Symbol' },
    { key: 'sector', header: 'Sector' },
    { key: 'referencePrice', header: 'Ref Price', align: 'right' },
    { key: 'totalShares', header: 'Total Shares', align: 'right' },
    { key: 'status', header: 'Status' },
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
      requests: this.listingService.getPendingExchangeRequests(),
      users: this.userService.getAllUsers()
    }).subscribe({
      next: ({ requests, users }) => {
        const map = new Map<string, string>();
        users.forEach(u => map.set(u.id, u.fullName));
        this.userMap.set(map);
        this.listings.set(requests);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  confirmApprove(id: string): void {
    this.dialog.open({
      title: 'Approve Listing Request',
      message: 'Are you sure you want to approve this listing? This will create a halted stock ready for an IPO.',
      primaryLabel: 'Approve Listing',
      secondaryLabel: 'Cancel',
      primaryVariant: 'success',
      onPrimary: () => this.executeApprove(id)
    });
  }

  private executeApprove(id: string): void {
    this.isProcessing.set(true);
    this.listingService.approveListingRequest(id).subscribe({
      next: () => {
        this.listings.update(arr => arr.filter(i => i.id !== id));
        this.isProcessing.set(false);
        this.toast.success('Listing approved successfully.');
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
    this.listingService.rejectListingRequest(id, this.rejectForm.controls.reason.value).subscribe({
      next: () => {
        this.listings.update(arr => arr.filter(i => i.id !== id));
        this.isProcessing.set(false);
        this.closeRejectModal();
        this.toast.success('Listing request rejected successfully.');
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