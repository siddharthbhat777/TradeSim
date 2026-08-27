import { Component, OnInit, inject, computed } from '@angular/core';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { Summary } from './summary/summary';
import { Allocation } from './allocation/allocation';
import { HoldingsTable } from './holdings-table/holdings-table';
import { PieChartData } from '../../../shared/components/charts/pie-chart-container/pie-chart/pie-chart';

@Component({
  selector: 'app-dashboard',
  imports: [Summary, Allocation, HoldingsTable],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private portfolioService = inject(PortfolioService);
  private walletService = inject(WalletService);
  private tradingAccountService = inject(TradingAccountService);

  public baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency ?? 'INR');
  public equity = computed(() => this.portfolioService.portfolio()?.equity ?? 0);
  public totalInvested = computed(() => this.portfolioService.portfolio()?.totalInvested ?? 0);
  public unrealizedPnl = computed(() => this.portfolioService.portfolio()?.totalUnrealizedPnl ?? 0);

  public buyingPower = computed(() => {
    const baseCurrency = this.tradingAccountService.tradingAccount()?.baseCurrency;
    const buckets = this.walletService.wallet()?.buckets ?? [];

    if (!baseCurrency || buckets.length === 0) return 0;

    const baseBucket = buckets.find(b => b.currency === baseCurrency);
    return baseBucket ? baseBucket.availableBalance : 0;
  });

  public holdingsData = computed(() => this.portfolioService.portfolio()?.holdings ?? []);

  public allocationData = computed<PieChartData[]>(() => {
    const portfolio = this.portfolioService.portfolio();
    const baseCurrency = this.baseCurrency();

    const data: PieChartData[] = [];

    if (!portfolio) return data;

    const netCash = portfolio.totalCashValue - portfolio.marginLoan;

    if (netCash > 0) {
      data.push({
        id: 'cash-slice',
        label: `Cash (${baseCurrency})`,
        value: netCash,
        color: 'var(--text-muted)'
      });
    }

    if (!portfolio.holdings) return data;

    const sortedHoldings = [...portfolio.holdings].sort((a, b) => b.currentValue - a.currentValue);

    const topHoldings = sortedHoldings.slice(0, 4);
    const remainingHoldings = sortedHoldings.slice(4);
    const chartColors = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)'];

    topHoldings.forEach((holding, index) => {
      if (holding.currentValue > 0) {
        data.push({
          id: holding.stockId,
          label: holding.symbol,
          value: holding.currentValue,
          color: chartColors[index]
        });
      }
    });

    if (remainingHoldings.length > 0) {
      const othersValue = remainingHoldings.reduce((sum, h) => sum + h.currentValue, 0);
      data.push({
        id: 'others-slice',
        label: 'Other Assets',
        value: othersValue,
        color: 'var(--chart-5)'
      });
    }

    return data.sort((a, b) => b.value - a.value);
  });

  ngOnInit(): void {
    this.portfolioService.loadPortfolio();
    this.walletService.loadWallet();
    this.tradingAccountService.loadTradingAccount();
  }
}