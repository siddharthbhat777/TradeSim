import { Component, input, output, effect, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyState {
  text = input.required<string>();
  subtext = input<string>();
  actionLabel = input<string>();
  actionClick = output<void>();

  constructor() {
    effect(() => {
      if (!this.text().trim()) {
        console.warn('[EmptyState] "text" should not be empty.');
      }
    });
  }
}