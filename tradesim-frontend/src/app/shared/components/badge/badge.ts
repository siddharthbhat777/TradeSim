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
  imports: [],
  templateUrl: './badge.html',
  styleUrl: './badge.scss',
})
export class Badge {
  text = input.required<string>();
  color = input<BadgeColor>('primary');
}