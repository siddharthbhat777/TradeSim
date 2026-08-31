import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Stock } from '../../../../models/stock';
import { OrderService } from '../../../../services/order/order-service';
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
    InputDirective
  ],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockDetails {
  readonly stock = input.required<Stock>();
  readonly back = output<void>();

  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);

  readonly chartData = signal<CandlestickData[]>([]);
  readonly orderSide = signal<'BUY' | 'SELL'>('BUY');
  readonly orderType = signal<'MARKET' | 'LIMIT'>('MARKET');
  readonly timeInForce = signal<'DAY' | 'IOC' | 'GTC'>('DAY');
  readonly orderQuantity = signal<number>(1);
  readonly limitPrice = signal<number | null>(null);
  readonly isSubmittingOrder = signal<boolean>(false);

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

  readonly estimatedTotal = computed(() => {
    const qty = this.orderQuantity();
    const type = this.orderType();
    const price = type === 'LIMIT' ? (this.limitPrice() ?? 0) : this.stock().currentPrice;
    return qty * price;
  });

  constructor() {
    effect(() => {
      const currentStock = this.stock();
      this.limitPrice.set(currentStock.currentPrice);
      this.generateHistoricalData(currentStock.currentPrice);
    });
  }

  onBack(): void {
    this.back.emit();
  }

  placeOrder(): void {
    const s = this.stock();
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

    this.isSubmittingOrder.set(true);

    this.orderService.createOrder({
      stockId: s.id,
      quantity: this.orderQuantity(),
      side: this.orderSide(),
      orderType: type,
      timeInForce: this.timeInForce(),
      limitPrice: type === 'LIMIT' ? lPrice : null
    }).subscribe({
      next: () => {
        this.toastService.success(`${this.orderSide()} order for ${s.symbol} placed successfully!`);
        this.isSubmittingOrder.set(false);
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