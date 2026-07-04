import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { Modal } from '../modal/modal';

@Component({
  selector: 'app-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Modal],
  templateUrl: './dialog.html',
  styleUrl: './dialog.scss'
})
export class Dialog {
  readonly closeOnBackdropClick = input<boolean>(true);
  readonly closeOnEscape = input<boolean>(true);
  readonly ariaLabel = input<string>('Dialog');
  readonly titleId = input<string | undefined>(undefined);
  readonly showCloseButton = input<boolean>(true);
  readonly closed = output<void>();
}