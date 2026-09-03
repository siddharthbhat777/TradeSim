import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Card } from '../../../../shared/components/card/card';
import { AreaChart, AreaChartData } from '../../../../shared/components/charts/area-chart/area-chart';

@Component({
  selector: 'app-history-chart',
  imports: [Card, AreaChart],
  templateUrl: './history-chart.html',
  styleUrl: './history-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HistoryChart {
  readonly data = input.required<AreaChartData[]>();
  readonly baseCurrency = input.required<string>();
}