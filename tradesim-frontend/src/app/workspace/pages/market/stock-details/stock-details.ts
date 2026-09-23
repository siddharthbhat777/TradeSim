import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Stock } from '../../../../models/stock';
import { OrderService } from '../../../../services/order/order-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { StockService } from '../../../../services/stock/stock-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Card } from '../../../../shared/components/card/card';
import { CandlestickChart, CandlestickData } from '../../../../shared/components/charts/candlestick-chart/candlestick-chart';
import { Drawer } from '../../../../shared/components/drawer/drawer';
import { Button } from '../../../../shared/components/button/button';
import { OrderEstimateResponse } from '../../../../models/order';

import { StockHeader } from './stock-header/stock-header';
import { OrderTicket, OrderTicketPayload } from './order-ticket/order-ticket';
import { FundManagerMode } from '../../../components/fund-manager/fund-manager';
import { OrderReviewModal } from './order-review-modal/order-review-modal';
import { FundManagerModal } from './fund-manager-modal/fund-manager-modal';

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
    FundManagerModal,
    OrderReviewModal
  ],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly stockService = inject(StockService);
  private readonly orderService = inject(OrderService);
  readonly walletService = inject(WalletService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly forexService = inject(ForexService);
  private readonly toastService = inject(ToastService);

  readonly stock = signal<Stock | null>(null);
  readonly isLoading = signal<boolean>(true);

  readonly supportedCurrencies = signal<string[]>([]);
  readonly chartData = signal<CandlestickData[]>([]);

  readonly activeOrderPayload = signal<OrderTicketPayload | null>(null);

  readonly showReviewModal = signal<boolean>(false);
  readonly showSuccessDrawer = signal<boolean>(false);
  readonly showFundModal = signal<boolean>(false);

  readonly fundManagerMode = signal<FundManagerMode>('deposit');
  readonly fundTargetCurrency = signal<string>('INR');

  readonly isEstimatingOrder = signal<boolean>(false);
  readonly isSubmittingOrder = signal<boolean>(false);
  readonly isFetchingRate = signal<boolean>(false);

  readonly orderEstimate = signal<OrderEstimateResponse | null>(null);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly baseBalance = computed(() => {
    const wallet = this.walletService.wallet();
    const bucket = wallet?.buckets.find(b => b.currency === this.baseCurrency());
    return bucket ? bucket.availableBalance : 0;
  });

  constructor() {
    effect(() => {
      const currentStock = this.stock();
      if (currentStock) {
        this.generateHistoricalData(currentStock.currentPrice);
      }
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadStock(id);
    }

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

  loadStock(id: string): void {
    this.isLoading.set(true);

    const navState = history.state;
    if (navState && navState.stock && navState.stock.id === id) {
      this.stock.set(navState.stock);
      this.isLoading.set(false);
      return;
    }

    this.stockService.getStock(id).subscribe({
      next: (data) => {
        this.stock.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.danger('Failed to load stock details.');
        this.onBack();
      }
    });
  }

  onBack(): void {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  onReviewOrder(payload: OrderTicketPayload): void {
    const currentStock = this.stock();
    if (!currentStock) return;

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
      stockId: currentStock.id,
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
    const currentStock = this.stock();
    if (!payload || !currentStock) return;

    this.isSubmittingOrder.set(true);

    this.orderService.createOrder({
      stockId: currentStock.id,
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
    this.fundManagerMode.set(this.baseBalance() > 0 && target !== base ? 'convert' : 'deposit');
    this.showFundModal.set(true);
  }

  onFundModalClosed(): void {
    this.showFundModal.set(false);
    this.refreshEstimateAfterFunding();
  }

  private refreshEstimateAfterFunding(): void {
    const currentStock = this.stock();
    if (!this.orderEstimate() || !this.activeOrderPayload() || !currentStock) {
      return;
    }

    const p = this.activeOrderPayload()!;
    this.isEstimatingOrder.set(true);

    this.orderService.estimateOrder({
      stockId: currentStock.id,
      quantity: p.orderQuantity,
      side: p.orderSide,
      orderType: p.orderType,
      timeInForce: p.timeInForce,
      limitPrice: p.orderType === 'LIMIT' ? p.limitPrice : null,
      fundingCurrency: p.fundingCurrency
    }).subscribe({
      next: (estimate) => {
        this.orderEstimate.set(estimate);
        this.isEstimatingOrder.set(false);
      },
      error: () => {
        this.isEstimatingOrder.set(false);
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