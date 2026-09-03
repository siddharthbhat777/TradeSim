import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Summary } from './summary/summary';
import { HistoryChart } from './history-chart/history-chart';
import { Allocation } from './allocation/allocation';
import { HoldingsTable } from './holdings-table/holdings-table';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { RiskService } from '../../../services/risk/risk-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { AreaChartData } from '../../../shared/components/charts/area-chart/area-chart';
import { PieChartData } from '../../../shared/components/charts/pie-chart-container/pie-chart/pie-chart';
import { RiskResponse } from '../../../models/risk';

@Component({
  selector: 'app-portfolio',
  imports: [CommonModule, Summary, HistoryChart, Allocation, HoldingsTable],
  templateUrl: './portfolio.html',
  styleUrl: './portfolio.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Portfolio implements OnInit {
  private readonly portfolioService = inject(PortfolioService);
  private readonly riskService = inject(RiskService);
  private readonly tradingAccountService = inject(TradingAccountService);

  readonly portfolio = this.portfolioService.portfolio;
  readonly historyData = signal<AreaChartData[]>([]);
  readonly riskData = signal<RiskResponse | null>(null);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  private readonly COLORS = [
    'var(--chart-1)',
    'var(--chart-2)',
    'var(--chart-3)',
    'var(--chart-4)',
    'var(--chart-5)'
  ];

  readonly exposureData = computed<PieChartData[]>(() => {
    const port = this.portfolio();
    if (!port) return [];

    const data: PieChartData[] = [];

    if (port.totalCashValue > 0) {
      data.push({
        id: 'cash',
        label: 'Cash Balance',
        value: port.totalCashValue,
        color: 'var(--success)'
      });
    }

    port.holdings.forEach((h, index) => {
      data.push({
        id: h.stockId,
        label: h.symbol,
        value: h.currentValue,
        color: this.COLORS[(index + 1) % this.COLORS.length]
      });
    });

    return data;
  });

  ngOnInit(): void {
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }

    this.portfolioService.loadPortfolio();

    this.portfolioService.getPortfolioHistory().subscribe(history => {
      this.historyData.set(history.map(item => ({
        time: item.snapshotDate,
        value: item.equity
      })));
    });

    this.riskService.getMyRisk().subscribe(risk => {
      this.riskData.set(risk);
    });
  }
}