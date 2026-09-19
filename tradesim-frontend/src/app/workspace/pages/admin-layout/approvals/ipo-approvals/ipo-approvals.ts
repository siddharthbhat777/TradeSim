import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { IpoService } from '../../../../../services/ipo/ipo-service';
import { IpoOfferResponse } from '../../../../../models/ipo';

@Component({
  selector: 'app-ipo-approvals',
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
  templateUrl: './ipo-approvals.html',
  styleUrl: './ipo-approvals.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IpoApprovals {
  readonly highlightedId = input<string | null>(null);
  readonly data = input.required<IpoOfferResponse[]>();
  readonly isLoading = input.required<boolean>();
  readonly processed = output<string>();

  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly ipoService = inject(IpoService);

  readonly isProcessing = signal(false);

  readonly columns = signal<TableColumn<IpoOfferResponse>[]>([
    { key: 'symbol', header: 'Symbol' },
    { key: 'issuePrice', header: 'Issue Price', align: 'right' },
    { key: 'sharesPerAllottee', header: 'Shares/Allottee', align: 'right' },
    { key: 'totalSharesOffered', header: 'Total Offered', align: 'right' },
    { key: 'status', header: 'Status' },
    { key: 'createdAt', header: 'Date Submitted' },
    { key: 'actions', header: '', align: 'right', width: '180px' }
  ]);

  readonly showRejectModal = signal(false);
  readonly rejectId = signal<string | null>(null);

  readonly rejectForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]]
  });

  confirmApprove(id: string): void {
    this.dialog.open({
      title: 'Approve IPO Offer',
      message: 'Are you sure you want to approve this IPO offer? This will open the subscription window for retail investors.',
      primaryLabel: 'Approve IPO',
      secondaryLabel: 'Cancel',
      primaryVariant: 'success',
      onPrimary: () => this.executeApprove(id)
    });
  }

  private executeApprove(id: string): void {
    this.isProcessing.set(true);
    this.ipoService.approveIpoOffer(id).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.toast.success('IPO offer approved successfully.');
        this.processed.emit(id);
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
    this.ipoService.rejectIpoOffer(id, this.rejectForm.controls.reason.value).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.closeRejectModal();
        this.toast.success('IPO offer rejected successfully.');
        this.processed.emit(id);
      },
      error: () => this.isProcessing.set(false)
    });
  }

  isHighlighted(id: string): boolean {
    return this.highlightedId() === id;
  }
}