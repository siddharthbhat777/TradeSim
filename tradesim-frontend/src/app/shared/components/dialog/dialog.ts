import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Modal } from '../modal/modal';
import { DialogService } from './dialog.service';

@Component({
  selector: 'app-dialog',
  imports: [Modal],
  templateUrl: './dialog.html',
  styleUrl: './dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dialog {
  protected readonly dialogService = inject(DialogService);

  protected onClosed(): void {
    this.dialogService.close();
  }

  protected onPrimary(): void {
    this.dialogService.primaryAction();
  }

  protected onSecondary(): void {
    this.dialogService.secondaryAction();
  }
}