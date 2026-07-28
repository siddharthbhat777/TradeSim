import { Component, input } from '@angular/core';

export type BadgeColor =
  | 'primary'
  | 'secondary'
  | 'accent'
  | 'success'
  | 'danger'
  | 'warning';

@Component({
  selector: 'app-badge',
  templateUrl: './badge.html',
  styleUrl: './badge.scss'
})
export class Badge {
  color = input<BadgeColor>('primary');
}