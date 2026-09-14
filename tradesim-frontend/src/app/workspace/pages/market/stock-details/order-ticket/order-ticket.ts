import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Stock } from '../../../../../models/stock';
import { Wallet } from '../../../../../models/wallet';
import { Button } from '../../../../../shared/components/button/button';
import { Card } from '../../../../../shared/components/card/card';
import { SegmentedControl, SegmentOption } from '../../../../../shared/components/segmented-control/segmented-control';
import { NumberStepper } from '../../../../../shared/components/number-stepper/number-stepper';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';

export interface OrderTicketPayload {
  orderSide: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT';
  timeInForce: 'DAY' | 'IOC' | 'GTC';
  orderQuantity: number;
  limitPrice: number | null;
  fundingCurrency: string;
}

@Component({
  selector: 'app-order-ticket',
  imports: [CommonModule, FormsModule, Button, Card, SegmentedControl, NumberStepper, CustomInput, InputDirective, Dropdown],
  templateUrl: './order-ticket.html',
  styleUrl: './order-ticket.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderTicket {
  readonly stock = input.required<Stock>();
  readonly wallet = input.required<Wallet | null>();
  readonly baseCurrency = input.required<string>();
  readonly supportedCurrencies = input.required<string[]>();

  readonly reviewOrder = output<OrderTicketPayload>();
  readonly openDeposit = output<string>();

  readonly orderSide = signal<'BUY' | 'SELL'>('BUY');
  readonly orderType = signal<'MARKET' | 'LIMIT'>('MARKET');
  readonly timeInForce = signal<'DAY' | 'IOC' | 'GTC'>('DAY');
  readonly orderQuantity = signal<number>(1);
  readonly limitPrice = signal<number | null>(null);

  readonly fundingMethod = signal<'BASE' | 'CUSTOM'>('BASE');
  readonly customCurrency = signal<string | null>(null);

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
    const base = this.baseCurrency();
    const isApproved = this.wallet()?.multiCurrencyStatus === 'APPROVED';
    return [
      { label: `Base (${base})`, value: 'BASE' },
      { label: 'Other Wallet', value: 'CUSTOM', disabled: !isApproved }
    ];
  });

  readonly walletBucketOptions = computed<DropdownOption<string>[]>(() => {
    const w = this.wallet();
    return this.supportedCurrencies().map(curr => {
      const bucket = w?.buckets.find(b => b.currency === curr);
      const balance = bucket ? bucket.availableBalance : 0;
      return {
        label: `${curr} (Available: ${balance.toFixed(2)})`,
        value: curr
      };
    });
  });

  readonly resolvedFundingCurrency = computed(() => {
    if (this.fundingMethod() === 'BASE') return this.baseCurrency();
    return this.customCurrency() || this.baseCurrency();
  });

  readonly financials = computed(() => {
    const qty = this.orderQuantity();
    const type = this.orderType();
    const price = type === 'LIMIT' ? (this.limitPrice() ?? 0) : this.stock().currentPrice;

    const subtotalInStockCurrency = qty * price;
    const fundingCurrency = this.resolvedFundingCurrency();
    const w = this.wallet();

    let available = 0;
    if (w) {
      const bucket = w.buckets.find(b => b.currency === fundingCurrency);
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
      this.limitPrice.set(this.stock().currentPrice);
    });

    effect(() => {
      if (this.fundingMethod() === 'CUSTOM' && !this.customCurrency()) {
        const firstOther = this.supportedCurrencies().find(c => c !== this.baseCurrency());
        if (firstOther) this.customCurrency.set(firstOther);
      }
    });
  }

  onReviewClick(): void {
    this.reviewOrder.emit({
      orderSide: this.orderSide(),
      orderType: this.orderType(),
      timeInForce: this.timeInForce(),
      orderQuantity: this.orderQuantity(),
      limitPrice: this.limitPrice(),
      fundingCurrency: this.resolvedFundingCurrency()
    });
  }

  onDepositClick(): void {
    this.openDeposit.emit(this.resolvedFundingCurrency());
  }
}