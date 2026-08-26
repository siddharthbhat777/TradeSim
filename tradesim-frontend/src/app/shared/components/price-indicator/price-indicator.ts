import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';

export type PriceIndicatorSign = 'positive' | 'negative' | 'neutral';

@Component({
  selector: 'app-price-indicator',
  templateUrl: './price-indicator.html',
  styleUrl: './price-indicator.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PriceIndicator {
  readonly value = input.required<number>();
  readonly percentage = input<number | undefined>(undefined);
  readonly showPercentage = input<boolean>(true);
  readonly showArrow = input<boolean>(true);
  readonly currency = input<string>('INR');
  readonly valueDecimals = input<number>(0);
  readonly percentageDecimals = input<number>(2);

  protected readonly sign = computed<PriceIndicatorSign>(() => {
    const v = this.value();
    if (v > 0) return 'positive';
    if (v < 0) return 'negative';
    return 'neutral';
  });

  protected readonly formattedValue = computed(() =>
    new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: this.currency(),
      minimumFractionDigits: this.valueDecimals(),
      maximumFractionDigits: this.valueDecimals(),
      signDisplay: 'always'
    }).format(this.value()),
  );

  protected readonly formattedPercentage = computed(() => {
    const p = this.percentage();
    if (p === undefined) {
      return '';
    }
    return new Intl.NumberFormat(undefined, {
      style: 'percent',
      minimumFractionDigits: this.percentageDecimals(),
      maximumFractionDigits: this.percentageDecimals(),
      signDisplay: 'always'
    }).format(p / 100);
  });

  protected readonly shouldShowPercentage = computed(() => this.percentage() !== undefined && this.showPercentage());
  protected readonly shouldShowArrow = computed(() => this.showArrow() && this.sign() !== 'neutral');
}