import { ChangeDetectionStrategy, Component, computed, input, model, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Modal } from '../../../../../shared/components/modal/modal';
import { Button } from '../../../../../shared/components/button/button';
import { SegmentedControl, SegmentOption } from '../../../../../shared/components/segmented-control/segmented-control';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { Skeleton } from '../../../../../shared/components/loaders/skeleton/skeleton';

@Component({
  selector: 'app-fund-manager',
  imports: [CommonModule, FormsModule, Modal, Button, SegmentedControl, CustomInput, InputDirective, Skeleton],
  templateUrl: './fund-manager.html',
  styleUrl: './fund-manager.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FundManager {
  readonly isOpen = model<boolean>(false);
  readonly activeTab = model<'DEPOSIT' | 'CONVERT'>('DEPOSIT');

  readonly baseCurrency = input.required<string>();
  readonly targetCurrency = input.required<string>();
  readonly baseBalance = input.required<number>();
  readonly targetBalance = input.required<number>();
  readonly requiredAmount = input<number>(0);
  readonly liveConversionRate = input.required<number>();
  readonly isFetchingRate = input.required<boolean>();
  readonly isProcessingFund = input.required<boolean>();

  readonly processDeposit = output<number>();
  readonly processConversion = output<number>();

  readonly depositAmount = signal<number | null>(null);
  readonly convertAmount = signal<number | null>(null);

  readonly fundTabs = computed<SegmentOption<'DEPOSIT' | 'CONVERT'>[]>(() => {
    const tabs: SegmentOption<'DEPOSIT' | 'CONVERT'>[] = [
      { label: 'Deposit', value: 'DEPOSIT' }
    ];
    if (this.targetCurrency() !== this.baseCurrency()) {
      tabs.push({ label: 'Convert', value: 'CONVERT' });
    }
    return tabs;
  });

  readonly estimatedConvertedAmount = computed(() => {
    const amt = this.convertAmount();
    if (!amt || amt <= 0) return 0;
    return amt * this.liveConversionRate();
  });

  readonly estimatedConversionFxFee = computed(() => {
    return this.estimatedConvertedAmount() * 0.01;
  });

  onDeposit(): void {
    const rawAmt = this.depositAmount();
    const amt = Number(rawAmt);
    if (rawAmt && !isNaN(amt) && amt > 0) {
      this.processDeposit.emit(amt);
      this.depositAmount.set(null);
    }
  }

  onConvert(): void {
    const rawAmt = this.convertAmount();
    const amt = Number(rawAmt);
    if (rawAmt && !isNaN(amt) && amt > 0) {
      this.processConversion.emit(amt);
      this.convertAmount.set(null);
    }
  }
}