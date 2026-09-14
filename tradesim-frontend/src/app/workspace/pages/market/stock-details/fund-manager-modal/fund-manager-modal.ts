import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { Modal } from '../../../../../shared/components/modal/modal';
import { FundManager, FundManagerMode } from '../../../../components/fund-manager/fund-manager';

@Component({
  selector: 'app-fund-manager-modal',
  imports: [Modal, FundManager],
  templateUrl: './fund-manager-modal.html',
  styleUrl: './fund-manager-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FundManagerModal {
  readonly activeMode = input<FundManagerMode>('deposit');
  readonly targetCurrency = input<string | null>(null);
  readonly closed = output<void>();

  closeModal(): void {
    this.closed.emit();
  }
}