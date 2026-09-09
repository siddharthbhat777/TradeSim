import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormatCurrencyPipe } from '../../../pipes/format-currency-pipe';

export interface LegendItem {
  id: string;
  label: string;
  value: number;
  color: string;
}

interface CalculatedLegendItem extends LegendItem {
  percentage: number;
}

@Component({
  selector: 'app-legend',
  imports: [FormatCurrencyPipe],
  templateUrl: './legend.html',
  styleUrl: './legend.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Legend {
  readonly data = input.required<LegendItem[]>();
  readonly activeId = input<string | null>(null);
  readonly showPercentage = input<boolean>(true);
  readonly currency = input<string>('INR');
  readonly itemHover = output<string | null>();

  readonly calculatedData = computed<CalculatedLegendItem[]>(() => {
    const items = this.data();
    const total = items.reduce((sum, item) => sum + item.value, 0);

    if (total === 0) return [];

    return items.map(item => ({
      ...item,
      percentage: (item.value / total) * 100
    }));
  });

  protected onMouseEnter(id: string): void {
    this.itemHover.emit(id);
  }

  protected onMouseLeave(): void {
    this.itemHover.emit(null);
  }
}