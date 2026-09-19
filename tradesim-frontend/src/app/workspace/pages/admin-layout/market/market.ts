import { ChangeDetectionStrategy, Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Card } from '../../../../shared/components/card/card';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { StockService } from '../../../../services/stock/stock-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { Exchange, ExchangeMarketClock } from '../../../../models/exchange';
import { Stock } from '../../../../models/stock';
import { MarketIndex, MarketIndexConstituent } from '../../../../models/market-index';
import { ExchangeSlate } from './exchange-slate/exchange-slate';
import { StockControls } from './stock-controls/stock-controls';
import { MarketIndices } from './market-indices/market-indices';
import { ForexAndFees } from './forex-and-fees/forex-and-fees';

export type MarketTab = 'Stocks' | 'Indices' | 'Forex';

@Component({
  selector: 'app-market',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    Card,
    SegmentedControl,
    ExchangeSlate,
    StockControls,
    MarketIndices,
    ForexAndFees
  ],
  templateUrl: './market.html',
  styleUrl: './market.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Market implements OnInit, OnDestroy {
  private readonly exchangeService = inject(ExchangeService);
  private readonly stockService = inject(StockService);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly forexService = inject(ForexService);

  readonly isLoading = signal(true);
  readonly exchanges = signal<Exchange[]>([]);
  readonly selectedExchangeId = signal<string | null>(null);
  readonly clocks = signal<Map<string, ExchangeMarketClock>>(new Map());
  readonly stocks = signal<Stock[]>([]);
  readonly indices = signal<MarketIndex[]>([]);
  readonly constituents = signal<Map<string, MarketIndexConstituent[]>>(new Map());
  readonly currencies = signal<string[]>([]);
  readonly activeTab = signal<MarketTab>('Stocks');

  readonly tabOptions = signal<SegmentOption<MarketTab>[]>([
    { label: 'Stock Controls', value: 'Stocks' },
    { label: 'Market Indices', value: 'Indices' },
    { label: 'Forex & Fees', value: 'Forex' }
  ]);

  readonly currentExchange = computed(() => {
    const id = this.selectedExchangeId();
    return this.exchanges().find(e => e.id === id) ?? null;
  });

  readonly exchangeStocks = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return [];
    return this.stocks().filter(s => s.exchangeId === id);
  });

  readonly exchangeIndices = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return [];
    return this.indices().filter(i => i.exchangeId === id);
  });

  private clockTimer: any;

  ngOnInit(): void {
    this.loadData();
    this.clockTimer = setInterval(() => this.tickClocks(), 1000);
  }

  ngOnDestroy(): void {
    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
  }

  onExchangeSelect(val: string | null): void {
    if (val) {
      this.selectedExchangeId.set(val);
    } else {
      const current = this.selectedExchangeId();
      if (current) {
        this.selectedExchangeId.set(null);
        setTimeout(() => this.selectedExchangeId.set(current));
      }
    }
  }

  private loadData(): void {
    this.isLoading.set(true);
    forkJoin({
      exchanges: this.exchangeService.getExchanges(),
      stocks: this.stockService.getStocks(),
      indices: this.marketIndexService.getAllIndices(),
      currencies: this.forexService.getSupportedCurrencies()
    }).subscribe({
      next: (res) => {
        this.exchanges.set(res.exchanges);
        if (res.exchanges.length > 0 && !this.selectedExchangeId()) {
          this.selectedExchangeId.set(res.exchanges[0].id);
        }
        this.stocks.set(res.stocks);
        this.indices.set(res.indices);
        this.currencies.set(res.currencies);
        res.exchanges.forEach(ex => this.loadMarketClock(ex.id));
        res.indices.forEach(idx => this.loadConstituents(idx.id));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  private loadMarketClock(exchangeId: string): void {
    this.exchangeService.getMarketClock(exchangeId).subscribe({
      next: (clock) => {
        const currentMap = new Map(this.clocks());
        currentMap.set(exchangeId, clock);
        this.clocks.set(currentMap);
      }
    });
  }

  loadConstituents(indexId: string): void {
    this.marketIndexService.getConstituents(indexId).subscribe({
      next: (data) => {
        const currentMap = new Map(this.constituents());
        currentMap.set(indexId, data);
        this.constituents.set(currentMap);
      }
    });
  }

  private tickClocks(): void {
    const currentMap = this.clocks();
    if (currentMap.size === 0) return;
    const newMap = new Map<string, ExchangeMarketClock>();
    currentMap.forEach((clock, id) => {
      if (!clock || !clock.localTime) {
        newMap.set(id, clock);
        return;
      }
      const parts = clock.localTime.split(':');
      if (parts.length === 3) {
        let h = parseInt(parts[0], 10);
        let m = parseInt(parts[1], 10);
        let s = parseInt(parts[2], 10);
        s++;
        if (s >= 60) {
          s = 0;
          m++;
        }
        if (m >= 60) {
          m = 0;
          h++;
        }
        if (h >= 24) {
          h = 0;
        }
        newMap.set(id, {
          ...clock,
          localTime: `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
        });
      } else {
        newMap.set(id, clock);
      }
    });
    this.clocks.set(newMap);
  }

  getFallbackCurrency(): string {
    return this.currentExchange()?.currency || (this.exchanges().length > 0 ? this.exchanges()[0].currency : 'USD');
  }

  onStockUpdated(updatedStock: Stock): void {
    this.stocks.update(arr => arr.map(s => s.id === updatedStock.id ? updatedStock : s));
  }

  onIndexCreated(newIndex: MarketIndex): void {
    this.indices.update(arr => [...arr, newIndex]);
  }

  onIndexInitialized(updatedIndex: MarketIndex): void {
    this.indices.update(arr => arr.map(i => i.id === updatedIndex.id ? updatedIndex : i));
  }
}