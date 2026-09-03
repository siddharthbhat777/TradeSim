import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../services/order/order-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { OrderHistoryResponse } from '../../../models/order';
import { Card } from '../../../shared/components/card/card';
import { Table, TableColumn } from '../../../shared/components/table/table';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { Button } from '../../../shared/components/button/button';
import { Badge } from '../../../shared/components/badge/badge';
import { Drawer } from '../../../shared/components/drawer/drawer';
import { SegmentedControl } from '../../../shared/components/segmented-control/segmented-control';
import { Dropdown, DropdownOption } from '../../../shared/components/dropdown/dropdown';
import { Slider } from '../../../shared/components/slider/slider';
import { ToastService } from '../../../shared/components/toast/toast.service';

export interface OrderRow extends OrderHistoryResponse {
  displayQuantity: string;
}

@Component({
  selector: 'app-order',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Table,
    CustomInput,
    InputDirective,
    Button,
    Badge,
    Drawer,
    SegmentedControl,
    Dropdown,
    Slider,
    DatePipe,
    CurrencyPipe
  ],
  templateUrl: './order.html',
  styleUrl: './order.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Order implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly tradingAccountService = inject(TradingAccountService);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');
  readonly allOrders = this.orderService.orders;

  readonly searchQuery = signal<string>('');
  readonly activeSort = signal<string>('DATE_DESC');

  readonly appliedSide = signal<string>('ALL');
  readonly appliedType = signal<string>('ALL');
  readonly appliedStatus = signal<string>('ALL');
  readonly _appliedPriceRange = signal<[number, number] | null>(null);

  readonly draftSide = signal<string>('ALL');
  readonly draftType = signal<string>('ALL');
  readonly draftStatus = signal<string>('ALL');
  readonly _draftPriceRange = signal<[number, number] | null>(null);

  readonly isFilterDrawerOpen = signal(false);
  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);
  readonly isCancelling = signal<string | null>(null);

  readonly sortOptions: DropdownOption[] = [
    { label: 'Newest First', value: 'DATE_DESC' },
    { label: 'Oldest First', value: 'DATE_ASC' },
    { label: 'Symbol (A-Z)', value: 'SYMBOL_ASC' },
    { label: 'Symbol (Z-A)', value: 'SYMBOL_DESC' }
  ];

  readonly sideOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'Buy', value: 'BUY' },
    { label: 'Sell', value: 'SELL' }
  ];

  readonly typeOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'Market', value: 'MARKET' },
    { label: 'Limit', value: 'LIMIT' }
  ];

  readonly statusOptions: DropdownOption[] = [
    { label: 'All Statuses', value: 'ALL' },
    { label: 'Open', value: 'OPEN' },
    { label: 'Filled', value: 'FILLED' },
    { label: 'Partially Filled', value: 'PARTIALLY_FILLED' },
    { label: 'Cancelled', value: 'CANCELLED' }
  ];

  readonly priceRangeBounds = computed(() => {
    const limitOrders = this.allOrders().filter(o => o.limitPrice !== null && o.limitPrice > 0);
    if (limitOrders.length === 0) return { min: 0, max: 100000 };
    const prices = limitOrders.map(o => o.limitPrice!);
    return {
      min: Math.floor(Math.min(...prices)),
      max: Math.ceil(Math.max(...prices))
    };
  });

  readonly appliedPriceRange = computed(() => {
    const val = this._appliedPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

  readonly draftPriceRange = computed(() => {
    const val = this._draftPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

  readonly draftMinPrice = computed(() => Math.min(this.draftPriceRange()[0], this.draftPriceRange()[1]));
  readonly draftMaxPrice = computed(() => Math.max(this.draftPriceRange()[0], this.draftPriceRange()[1]));

  readonly hasActiveFilters = computed(() => {
    const min = Math.min(this.appliedPriceRange()[0], this.appliedPriceRange()[1]);
    const max = Math.max(this.appliedPriceRange()[0], this.appliedPriceRange()[1]);
    const bounds = this.priceRangeBounds();

    return this.appliedSide() !== 'ALL' ||
      this.appliedType() !== 'ALL' ||
      this.appliedStatus() !== 'ALL' ||
      min > bounds.min ||
      max < bounds.max;
  });

  readonly isApplyDisabled = computed(() => {
    const draftMin = Math.min(this.draftPriceRange()[0], this.draftPriceRange()[1]);
    const draftMax = Math.max(this.draftPriceRange()[0], this.draftPriceRange()[1]);
    const appliedMin = Math.min(this.appliedPriceRange()[0], this.appliedPriceRange()[1]);
    const appliedMax = Math.max(this.appliedPriceRange()[0], this.appliedPriceRange()[1]);

    return this.draftSide() === this.appliedSide() &&
      this.draftType() === this.appliedType() &&
      this.draftStatus() === this.appliedStatus() &&
      draftMin === appliedMin &&
      draftMax === appliedMax;
  });

  readonly processedOrders = computed<OrderRow[]>(() => {
    let filtered = this.allOrders();

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      filtered = filtered.filter(o => o.symbol.toLowerCase().includes(query));
    }

    if (this.appliedSide() !== 'ALL') {
      filtered = filtered.filter(o => o.side === this.appliedSide());
    }

    if (this.appliedType() !== 'ALL') {
      filtered = filtered.filter(o => o.orderType === this.appliedType());
    }

    if (this.appliedStatus() !== 'ALL') {
      filtered = filtered.filter(o => o.status === this.appliedStatus());
    }

    const currentRange = this.appliedPriceRange();
    const bounds = this.priceRangeBounds();
    const minPrice = Math.min(currentRange[0], currentRange[1]);
    const maxPrice = Math.max(currentRange[0], currentRange[1]);

    if (minPrice > bounds.min || maxPrice < bounds.max) {
      filtered = filtered.filter(o => {
        if (!o.limitPrice) return true;
        return o.limitPrice >= minPrice && o.limitPrice <= maxPrice;
      });
    }

    const sort = this.activeSort();
    const sorted = [...filtered].sort((a, b) => {
      switch (sort) {
        case 'DATE_ASC':
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        case 'SYMBOL_ASC':
          return a.symbol.localeCompare(b.symbol);
        case 'SYMBOL_DESC':
          return b.symbol.localeCompare(a.symbol);
        case 'DATE_DESC':
        default:
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });

    return sorted.map(o => ({
      ...o,
      displayQuantity: `${o.filledQuantity} / ${o.quantity}`
    }));
  });

  readonly columns: TableColumn<OrderRow>[] = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'side', header: 'Type' },
    { key: 'limitPrice', header: 'Price', align: 'right' },
    { key: 'displayQuantity', header: 'Filled / Total', align: 'right' },
    { key: 'status', header: 'Status', align: 'center' },
    { key: 'createdAt', header: 'Date', align: 'right' },
    { key: 'actions', header: '', align: 'right' }
  ];

  ngOnInit(): void {
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }

    this.orderService.loadOrders();

    const bounds = this.priceRangeBounds();
    this._appliedPriceRange.set([bounds.min, bounds.max]);
    this._draftPriceRange.set([bounds.min, bounds.max]);
  }

  openFilterDrawer(): void {
    this.isFilterDrawerOpen.set(true);
  }

  onPriceSliderChange(val: number | [number, number]): void {
    if (Array.isArray(val)) {
      this._draftPriceRange.set([val[0], val[1]]);
    }
  }

  applyFilters(): void {
    const [min, max] = this.draftPriceRange();
    this._appliedPriceRange.set([Math.min(min, max), Math.max(min, max)]);
    this.appliedSide.set(this.draftSide());
    this.appliedType.set(this.draftType());
    this.appliedStatus.set(this.draftStatus());
    this.currentPage.set(1);
    this.isFilterDrawerOpen.set(false);
  }

  resetDraftFilters(): void {
    const bounds = this.priceRangeBounds();
    this._draftPriceRange.set([bounds.min, bounds.max]);
    this.draftSide.set('ALL');
    this.draftType.set('ALL');
    this.draftStatus.set('ALL');
  }

  cancelOrder(orderId: string): void {
    this.isCancelling.set(orderId);
    this.orderService.cancelOrder(orderId).subscribe({
      next: () => {
        this.toastService.success('Order cancelled successfully.');
        this.orderService.loadOrders();
        this.isCancelling.set(null);
      },
      error: () => {
        this.isCancelling.set(null);
      }
    });
  }
}