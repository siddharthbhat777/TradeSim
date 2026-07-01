import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';

export type CardVariant =
  | 'default'
  | 'surface'
  | 'outline'
  | 'accent'
  | 'success'
  | 'danger'
  | 'warning';

export type CardBorder = 'primary' | 'secondary' | 'accent' | 'success' | 'danger' | 'warning';

@Component({
  selector: 'app-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './card.html',
  styleUrl: './card.scss'
})
export class CardComponent {
  readonly variant = input<CardVariant>('default');

  readonly border = input<CardBorder | undefined>(undefined);

  readonly hoverable = input<boolean>(false);

  readonly onCardClick = input<((event: MouseEvent | KeyboardEvent) => void) | undefined>(
    undefined,
  );

  protected readonly resolvedBorder = computed(() => {
    const variant = this.variant();
    const border = this.border();

    if (!border) {
      return undefined;
    }

    if (variant !== 'default') {
      if (typeof ngDevMode === 'undefined' || ngDevMode) {
        console.warn(
          `[CardComponent] "border" is only applied when variant is "default". ` +
          `Received variant="${variant}" with border="${border}" — the border was ignored.`,
        );
      }
      return undefined;
    }

    return border;
  });

  protected readonly combinedClasses = computed(() => {
    const classes = [`card--${this.variant()}`];
    const border = this.resolvedBorder();
    if (border) {
      classes.push('card--bordered', `card--border-${border}`);
    }
    return classes.join(' ');
  });

  protected handleClick(event?: Event): void {
    const handler = this.onCardClick();
    if (!handler) {
      return;
    }
    if (event instanceof KeyboardEvent && event.code === 'Space') {
      event.preventDefault();
    }
    handler(event as MouseEvent | KeyboardEvent);
  }
}