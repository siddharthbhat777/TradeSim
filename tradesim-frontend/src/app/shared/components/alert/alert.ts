import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type AlertVariant = 'danger' | 'warning' | 'success' | 'info';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.html',
  styleUrl: './alert.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Alert {
  readonly variant = input<AlertVariant>('info');
  readonly showIcon = input(true);
  readonly title = input<string>();
}