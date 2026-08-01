import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { PieChart, PieChartData } from './pie-chart/pie-chart';
import { Legend } from '../legend/legend';
import { InlineLoader } from '../../loaders/inline-loader/inline-loader';

@Component({
  selector: 'app-pie-chart-container',
  imports: [PieChart, Legend, InlineLoader],
  templateUrl: './pie-chart-container.html',
  styleUrl: './pie-chart-container.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PieChartContainer {
  readonly data = input.required<PieChartData[]>();
  readonly shape = input<'full' | 'half'>('full');
  readonly chartStyle = input<'solid' | 'donut'>('solid');
  readonly size = input<number>(200);
  readonly legendPosition = input<'top' | 'bottom' | 'left' | 'right'>('right');
  readonly wrap = input<boolean>(true);
  readonly isLoading = input<boolean>(false);

  readonly activeSliceId = signal<string | null>(null);

  protected onSliceHover(id: string | null): void {
    this.activeSliceId.set(id);
  }

  protected onLegendHover(id: string | null): void {
    this.activeSliceId.set(id);
  }
}