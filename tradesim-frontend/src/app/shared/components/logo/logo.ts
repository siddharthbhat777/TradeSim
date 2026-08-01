import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  templateUrl: './logo.html',
  styleUrl: './logo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Logo {
  size = input<'small' | 'medium' | 'large'>('medium');
  showText = input<boolean>(true);
}