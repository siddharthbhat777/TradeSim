import { Component, input, model, signal, ElementRef, viewChild } from '@angular/core';

@Component({
  selector: 'app-checkbox',
  standalone: true,
  templateUrl: './checkbox.html',
  styleUrl: './checkbox.scss',
})
export class Checkbox {
  checked = model<boolean>(false);

  indeterminate = input<boolean>(false);
  disabled = input<boolean>(false);
  required = input<boolean>(false);
  invalid = input<boolean>(false);
  labelPosition = input<'left' | 'right'>('right');

  touched = signal<boolean>(false);

  nativeInput = viewChild<ElementRef<HTMLInputElement>>('inputElement');

  toggle(event: Event) {
    if (this.disabled()) return;

    this.touched.set(true);
    const inputElement = event.target as HTMLInputElement;
    this.checked.set(inputElement.checked);
  }

  markAsTouched() {
    this.touched.set(true);
  }
}