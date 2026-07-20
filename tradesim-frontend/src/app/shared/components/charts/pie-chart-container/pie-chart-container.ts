import { Component, input, signal } from '@angular/core';
import { PieChart, PieChartData } from './pie-chart/pie-chart';
import { Legend } from '../legend/legend';

@Component({
  selector: 'app-pie-chart-container',
  standalone: true,
  imports: [PieChart, Legend],
  templateUrl: './pie-chart-container.html',
  styleUrl: './pie-chart-container.scss'
})
export class PieChartContainer {
  readonly data = input.required<PieChartData[]>();
  readonly shape = input<'full' | 'half'>('full');
  readonly chartStyle = input<'solid' | 'donut'>('solid');
  readonly size = input<number>(200);
  readonly legendPosition = input<'top' | 'bottom' | 'left' | 'right'>('right');
  readonly wrap = input<boolean>(true);

  readonly activeSliceId = signal<string | null>(null);

  protected onSliceHover(id: string | null): void {
    this.activeSliceId.set(id);
  }

  protected onLegendHover(id: string | null): void {
    this.activeSliceId.set(id);
  }
}