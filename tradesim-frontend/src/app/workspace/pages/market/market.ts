import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ExchangeService } from '../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../services/market-index/market-index-service';
import { StockService } from '../../../services/stock/stock-service';
import { Exchange } from '../../../models/exchange';
import { MarketIndex } from '../../../models/market-index';
import { Stock } from '../../../models/stock';
import { Dropdown } from '../../../shared/components/dropdown/dropdown';
import { Card } from '../../../shared/components/card/card';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { Button } from '../../../shared/components/button/button';
import { Table, TableCellDirective } from '../../../shared/components/table/table';
import { Badge } from '../../../shared/components/badge/badge';
import { Drawer } from '../../../shared/components/drawer/drawer';
import { Slider } from '../../../shared/components/slider/slider';
import { CheckboxGroup } from '../../../shared/components/checkbox/checkbox-group/checkbox-group';
import { CandlestickChart, CandlestickData } from '../../../shared/components/charts/candlestick-chart/candlestick-chart';
import { Clock } from './clock/clock';
import { StockDetails } from './stock-details/stock-details';

@Component({
  selector: 'app-market',
  imports: [
    CommonModule,
    FormsModule,
    Dropdown,
    Card,
    EmptyState,
    CustomInput,
    InputDirective,
    Button,
    Table,
    TableCellDirective,
    Badge,
    Drawer,
    Slider,
    CheckboxGroup,
    CandlestickChart,
    Clock,
    StockDetails
  ],
  templateUrl: './market.html',
  styleUrl: './market.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Market implements OnInit {
  private readonly exchangeService = inject(ExchangeService);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly stockService = inject(StockService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isFilterDrawerOpen = signal<boolean>(false);
  readonly isLoadingStocks = signal<boolean>(false);

  readonly exchanges = signal<Exchange[]>([]);
  readonly selectedExchangeId = signal<string | null>(null);

  readonly indices = signal<MarketIndex[]>([]);
  readonly selectedIndexId = signal<string | null>(null);

  readonly rawStocks = signal<Stock[]>([]);
  readonly selectedStock = signal<Stock | null>(null);
  readonly indexChartData = signal<CandlestickData[]>([]);

  readonly searchQuery = signal<string>('');
  readonly sortBy = signal<string>('SYMBOL_ASC');

  readonly maxStockPrice = computed(() => {
    const stocks = this.rawStocks();
    if (stocks.length === 0) return 10000;
    const max = Math.max(...stocks.map(s => s.currentPrice));
    return max > 0 ? Math.ceil(max / 100) * 100 : 10000;
  });

  readonly appliedPriceRange = signal<[number, number]>([0, 10000]);
  readonly appliedSectors = signal<string[]>([]);
  readonly appliedStatuses = signal<string[]>([]);
  readonly appliedMarketCapCategories = signal<string[]>([]);

  readonly draftPriceRange = signal<[number, number]>([0, 10000]);
  readonly draftSectors = signal<string[]>([]);
  readonly draftStatuses = signal<string[]>([]);
  readonly draftMarketCapCategories = signal<string[]>([]);

  readonly exchangeOptions = computed(() =>
    this.exchanges().map(e => ({ label: e.name, value: e.id, code: e.code, currency: e.currency, status: e.status }))
  );

  readonly indexOptions = computed(() =>
    this.indices().map(i => ({ label: i.name, value: i.id }))
  );

  readonly sortOptions = [
    { label: 'Symbol (A-Z)', value: 'SYMBOL_ASC' },
    { label: 'Company (A-Z)', value: 'NAME_ASC' },
    { label: 'Price (High to Low)', value: 'PRICE_DESC' },
    { label: 'Price (Low to High)', value: 'PRICE_ASC' }
  ];

  readonly sectorOptions = computed(() => {
    const sectors = new Set(this.rawStocks().map(s => s.sector));
    return Array.from(sectors).sort().map(sector => ({
      label: sector.charAt(0) + sector.slice(1).toLowerCase().replace(/_/g, ' '),
      value: sector
    }));
  });

  readonly statusOptions = computed(() => {
    const statuses = new Set(this.rawStocks().map(s => s.status));
    return Array.from(statuses).sort().map(status => ({
      label: status.charAt(0) + status.slice(1).toLowerCase(),
      value: status
    }));
  });

  readonly marketCapOptions = computed(() => {
    const categories = new Set(
      this.rawStocks()
        .map(s => s.marketCapCategory)
        .filter(cat => !!cat && cat !== 'UNKNOWN')
    );
    const order: Record<string, number> = { LARGE: 1, MID: 2, SMALL: 3 };
    return Array.from(categories)
      .sort((a, b) => (order[a] ?? 99) - (order[b] ?? 99))
      .map(category => ({
        label: category.charAt(0) + category.slice(1).toLowerCase() + ' Cap',
        value: category
      }));
  });

  readonly tableColumns = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'companyName', header: 'Company' },
    { key: 'sector', header: 'Sector' },
    { key: 'currentPrice', header: 'Price', align: 'right' as const },
    { key: 'dayVolume', header: 'Volume', align: 'right' as const },
    { key: 'status', header: 'Status', align: 'center' as const }
  ];

  readonly hasUnsavedFilters = computed(() => {
    const appliedPrice = this.appliedPriceRange();
    const draftPrice = this.draftPriceRange();

    if (appliedPrice[0] !== draftPrice[0] || appliedPrice[1] !== draftPrice[1]) return true;

    const checkArraysDifference = (arr1: string[], arr2: string[]) => {
      if (arr1.length !== arr2.length) return true;
      const sorted1 = [...arr1].sort();
      const sorted2 = [...arr2].sort();
      return sorted1.some((val, i) => val !== sorted2[i]);
    };

    if (checkArraysDifference(this.appliedSectors(), this.draftSectors())) return true;
    if (checkArraysDifference(this.appliedStatuses(), this.draftStatuses())) return true;
    if (checkArraysDifference(this.appliedMarketCapCategories(), this.draftMarketCapCategories())) return true;

    return false;
  });

  readonly filteredAndSortedStocks = computed(() => {
    let result = [...this.rawStocks()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      result = result.filter(s =>
        s.symbol.toLowerCase().includes(query) ||
        s.companyName.toLowerCase().includes(query)
      );
    }

    const [p1, p2] = this.appliedPriceRange();
    const minPrice = Math.min(p1, p2);
    const maxPrice = Math.max(p1, p2);
    const ceiling = this.maxStockPrice();

    if (minPrice > 0 || maxPrice < ceiling) {
      result = result.filter(s => s.currentPrice >= minPrice && s.currentPrice <= maxPrice);
    }

    const sectors = this.appliedSectors();
    if (sectors.length > 0) {
      result = result.filter(s => sectors.includes(s.sector));
    }

    const statuses = this.appliedStatuses();
    if (statuses.length > 0) {
      result = result.filter(s => statuses.includes(s.status));
    }

    const marketCaps = this.appliedMarketCapCategories();
    if (marketCaps.length > 0) {
      result = result.filter(s => marketCaps.includes(s.marketCapCategory));
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
      } else {
        this.indices.set([]);
        this.selectedIndexId.set(null);
      }
    });

    effect(() => {
      const indexId = this.selectedIndexId();
      if (indexId) {
        this.indexChartData.set([]);
      } else {
        this.indexChartData.set([]);
      }
    });

    this.route.queryParamMap.subscribe(params => {
      const stockId = params.get('stockId');
      if (stockId) {
        const found = this.rawStocks().find(s => s.id === stockId);
        if (found) {
          this.selectedStock.set(found);
        }
      } else {
        this.selectedStock.set(null);
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

      const maxPrice = this.maxStockPrice();
      this.appliedPriceRange.set([0, maxPrice]);
      this.draftPriceRange.set([0, maxPrice]);

      const targetStockId = this.route.snapshot.queryParamMap.get('stockId');
      if (targetStockId) {
        const found = data.find(s => s.id === targetStockId);
        if (found) {
          this.selectedStock.set(found);
        }
      }
    });
  }

  onSelectStock(stock: Stock): void {
    this.selectedStock.set(stock);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { stockId: stock.id },
      queryParamsHandling: 'merge'
    });
  }

  onBackToMarket(): void {
    this.selectedStock.set(null);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { stockId: null },
      queryParamsHandling: 'merge'
    });
  }

  openFilterDrawer(): void {
    const [min, max] = this.appliedPriceRange();
    this.draftPriceRange.set([Math.min(min, max), Math.max(min, max)]);
    this.draftSectors.set([...this.appliedSectors()]);
    this.draftStatuses.set([...this.appliedStatuses()]);
    this.draftMarketCapCategories.set([...this.appliedMarketCapCategories()]);
    this.isFilterDrawerOpen.set(true);
  }

  resetDraftFilters(): void {
    this.draftPriceRange.set([0, this.maxStockPrice()]);
    this.draftSectors.set([]);
    this.draftStatuses.set([]);
    this.draftMarketCapCategories.set([]);
  }

  applyFilters(): void {
    const [min, max] = this.draftPriceRange();
    this.appliedPriceRange.set([Math.min(min, max), Math.max(min, max)]);
    this.appliedSectors.set([...this.draftSectors()]);
    this.appliedStatuses.set([...this.draftStatuses()]);
    this.appliedMarketCapCategories.set([...this.draftMarketCapCategories()]);
    this.isFilterDrawerOpen.set(false);
  }
}