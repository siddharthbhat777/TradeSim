import { booleanAttribute, Component, input, output } from '@angular/core';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'success'
  | 'danger'
  | 'warning'
  | 'outline';

export type ButtonType = 'button' | 'submit' | 'reset';
export type ButtonIconPosition = 'left' | 'right';

@Component({
  selector: 'app-button',
  imports: [],
  templateUrl: './button.html',
  styleUrl: './button.scss',
  host: {
    '[style.width]': 'width() || null',
    '[style.height]': 'height() || null'
  }
})
export class Button {
  text = input.required<string>();

  variant = input<ButtonVariant>('primary');
  type = input<ButtonType>('button');
  iconPosition = input<ButtonIconPosition>('left');
  width = input('');
  height = input('');
  ariaLabel = input('');

  disabled = input(false, { transform: booleanAttribute });

  buttonClick = output<MouseEvent>();

  onClick(event: MouseEvent): void {
    if (this.disabled()) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }

    this.buttonClick.emit(event);
  }
}