import { Component, input } from '@angular/core';

@Component({
  selector: 'app-inline-loader',
  templateUrl: './inline-loader.html',
  styleUrl: './inline-loader.scss'
})
export class InlineLoader {
  size = input<'small' | 'medium' | 'large'>('small');
  variant = input<'primary' | 'secondary' | 'accent' | 'success' | 'danger' | 'warning' | 'muted'>('primary');
}