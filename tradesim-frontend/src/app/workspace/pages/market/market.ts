import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { ExchangeService } from '../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../services/market-index/market-index-service';
import { StockService } from '../../../services/stock/stock-service';
import { Exchange } from '../../../models/exchange';
import { MarketIndex } from '../../../models/market-index';
import { Stock } from '../../../models/stock';

@Component({
  selector: 'app-market',
  templateUrl: './market.html',
  styleUrl: './market.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Market implements OnInit {
  private readonly exchangeService = inject(ExchangeService);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly stockService = inject(StockService);

  readonly isFilterDrawerOpen = signal<boolean>(false);
  readonly isLoadingStocks = signal<boolean>(false);

  readonly exchanges = signal<Exchange[]>([]);
  readonly selectedExchangeId = signal<string | null>(null);

  readonly indices = signal<MarketIndex[]>([]);
  readonly selectedIndexId = signal<string | null>(null);

  readonly rawStocks = signal<Stock[]>([]);

  readonly searchQuery = signal<string>('');
  readonly priceRange = signal<[number, number]>([0, 10000]);
  readonly selectedSectors = signal<string[]>([]);
  readonly sortBy = signal<string>('SYMBOL_ASC');

  readonly filteredAndSortedStocks = computed(() => {
    let result = [...this.rawStocks()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      result = result.filter(s =>
        s.symbol.toLowerCase().includes(query) ||
        s.companyName.toLowerCase().includes(query)
      );
    }

    const [minPrice, maxPrice] = this.priceRange();
    if (minPrice > 0 || maxPrice < 10000) {
      result = result.filter(s => s.currentPrice >= minPrice && s.currentPrice <= maxPrice);
    }

    const sectors = this.selectedSectors();
    if (sectors.length > 0) {
      result = result.filter(s => sectors.includes(s.sector));
    }

    const sort = this.sortBy();
    result.sort((a, b) => {
      switch (sort) {
        case 'PRICE_DESC': return b.currentPrice - a.currentPrice;
        case 'PRICE_ASC': return a.currentPrice - b.currentPrice;
        case 'NAME_ASC': return a.companyName.localeCompare(b.companyName);
        case 'SYMBOL_ASC': return a.symbol.localeCompare(b.symbol);
        default: return 0;
      }
    });

    return result;
  });

  constructor() {
    effect(() => {
      const exchangeId = this.selectedExchangeId();
      if (exchangeId) {
        this.marketIndexService.getIndicesByExchange(exchangeId).subscribe(data => {
          this.indices.set(data);
          if (data.length > 0) {
            this.selectedIndexId.set(data[0].id);
          } else {
            this.selectedIndexId.set(null);
          }
        });
      }
    });
  }

  ngOnInit(): void {
    this.exchangeService.getExchanges().subscribe(data => {
      this.exchanges.set(data);
      if (data.length > 0) {
        this.selectedExchangeId.set(data[0].id);
      }
    });

    this.isLoadingStocks.set(true);
    this.stockService.getStocks().subscribe(data => {
      this.rawStocks.set(data);
      this.isLoadingStocks.set(false);
    });
  }

  openFilterDrawer(): void {
    this.isFilterDrawerOpen.set(true);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.priceRange.set([0, 10000]);
    this.selectedSectors.set([]);
  }
}