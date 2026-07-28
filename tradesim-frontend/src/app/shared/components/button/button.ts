import { booleanAttribute, Component, input, output } from '@angular/core';
import { InlineLoader } from '../loaders/inline-loader/inline-loader';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'success'
  | 'danger'
  | 'warning'
  | 'outline';

export type ButtonType = 'button' | 'submit' | 'reset';
export type ButtonIconPosition = 'left' | 'right';
export type ButtonSize = 'small' | 'medium' | 'large';

@Component({
  selector: 'app-button',
  imports: [InlineLoader],
  templateUrl: './button.html',
  styleUrl: './button.scss',
  host: {
    '[style.width]': 'width() || null',
    '[style.height]': 'height() || null'
  }
})
export class Button {
  variant = input<ButtonVariant>('primary');
  type = input<ButtonType>('button');
  iconPosition = input<ButtonIconPosition>('left');
  size = input<ButtonSize>('medium');
  width = input('');
  height = input('');
  ariaLabel = input('');

  disabled = input(false, { transform: booleanAttribute });
  isLoading = input(false, { transform: booleanAttribute });

  buttonClick = output<MouseEvent>();

  onClick(event: MouseEvent): void {
    if (this.disabled() || this.isLoading()) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }

    this.buttonClick.emit(event);
  }
}