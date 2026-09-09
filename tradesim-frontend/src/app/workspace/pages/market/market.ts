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
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency-pipe';

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
    StockDetails,
    FormatCurrencyPipe
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

  readonly appliedSectors = signal<string[]>([]);
  readonly appliedStatuses = signal<string[]>([]);
  readonly appliedMarketCapCategories = signal<string[]>([]);

  readonly draftSectors = signal<string[]>([]);
  readonly draftStatuses = signal<string[]>([]);
  readonly draftMarketCapCategories = signal<string[]>([]);

  readonly currentExchangeCurrency = computed(() => {
    const id = this.selectedExchangeId();
    const exchange = this.exchanges().find(e => e.id === id);
    return exchange?.currency || 'USD';
  });

  readonly exchangeStocks = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return [];
    return this.rawStocks().filter(s => s.exchangeId === id);
  });

  readonly priceRangeBounds = computed(() => {
    const data = this.exchangeStocks();
    if (data.length === 0) return { min: 0, max: 100 };

    const maxPrice = Math.max(...data.map(s => s.currentPrice));
    return {
      min: 0,
      max: (Math.ceil(maxPrice / 100) * 100) + 100
    };
  });

  readonly priceSliderStep = computed(() => {
    return Math.max(1, Math.floor(this.priceRangeBounds().max / 100));
  });

  readonly _appliedPriceRange = signal<[number, number] | null>(null);
  readonly _draftPriceRange = signal<[number, number] | null>(null);

  readonly appliedPriceRange = computed(() => {
    const val = this._appliedPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

  readonly draftPriceRange = computed(() => {
    const val = this._draftPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

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

  readonly marketCapOptions = [
    { label: 'Large Cap', value: 'LARGE' },
    { label: 'Mid Cap', value: 'MID' },
    { label: 'Small Cap', value: 'SMALL' }
  ];

  readonly statusOptions = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Halted', value: 'HALTED' },
    { label: 'Delisted', value: 'DELISTED' }
  ];

  readonly sectorOptions = computed(() => {
    const sectors = new Set(this.exchangeStocks().map(s => s.sector));
    return Array.from(sectors).sort().map(sector => ({
      label: sector.charAt(0) + sector.slice(1).toLowerCase().replace(/_/g, ' '),
      value: sector
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
    const activeRange = this.appliedPriceRange();
    const draftRange = this.draftPriceRange();
    if (activeRange[0] !== draftRange[0] || activeRange[1] !== draftRange[1]) return true;

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

  readonly hasActiveFilters = computed(() => {
    const pRange = this.appliedPriceRange();
    const minPrice = Math.min(pRange[0], pRange[1]);
    const maxPrice = Math.max(pRange[0], pRange[1]);
    const bounds = this.priceRangeBounds();

    return this.appliedSectors().length > 0 ||
      this.appliedStatuses().length > 0 ||
      this.appliedMarketCapCategories().length > 0 ||
      minPrice > bounds.min ||
      maxPrice < bounds.max;
  });

  readonly filteredAndSortedStocks = computed(() => {
    let result = [...this.exchangeStocks()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      result = result.filter(s =>
        s.symbol.toLowerCase().includes(query) ||
        s.companyName.toLowerCase().includes(query)
      );
    }

    const pRange = this.appliedPriceRange();
    const minPrice = Math.min(pRange[0], pRange[1]);
    const maxPrice = Math.max(pRange[0], pRange[1]);
    const bounds = this.priceRangeBounds();

    if (minPrice > bounds.min || maxPrice < bounds.max) {
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

      this._appliedPriceRange.set(null);
      this._draftPriceRange.set(null);
      this.appliedSectors.set([]);
      this.draftSectors.set([]);
      this.appliedStatuses.set([]);
      this.draftStatuses.set([]);
      this.appliedMarketCapCategories.set([]);
      this.draftMarketCapCategories.set([]);

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
    this.isFilterDrawerOpen.set(true);
  }

  onPriceSliderChange(val: number | [number, number]): void {
    if (Array.isArray(val)) {
      this._draftPriceRange.set([val[0], val[1]]);
    }
  }

  resetDraftFilters(): void {
    this.draftSectors.set([]);
    this.draftStatuses.set([]);
    this.draftMarketCapCategories.set([]);
    this._draftPriceRange.set(null);
  }

  applyFilters(): void {
    this.appliedSectors.set([...this.draftSectors()]);
    this.appliedStatuses.set([...this.draftStatuses()]);
    this.appliedMarketCapCategories.set([...this.draftMarketCapCategories()]);

    const [min, max] = this.draftPriceRange();
    this._appliedPriceRange.set([Math.min(min, max), Math.max(min, max)]);

    this.isFilterDrawerOpen.set(false);
  }
}