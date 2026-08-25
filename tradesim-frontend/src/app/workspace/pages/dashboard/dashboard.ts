import { Component, OnInit, inject, computed } from '@angular/core';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { Summary } from './summary/summary';

@Component({
  selector: 'app-dashboard',
  imports: [Summary],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private portfolioService = inject(PortfolioService);
  private walletService = inject(WalletService);
  private tradingAccountService = inject(TradingAccountService);

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

  ngOnInit(): void {
    this.portfolioService.loadPortfolio();
    this.walletService.loadWallet();
    this.tradingAccountService.loadTradingAccount();
  }
}