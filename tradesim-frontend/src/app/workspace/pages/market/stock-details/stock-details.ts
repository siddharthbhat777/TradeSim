import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Stock } from '../../../../models/stock';
import { OrderService } from '../../../../services/order/order-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Button } from '../../../../shared/components/button/button';
import { Badge } from '../../../../shared/components/badge/badge';
import { Card } from '../../../../shared/components/card/card';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { CandlestickChart, CandlestickData } from '../../../../shared/components/charts/candlestick-chart/candlestick-chart';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { NumberStepper } from '../../../../shared/components/number-stepper/number-stepper';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { Modal } from '../../../../shared/components/modal/modal';
import { Skeleton } from '../../../../shared/components/loaders/skeleton/skeleton';
import { OrderEstimateResponse } from '../../../../models/order';

@Component({
  selector: 'app-stock-details',
  imports: [
    CommonModule,
    FormsModule,
    Button,
    Badge,
    Card,
    PriceIndicator,
    CandlestickChart,
    SegmentedControl,
    NumberStepper,
    CustomInput,
    InputDirective,
    Dropdown,
    Modal,
    Skeleton
  ],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockDetails implements OnInit {
  readonly stock = input.required<Stock>();
  readonly back = output<void>();

  private readonly orderService = inject(OrderService);
  private readonly walletService = inject(WalletService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly forexService = inject(ForexService);
  private readonly toastService = inject(ToastService);

  readonly supportedCurrencies = signal<string[]>([]);
  readonly chartData = signal<CandlestickData[]>([]);

  readonly orderSide = signal<'BUY' | 'SELL'>('BUY');
  readonly orderType = signal<'MARKET' | 'LIMIT'>('MARKET');
  readonly timeInForce = signal<'DAY' | 'IOC' | 'GTC'>('DAY');
  readonly orderQuantity = signal<number>(1);
  readonly limitPrice = signal<number | null>(null);

  readonly fundingMethod = signal<'BASE' | 'CUSTOM'>('BASE');
  readonly customCurrency = signal<string | null>(null);

  readonly showReviewModal = signal<boolean>(false);
  readonly isEstimatingOrder = signal<boolean>(false);
  readonly isSubmittingOrder = signal<boolean>(false);
  readonly orderEstimate = signal<OrderEstimateResponse | null>(null);

  readonly sideOptions: SegmentOption<'BUY' | 'SELL'>[] = [
    { label: 'Buy', value: 'BUY' },
    { label: 'Sell', value: 'SELL' }
  ];

  readonly typeOptions: SegmentOption<'MARKET' | 'LIMIT'>[] = [
    { label: 'Market', value: 'MARKET' },
    { label: 'Limit', value: 'LIMIT' }
  ];

  readonly tifOptions: SegmentOption<'DAY' | 'IOC' | 'GTC'>[] = [
    { label: 'Day', value: 'DAY' },
    { label: 'IOC', value: 'IOC' },
    { label: 'GTC', value: 'GTC' }
  ];

  readonly fundingMethodOptions = computed<SegmentOption<'BASE' | 'CUSTOM'>[]>(() => {
    const base = this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR';
    const isApproved = this.walletService.wallet()?.multiCurrencyStatus === 'APPROVED';
    return [
      { label: `Base (${base})`, value: 'BASE' },
      { label: 'Other Wallet', value: 'CUSTOM', disabled: !isApproved }
    ];
  });

  readonly walletBucketOptions = computed<DropdownOption<string>[]>(() => {
    const wallet = this.walletService.wallet();
    return this.supportedCurrencies().map(curr => {
      const bucket = wallet?.buckets.find(b => b.currency === curr);
      const balance = bucket ? bucket.availableBalance : 0;
      return {
        label: `${curr} (Available: ${balance.toFixed(2)})`,
        value: curr
      };
    });
  });

  readonly resolvedFundingCurrency = computed(() => {
    const base = this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR';
    if (this.fundingMethod() === 'BASE') return base;
    return this.customCurrency() || base;
  });

  readonly financials = computed(() => {
    const qty = this.orderQuantity();
    const type = this.orderType();
    const price = type === 'LIMIT' ? (this.limitPrice() ?? 0) : this.stock().currentPrice;

    const subtotalInStockCurrency = qty * price;
    const fundingCurrency = this.resolvedFundingCurrency();
    const wallet = this.walletService.wallet();

    let available = 0;
    if (wallet) {
      const bucket = wallet.buckets.find(b => b.currency === fundingCurrency);
      if (bucket) available = bucket.availableBalance;
    }

    const hasFunds = this.orderSide() === 'SELL' || available > 0;

    return {
      subtotalInStockCurrency,
      hasFunds
    };
  });

  constructor() {
    effect(() => {
      const currentStock = this.stock();
      this.limitPrice.set(currentStock.currentPrice);
      this.generateHistoricalData(currentStock.currentPrice);
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

  openDeposit(): void {
    this.toastService.info(`Deposit flow for ${this.resolvedFundingCurrency()} triggered.`);
  }

  reviewOrder(): void {
    const type = this.orderType();
    const lPrice = this.limitPrice();

    if (type === 'LIMIT' && (!lPrice || lPrice <= 0)) {
      this.toastService.danger('Please enter a valid limit price.');
      return;
    }

    if (this.orderQuantity() <= 0) {
      this.toastService.danger('Quantity must be greater than zero.');
      return;
    }

    this.showReviewModal.set(true);
    this.isEstimatingOrder.set(true);
    this.orderEstimate.set(null);

    const s = this.stock();

    this.orderService.estimateOrder({
      stockId: s.id,
      quantity: this.orderQuantity(),
      side: this.orderSide(),
      orderType: this.orderType(),
      timeInForce: this.timeInForce(),
      limitPrice: this.orderType() === 'LIMIT' ? this.limitPrice() : null,
      fundingCurrency: this.resolvedFundingCurrency()
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

  executeOrder(): void {
    const s = this.stock();
    this.isSubmittingOrder.set(true);

    this.orderService.createOrder({
      stockId: s.id,
      quantity: this.orderQuantity(),
      side: this.orderSide(),
      orderType: this.orderType(),
      timeInForce: this.timeInForce(),
      limitPrice: this.orderType() === 'LIMIT' ? this.limitPrice() : null,
      fundingCurrency: this.resolvedFundingCurrency()
    }).subscribe({
      next: () => {
        this.toastService.success(`${this.orderSide()} order for ${s.symbol} placed successfully!`);
        this.walletService.loadWallet();
        this.isSubmittingOrder.set(false);
        this.showReviewModal.set(false);
        this.onBack();
      },
      error: () => {
        this.isSubmittingOrder.set(false);
      }
    });
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

      return {
        time: date,
        open,
        high,
        low,
        close
      };
    });

    this.chartData.set(data);
  }
}