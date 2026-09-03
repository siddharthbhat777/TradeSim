import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Card } from '../../../../shared/components/card/card';
import { PieChartContainer } from '../../../../shared/components/charts/pie-chart-container/pie-chart-container';
import { PieChartData } from '../../../../shared/components/charts/pie-chart-container/pie-chart/pie-chart';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-allocation',
  imports: [Card, PieChartContainer, EmptyState],
  templateUrl: './allocation.html',
  styleUrl: './allocation.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Allocation {
  readonly data = input.required<PieChartData[]>();
}