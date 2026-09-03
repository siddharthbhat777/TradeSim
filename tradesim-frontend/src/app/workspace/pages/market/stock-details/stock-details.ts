import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Stock } from '../../../../models/stock';
import { OrderService } from '../../../../services/order/order-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Card } from '../../../../shared/components/card/card';
import { CandlestickChart, CandlestickData } from '../../../../shared/components/charts/candlestick-chart/candlestick-chart';
import { Drawer } from '../../../../shared/components/drawer/drawer';
import { Button } from '../../../../shared/components/button/button';
import { OrderEstimateResponse } from '../../../../models/order';

import { StockHeader } from './stock-header/stock-header';
import { OrderTicket, OrderTicketPayload } from './order-ticket/order-ticket';
import { FundManager } from './fund-manager/fund-manager';
import { OrderReviewModal } from './order-review-modal/order-review-modal';

@Component({
  selector: 'app-stock-details',
  imports: [
    CommonModule,
    Card,
    CandlestickChart,
    Drawer,
    Button,
    StockHeader,
    OrderTicket,
    FundManager,
    OrderReviewModal
  ],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockDetails implements OnInit {
  readonly stock = input.required<Stock>();
  readonly back = output<void>();

  private readonly orderService = inject(OrderService);
  readonly walletService = inject(WalletService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly forexService = inject(ForexService);
  private readonly toastService = inject(ToastService);

  readonly supportedCurrencies = signal<string[]>([]);
  readonly chartData = signal<CandlestickData[]>([]);

  readonly activeOrderPayload = signal<OrderTicketPayload | null>(null);

  readonly showReviewModal = signal<boolean>(false);
  readonly showSuccessDrawer = signal<boolean>(false);
  readonly showFundModal = signal<boolean>(false);

  readonly activeFundTab = signal<'DEPOSIT' | 'CONVERT'>('DEPOSIT');
  readonly fundTargetCurrency = signal<string>('INR');

  readonly isEstimatingOrder = signal<boolean>(false);
  readonly isSubmittingOrder = signal<boolean>(false);
  readonly isFetchingRate = signal<boolean>(false);
  readonly isProcessingFund = signal<boolean>(false);

  readonly orderEstimate = signal<OrderEstimateResponse | null>(null);
  readonly liveConversionRate = signal<number>(1);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly baseBalance = computed(() => {
    const wallet = this.walletService.wallet();
    const bucket = wallet?.buckets.find(b => b.currency === this.baseCurrency());
    return bucket ? bucket.availableBalance : 0;
  });

  readonly targetBalance = computed(() => {
    const wallet = this.walletService.wallet();
    const bucket = wallet?.buckets.find(b => b.currency === this.fundTargetCurrency());
    return bucket ? bucket.availableBalance : 0;
  });

  readonly requiredFundingAmount = computed(() => {
    const estimate = this.orderEstimate();
    if (!estimate || estimate.hasFunds) return 0;
    const shortfall = estimate.finalTotal - this.targetBalance();
    return shortfall > 0 ? shortfall : 0;
  });

  constructor() {
    effect(() => {
      this.generateHistoricalData(this.stock().currentPrice);
    });
  }

  ngOnInit(): void {
    if (!this.walletService.wallet()) {
      this.walletService.loadWallet();
    }
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }

    this.forexService.getSupportedCurrencies().subscribe(currencies => {
      this.supportedCurrencies.set(currencies);
    });
  }

  onBack(): void {
    this.back.emit();
  }

  onReviewOrder(payload: OrderTicketPayload): void {
    if (payload.orderType === 'LIMIT' && (!payload.limitPrice || payload.limitPrice <= 0)) {
      this.toastService.danger('Please enter a valid limit price.');
      return;
    }

    if (payload.orderQuantity <= 0) {
      this.toastService.danger('Quantity must be greater than zero.');
      return;
    }

    this.activeOrderPayload.set(payload);
    this.showReviewModal.set(true);
    this.isEstimatingOrder.set(true);
    this.orderEstimate.set(null);

    this.orderService.estimateOrder({
      stockId: this.stock().id,
      quantity: payload.orderQuantity,
      side: payload.orderSide,
      orderType: payload.orderType,
      timeInForce: payload.timeInForce,
      limitPrice: payload.orderType === 'LIMIT' ? payload.limitPrice : null,
      fundingCurrency: payload.fundingCurrency
    }).subscribe({
      next: (estimate) => {
        this.orderEstimate.set(estimate);
        this.isEstimatingOrder.set(false);
      },
      error: () => {
        this.isEstimatingOrder.set(false);
        this.showReviewModal.set(false);
      }
    });
  }

  onExecuteOrder(): void {
    const payload = this.activeOrderPayload();
    if (!payload) return;

    this.isSubmittingOrder.set(true);

    this.orderService.createOrder({
      stockId: this.stock().id,
      quantity: payload.orderQuantity,
      side: payload.orderSide,
      orderType: payload.orderType,
      timeInForce: payload.timeInForce,
      limitPrice: payload.orderType === 'LIMIT' ? payload.limitPrice : null,
      fundingCurrency: payload.fundingCurrency
    }).subscribe({
      next: () => {
        this.walletService.loadWallet();
        this.isSubmittingOrder.set(false);
        this.showReviewModal.set(false);
        this.showSuccessDrawer.set(true);
      },
      error: () => {
        this.isSubmittingOrder.set(false);
      }
    });
  }

  onOpenDeposit(currency?: string): void {
    const target = currency || this.activeOrderPayload()?.fundingCurrency || this.baseCurrency();
    this.fundTargetCurrency.set(target);

    const base = this.baseCurrency();
    this.activeFundTab.set(this.baseBalance() > 0 && target !== base ? 'CONVERT' : 'DEPOSIT');
    this.showFundModal.set(true);

    if (target !== base) {
      this.isFetchingRate.set(true);
      this.forexService.getExchangeRate(base, target).subscribe({
        next: (rate) => {
          this.liveConversionRate.set(rate);
          this.isFetchingRate.set(false);
        },
        error: () => {
          this.isFetchingRate.set(false);
          this.toastService.danger('Failed to fetch live exchange rate.');
        }
      });
    } else {
      this.liveConversionRate.set(1);
    }
  }

  onProcessDeposit(amt: number): void {
    this.isProcessingFund.set(true);
    this.walletService.deposit({ amount: amt }).subscribe({
      next: () => {
        this.toastService.success(`Successfully deposited ${amt} ${this.baseCurrency()}`);
        this.walletService.loadWallet();

        if (this.fundTargetCurrency() !== this.baseCurrency()) {
          this.activeFundTab.set('CONVERT');
          this.isProcessingFund.set(false);
        } else {
          this.refreshEstimateAfterFunding();
        }
      },
      error: () => {
        this.isProcessingFund.set(false);
      }
    });
  }

  onProcessConversion(amt: number): void {
    this.isProcessingFund.set(true);
    this.walletService.convert({
      sourceCurrencyCode: this.baseCurrency(),
      targetCurrencyCode: this.fundTargetCurrency(),
      amountToConvert: amt
    }).subscribe({
      next: () => {
        this.toastService.success('Conversion successful');
        this.walletService.loadWallet();
        this.refreshEstimateAfterFunding();
      },
      error: () => {
        this.isProcessingFund.set(false);
      }
    });
  }

  private refreshEstimateAfterFunding(): void {
    if (!this.orderEstimate() || !this.activeOrderPayload()) {
      this.showFundModal.set(false);
      this.isProcessingFund.set(false);
      return;
    }

    const p = this.activeOrderPayload()!;
    this.orderService.estimateOrder({
      stockId: this.stock().id,
      quantity: p.orderQuantity,
      side: p.orderSide,
      orderType: p.orderType,
      timeInForce: p.timeInForce,
      limitPrice: p.orderType === 'LIMIT' ? p.limitPrice : null,
      fundingCurrency: p.fundingCurrency
    }).subscribe({
      next: (estimate) => {
        this.orderEstimate.set(estimate);
        if (estimate.hasFunds) {
          this.showFundModal.set(false);
        }
        this.isProcessingFund.set(false);
      },
      error: () => {
        this.isProcessingFund.set(false);
      }
    });
  }

  onSuccessDrawerClosed(): void {
    this.showSuccessDrawer.set(false);
  }

  onBackToMarket(): void {
    this.showSuccessDrawer.set(false);
    setTimeout(() => this.onBack(), 300);
  }

  private generateHistoricalData(basePrice: number): void {
    const data: CandlestickData[] = Array.from({ length: 60 }).map((_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - (60 - i));
      const variation = Math.sin(i * 0.15) * (basePrice * 0.05) + (i * (basePrice * 0.002));
      const open = Math.max(1, basePrice + variation + (Math.random() * 4 - 2));
      const high = open + Math.random() * (basePrice * 0.03) + 1;
      const low = Math.max(1, open - Math.random() * (basePrice * 0.03) - 1);
      const close = Math.min(high, Math.max(low, open + (Math.random() * 6 - 3)));
      return { time: date, open, high, low, close };
    });
    this.chartData.set(data);
  }
}