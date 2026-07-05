import { Component, input, output, effect } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss'
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